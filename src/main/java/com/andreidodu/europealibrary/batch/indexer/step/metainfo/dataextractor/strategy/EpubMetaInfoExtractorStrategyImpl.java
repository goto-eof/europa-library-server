package com.andreidodu.europealibrary.batch.indexer.step.metainfo.dataextractor.strategy;

import com.andreidodu.europealibrary.batch.indexer.step.metainfo.dataextractor.MetaInfoExtractorStrategy;
import com.andreidodu.europealibrary.constants.DataPropertiesConst;
import com.andreidodu.europealibrary.dto.BookCodesDTO;
import com.andreidodu.europealibrary.model.BookInfo;
import com.andreidodu.europealibrary.model.FileMetaInfo;
import com.andreidodu.europealibrary.model.FileSystemItem;
import com.andreidodu.europealibrary.repository.BookInfoRepository;
import com.andreidodu.europealibrary.repository.FileMetaInfoRepository;
import com.andreidodu.europealibrary.util.EpubUtil;
import com.andreidodu.europealibrary.util.FileUtil;
import com.andreidodu.europealibrary.util.StringUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Date;
import nl.siegmann.epublib.domain.Identifier;
import nl.siegmann.epublib.domain.Metadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Order(1)
@Component
@Transactional
@RequiredArgsConstructor
public class EpubMetaInfoExtractorStrategyImpl implements MetaInfoExtractorStrategy {
    final private static String STRATEGY_NAME = "epub-meta-info-extractor-strategy";
    private static final int ISBN_10_LENGTH = 10;
    private static final int ISBN_13_LENGTH = 13;

    private final EpubUtil epubUtil;
    private final DataExtractorStrategyUtil dataExtractorStrategyUtil;
    private final FileMetaInfoRepository fileMetaInfoRepository;
    private final BookInfoRepository bookInfoRepository;
    private final TagUtil tagUtil;
    private final FileUtil fileUtil;
    private final OtherMetaInfoExtractorStrategyImpl otherMetaInfoExtractorStrategy;
    @Value("${com.andreidodu.europea-library.job.indexer.step-indexer.disable-epub-metadata-extractor}")
    private boolean disableEpubMetadataExtractor;
    @Value("${com.andreidodu.europea-library.job.indexer.step-meta-info-writer.disable-isbn-extractor}")
    private boolean disableIsbExtractor;
    @Value("${com.andreidodu.europea-library.job.indexer.avoid-duplicate-meta-info}")
    private boolean avoidDuplicateMetaInfo;
    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public boolean accept(String fullPathAndName, FileSystemItem fileSystemItem) {
        return !this.fileUtil.isDirectory(fullPathAndName) && dataExtractorStrategyUtil.isFileExtensionInWhiteList(epubUtil.getEpubFileExtension()) &&
                !disableEpubMetadataExtractor &&
                epubUtil.isEpub(fullPathAndName) &&
                dataExtractorStrategyUtil.wasNotAlreadyProcessed(fileSystemItem);
    }


    @Override
    public Optional<FileMetaInfo> extract(String filename, FileSystemItem fileSystemItem) {
        log.debug("applying strategy: {}", getStrategyName());
        FileMetaInfo fileMetaInfo = fileSystemItem.getFileMetaInfo();
        try {
            return epubUtil.retrieveBook(filename)
                    .map(book -> {
                        log.debug("metadata found for: {}", filename);
                        return manageCaseBookInfoNotEmpty(book, filename, fileMetaInfo);
                    });
        } catch (Exception e) {
            log.debug("invalid file: {} ({})", filename, e.getMessage());
            return this.otherMetaInfoExtractorStrategy.extract(filename, fileSystemItem);
        }
    }

    private FileMetaInfo manageCaseBookInfoNotEmpty(Book book, String fullPath, FileMetaInfo existingFileMetaInfo) {
        log.debug("gathering information from ebook {}", fullPath);
        final FileMetaInfo fileMetaInfo = existingFileMetaInfo == null ? new FileMetaInfo() : existingFileMetaInfo;

        Metadata metadata = book.getMetadata();

        calculateAndSetBookTitle(fullPath, metadata, fileMetaInfo);

        fileMetaInfo.setDescription(StringUtil.cleanAndTrimToNullSubstring(StringUtil.removeHTML(getFirstAvailable(metadata.getDescriptions())), DataPropertiesConst.FILE_META_INFO_DESCRIPTION_MAX_LENGTH));
        FileMetaInfo savedFileMetaInfo = this.fileMetaInfoRepository.save(fileMetaInfo);
        savedFileMetaInfo = buildBookInfo(book, savedFileMetaInfo, metadata);
        return savedFileMetaInfo;
    }

    private void calculateAndSetBookTitle(String fullPath, Metadata metadata, FileMetaInfo fileMetaInfo) {
        final String title = StringUtil.cleanAndTrimToNullSubstring(metadata.getFirstTitle(), DataPropertiesConst.FILE_META_INFO_TITLE_MAX_LENGTH);
        Optional.ofNullable(title).ifPresentOrElse(cleanedTitle -> {
            fileMetaInfo.setTitle(title);
        }, () -> Optional.ofNullable(StringUtil.cleanAndTrimToNullSubstring(fileUtil.calculateFileBaseName(fullPath), DataPropertiesConst.FILE_META_INFO_TITLE_MAX_LENGTH))
                .ifPresentOrElse(fileMetaInfo::setTitle, () -> fileMetaInfo.setTitle("")));
    }

    private FileMetaInfo buildBookInfo(Book book, FileMetaInfo fileMetaInfo, Metadata metadata) {
        List<String> isbnList = extractISBN(metadata.getIdentifiers());
        if (avoidDuplicateMetaInfo) {
            Optional<FileMetaInfo> fileMetaInfoByIsbn = this.findFileMetaInfoByIsbn(isbnList)
                    .stream().findFirst();
            if (fileMetaInfoByIsbn.isPresent()) {
                return fileMetaInfoByIsbn.get();
            }
        }

        BookInfo bookInfo = fileMetaInfo.getBookInfo() != null ? fileMetaInfo.getBookInfo() : new BookInfo();
        bookInfo.setFileMetaInfo(fileMetaInfo);
        Optional.ofNullable(metadata.getLanguage())
                .ifPresent(language ->
                        bookInfo.setLanguage(StringUtil.cleanAndTrimToNullLowerCaseSubstring(language, DataPropertiesConst.BOOK_INFO_LANGUAGE_MAX_LENGTH)));
        bookInfo.setNumberOfPages(book.getContents().size());
        final List<String> authors = StringUtil.cleanAndTrimToNull(metadata.getAuthors().stream().map(auth -> auth.getFirstname() + " " + auth.getLastname()).collect(Collectors.toList()));
        if (!authors.isEmpty()) {
            bookInfo.setAuthors(StringUtil.cleanAndTrimToNullSubstring(String.join(",", authors), DataPropertiesConst.BOOK_INFO_AUTHORS_MAX_LENGTH));
        }


        if (isbnList.stream().anyMatch(isbn -> isbn.length() == ISBN_10_LENGTH || isbn.length() == ISBN_13_LENGTH)) {
            isbnList.forEach(isbn -> {
                if (isbn.length() == ISBN_13_LENGTH) {
                    bookInfo.setIsbn13(isbn);

                } else if (isbn.length() == ISBN_10_LENGTH) {
                    bookInfo.setIsbn10(isbn);
                }
            });
        } else {
            if (!disableIsbExtractor) {
                BookCodesDTO<Optional<String>, Optional<String>> bookCodes = this.epubUtil.extractISBN(book);
                dataExtractorStrategyUtil.setISBN13(bookCodes, bookInfo);
                dataExtractorStrategyUtil.setISBN10(bookCodes, bookInfo);
            }
        }


        final List<String> publishers = StringUtil.cleanAndTrimToNull(metadata.getPublishers());
        if (!publishers.isEmpty()) {
            bookInfo.setPublisher(StringUtil.cleanAndTrimToNullSubstring(String.join(",", publishers), DataPropertiesConst.BOOK_INFO_PUBLISHER_MAX_LENGTH));
        }
        List<Date> dates = metadata.getDates();
        extractPublishedDate(dates)
                .ifPresent(date -> {
                    bookInfo.setPublishedDate(StringUtil.cleanAndTrimToNullSubstring(date, DataPropertiesConst.BOOK_INFO_PUBLISHED_DATE_MAX_LENGTH));
                });
        bookInfo.setFileMetaInfo(fileMetaInfo);
        bookInfo.setFileExtractionStatus(FileExtractionStatusEnum.SUCCESS.getStatus());
        fileMetaInfo.setBookInfo(this.bookInfoRepository.save(bookInfo));
        FileMetaInfo savedFileMetaInfo = this.fileMetaInfoRepository.save(fileMetaInfo);

        savedFileMetaInfo = dataExtractorStrategyUtil.createAndAssociateTags(metadata.getSubjects(), savedFileMetaInfo);

        return savedFileMetaInfo;
    }

    private List<FileMetaInfo> findFileMetaInfoByIsbn(List<String> isbnList) {
        return this.fileMetaInfoRepository.findByIsbnList(isbnList);
    }

    private List<String> extractISBN(List<Identifier> identifiers) {
        return identifiers.stream()
                .filter(id -> id.getScheme() != null && id.getScheme().toLowerCase().contains("isbn"))
                .map(Identifier::getValue)
                .map(StringUtil::cleanAndTrimToNull)
                .filter(isbn -> !Objects.isNull(isbn))
                .map(isbn -> isbn.replace("-", ""))
                .toList();


    }

    private Optional<String> extractPublishedDate(List<Date> dates) {
        return dates.stream().filter(date -> date.getEvent() == null).map(Date::getValue).findFirst();
    }

    private String getFirstAvailable(List<String> list) {
        if (list != null && !list.isEmpty()) {
            return list.stream().findFirst().get();
        }
        return null;
    }
}