package com.andreidodu.europealibrary.service.impl;

import com.andreidodu.europealibrary.batch.indexer.enums.JobStepEnum;
import com.andreidodu.europealibrary.constants.ApplicationConst;
import com.andreidodu.europealibrary.constants.CacheConst;
import com.andreidodu.europealibrary.constants.PersistenceConst;
import com.andreidodu.europealibrary.dto.*;
import com.andreidodu.europealibrary.dto.common.FileMetaInfoDTO;
import com.andreidodu.europealibrary.enums.OrderEnum;
import com.andreidodu.europealibrary.enums.StripeCustomerProductsOwnedStatus;
import com.andreidodu.europealibrary.exception.ApplicationException;
import com.andreidodu.europealibrary.exception.EntityNotFoundException;
import com.andreidodu.europealibrary.exception.ValidationException;
import com.andreidodu.europealibrary.mapper.*;
import com.andreidodu.europealibrary.model.FileMetaInfo;
import com.andreidodu.europealibrary.model.FileSystemItem;
import com.andreidodu.europealibrary.model.stripe.StripeCustomerProductsOwned;
import com.andreidodu.europealibrary.model.stripe.StripeProduct;
import com.andreidodu.europealibrary.repository.*;
import com.andreidodu.europealibrary.service.CursoredFileSystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CursoredFileSystemServiceImpl extends CursoredServiceCommon implements CursoredFileSystemService {
    private final StripeCustomerProductsOwnedRepository stripeCustomerProductsOwnedRepository;
    private final FileSystemItemRepository fileSystemItemRepository;
    private final FileSystemItemFullMapper fileSystemItemFullMapper;
    private final FileSystemItemLessMapper fileSystemItemLessMapper;
    private final ItemAndFrequencyMapper itemAndFrequencyMapper;
    private final FileExtensionMapper fileExtensionMapper;
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Override
    public CursoredFileSystemItemDTO readDirectory(CursorRequestDTO cursorRequestDTO) {
        return Optional.ofNullable(cursorRequestDTO.getParentId())
                .map(id -> this.manageCaseReadDirectoryIdProvided(cursorRequestDTO))
                .orElse(manageCaseReadDirectoryNoIdProvided());
    }

    @Override
    public CursoredFileSystemItemDTO readDirectory() {
        return manageCaseReadDirectoryNoIdProvided();
    }

    @Override
    public CursoredCategoryDTO retrieveByCategoryId(CursorRequestDTO cursorRequestDTO) {
        Optional.ofNullable(cursorRequestDTO.getParentId())
                .orElseThrow(() -> new EntityNotFoundException("Invalid category id"));
        List<FileSystemItem> children = this.fileSystemItemRepository.retrieveChildrenByCursoredCategoryId(cursorRequestDTO);
        CursoredCategoryDTO cursoredCategoryDTO = new CursoredCategoryDTO();
        List<FileSystemItem> childrenList = limit(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC);
        cursoredCategoryDTO.setChildrenList(childrenList.stream()
                .map(this.fileSystemItemLessMapper::toDTOWithParentDTORecursively)
                .collect(Collectors.toList()));
        super.calculateNextId(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC)
                .ifPresent(cursoredCategoryDTO::setNextCursor);
        this.categoryRepository.findById(cursorRequestDTO.getParentId())
                .ifPresent(category -> cursoredCategoryDTO.setCategory(this.categoryMapper.toDTO(category)));
        return cursoredCategoryDTO;
    }

    @Override
    public CursoredTagDTO retrieveByTagId(CursorRequestDTO cursorRequestDTO) {
        Optional.ofNullable(cursorRequestDTO.getParentId())
                .orElseThrow(() -> new EntityNotFoundException("Invalid tag id"));
        List<FileSystemItem> children = this.fileSystemItemRepository.retrieveChildrenByCursoredTagId(cursorRequestDTO);
        CursoredTagDTO cursoredTagDTO = new CursoredTagDTO();
        List<FileSystemItem> childrenList = limit(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC);
        cursoredTagDTO.setChildrenList(childrenList.stream()
                .map(this.fileSystemItemLessMapper::toDTOWithParentDTORecursively)
                .collect(Collectors.toList()));
        super.calculateNextId(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC)
                .ifPresent(cursoredTagDTO::setNextCursor);
        this.tagRepository.findById(cursorRequestDTO.getParentId())
                .ifPresent(tag -> cursoredTagDTO.setTag(this.tagMapper.toDTO(tag)));
        return cursoredTagDTO;
    }

    private CursoredFileSystemItemDTO manageCaseReadDirectoryIdProvided(CursorRequestDTO cursorRequestDTO) {
        CursoredFileSystemItemDTO cursoredFileSystemItemDTO = new CursoredFileSystemItemDTO();
        FileSystemItem parent = checkFileSystemItemExistence(cursorRequestDTO.getParentId());
        cursoredFileSystemItemDTO.setParent(this.fileSystemItemLessMapper.toDTOWithoutChildrenAndParentLess(parent));
        this.fileSystemItemLessMapper.toParentDTORecursively(cursoredFileSystemItemDTO.getParent(), parent);
        List<FileSystemItem> children = this.fileSystemItemRepository.retrieveChildrenByCursor(cursorRequestDTO);
        cursoredFileSystemItemDTO.setChildrenList(this.fileSystemItemLessMapper.toDTOLess(limit(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC)));
        super.calculateNextId(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC)
                .ifPresent(cursoredFileSystemItemDTO::setNextCursor);
        return cursoredFileSystemItemDTO;
    }


    private CursoredFileSystemItemDTO manageCaseReadDirectoryNoIdProvided() {
        FileSystemItem fileSystemItem = this.fileSystemItemRepository.findByNoParent(JobStepEnum.READY.getStepNumber())
                .orElseThrow(() -> new EntityNotFoundException("Entity not found"));
        CursorRequestDTO cursorRequestDTO = new CursorRequestDTO();
        cursorRequestDTO.setParentId(fileSystemItem.getId());
        cursorRequestDTO.setLimit(ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE);
        return manageCaseReadDirectoryIdProvided(cursorRequestDTO);
    }

    private FileSystemItem checkFileSystemItemExistence(Long id) {
        return this.fileSystemItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found"));
    }

    @Override
    @Cacheable(cacheNames = {CacheConst.CACHE_NAME_EXTENSIONS})
    public List<FileExtensionDTO> getAllExtensions() {
        return this.fileExtensionMapper.toDTO(this.fileSystemItemRepository.retrieveExtensionsInfo());
    }

    @Override
    public CursoredFileExtensionDTO retrieveByFileExtension(CursorTypeRequestDTO cursorTypeRequestDTO) {
        Optional.ofNullable(cursorTypeRequestDTO.getExtension())
                .orElseThrow(() -> new EntityNotFoundException("Invalid file extension"));
        List<FileSystemItem> children = this.fileSystemItemRepository.retrieveChildrenByCursoredFileExtension(cursorTypeRequestDTO);
        CursoredFileExtensionDTO cursoredFileExtensionDTO = new CursoredFileExtensionDTO();
        List<FileSystemItem> childrenList = limit(children, cursorTypeRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC);
        cursoredFileExtensionDTO.setChildrenList(childrenList.stream()
                .map(this.fileSystemItemLessMapper::toDTOWithParentDTORecursively)
                .collect(Collectors.toList()));
        super.calculateNextId(children, cursorTypeRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC)
                .ifPresent(cursoredFileExtensionDTO::setNextCursor);
        cursoredFileExtensionDTO.setExtension(cursorTypeRequestDTO.getExtension());
        return cursoredFileExtensionDTO;
    }

    @Override
    @Cacheable(cacheNames = {CacheConst.CACHE_NAME_LANGUAGES})
    @Transactional(timeout = PersistenceConst.TIMEOUT_DEMANDING_QUERIES_SECONDS, propagation = Propagation.REQUIRED)
    public List<ItemAndFrequencyDTO> retrieveAllLanguages() {
        return this.itemAndFrequencyMapper.toDTO(this.fileSystemItemRepository.retrieveLanguagesInfo());
    }


    @Override
    @Cacheable(cacheNames = {CacheConst.CACHE_NAME_PUBLISHERS})
    @Transactional(timeout = PersistenceConst.TIMEOUT_DEMANDING_QUERIES_SECONDS, propagation = Propagation.REQUIRED)
    public List<ItemAndFrequencyDTO> retrieveAllPublishers() {
        return this.itemAndFrequencyMapper.toDTO(this.fileSystemItemRepository.retrievePublishersInfo());
    }

    @Override
    public DownloadDTO retrieveResourceForDownload(String username, Long fileSystemId) {
        return this.fileSystemItemRepository.findById(fileSystemId)
                .map(fileSystemItem -> {
                    try {
                        checkIfUserAllowedToDownloadResource(username, fileSystemItem);
                        fileSystemItem.setDownloadCount(fileSystemItem.getDownloadCount() + 1);
                        this.fileSystemItemRepository.save(fileSystemItem);
                        File file = new File(fileSystemItem.getBasePath() + "/" + fileSystemItem.getName());
                        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
                        DownloadDTO downloadDTO = new DownloadDTO();
                        downloadDTO.setInputStreamResource(resource);
                        downloadDTO.setFileSize(file.length());
                        downloadDTO.setFileName(fileSystemItem.getName());
                        return downloadDTO;
                    } catch (IOException e) {
                        throw new ApplicationException("Unable to retrieve file");
                    }
                }).orElseThrow();
    }

    private void checkIfUserAllowedToDownloadResource(String username, FileSystemItem fileSystemItem) {
        FileMetaInfo fileMetaInfo = fileSystemItem.getFileMetaInfo();
        if (BooleanUtils.isTrue(fileMetaInfo.getOnSale())) {
            stripeCustomerProductsOwnedRepository.findFirstByStripeCustomer_User_usernameAndStripeProduct_FileMetaInfo_idAndStatus(username, fileMetaInfo.getId(), StripeCustomerProductsOwnedStatus.PURCHASED)
                    .orElseThrow(() -> new AccessDeniedException("Product not purchased"));
        }
    }

    @Override
    public FileSystemItemDTO get(String username, Long fileSystemItemId) {
        FileSystemItemDTO fileSystemItemDTO = this.fileSystemItemRepository.findById(fileSystemItemId)
                .map(this.fileSystemItemFullMapper::toDTOFull)
                .orElseThrow(() -> new ApplicationException("Item not found"));
        Optional.ofNullable(username)
                .ifPresent(usernameValue -> {
                    fileSystemItemDTO.getFileMetaInfo().setDownloadable(calculateIsDownloadableByFileMetaInfoId(fileSystemItemDTO.getFileMetaInfo(), usernameValue));
                });
        return fileSystemItemDTO;
    }

    @Override
    public FileSystemItemDTO getByFileMetaInfoId(String username, Long fileMetaInfoId) {
        FileSystemItemDTO fileSystemItemDTO = this.fileSystemItemRepository.findByFileMetaInfo_id(fileMetaInfoId)
                .map(this.fileSystemItemFullMapper::toDTOFull)
                .orElseThrow(() -> new ApplicationException("Item not found"));
        Optional.ofNullable(username)
                .ifPresent(usernameValue -> {
                    fileSystemItemDTO.getFileMetaInfo().setDownloadable(calculateIsDownloadableByFileMetaInfoId(fileSystemItemDTO.getFileMetaInfo(), usernameValue));
                });
        return fileSystemItemDTO;
    }

    private boolean calculateIsDownloadableByFileMetaInfoId(FileMetaInfoDTO fileMetaInfo, String username) {
        return BooleanUtils.isTrue(fileMetaInfo.getOnSale()) ? stripeCustomerProductsOwnedRepository.existsByStripeCustomer_User_usernameAndStripeProduct_FileMetaInfo_idAndStatus(username, fileMetaInfo.getId(), StripeCustomerProductsOwnedStatus.PURCHASED) : true;
    }

    @Override
    public SearchResultDTO<SearchFileSystemItemRequestDTO, FileSystemItemDTO> search(SearchFileSystemItemRequestDTO searchFileSystemItemRequestDTO) {
        SearchResultDTO<SearchFileSystemItemRequestDTO, FileSystemItemDTO> result = new SearchResultDTO<>();
        List<FileSystemItem> children = this.fileSystemItemRepository.search(searchFileSystemItemRequestDTO);
        result.setChildrenList(this.fileSystemItemLessMapper.toDTOLess(limit(children, searchFileSystemItemRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC)));
        super.calculateNextId(children, searchFileSystemItemRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC)
                .ifPresent(result::setNextCursor);
        result.setQuery(searchFileSystemItemRequestDTO);
        return result;
    }

    @Override
    public GenericCursoredResponseDTO<String, FileSystemItemDTO> retrieveByLanguage(GenericCursorRequestDTO<String> cursorRequestDTO) {
        Optional.ofNullable(cursorRequestDTO.getParent())
                .orElseThrow(() -> new EntityNotFoundException("Invalid parent"));
        List<FileSystemItem> children = this.fileSystemItemRepository.retrieveChildrenByCursoredLanguage(cursorRequestDTO);
        GenericCursoredResponseDTO<String, FileSystemItemDTO> cursoredTagDTO = new GenericCursoredResponseDTO<>();
        List<FileSystemItem> childrenList = limit(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC);
        cursoredTagDTO.setChildrenList(childrenList.stream()
                .map(this.fileSystemItemLessMapper::toDTOWithParentDTORecursively)
                .collect(Collectors.toList()));
        super.calculateNextId(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC).ifPresent(cursoredTagDTO::setNextCursor);
        cursoredTagDTO.setParent(cursorRequestDTO.getParent());
        return cursoredTagDTO;
    }

    @Override
    public GenericCursoredResponseDTO<String, FileSystemItemDTO> retrieveByPublisher(GenericCursorRequestDTO<String> cursorRequestDTO) {
        Optional.ofNullable(cursorRequestDTO.getParent())
                .orElseThrow(() -> new EntityNotFoundException("Invalid parent"));
        List<FileSystemItem> children = this.fileSystemItemRepository.retrieveChildrenByCursoredPublisher(cursorRequestDTO);
        GenericCursoredResponseDTO<String, FileSystemItemDTO> cursoredTagDTO = new GenericCursoredResponseDTO<>();
        List<FileSystemItem> childrenList = limit(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC);
        cursoredTagDTO.setChildrenList(childrenList.stream()
                .map(this.fileSystemItemLessMapper::toDTOWithParentDTORecursively)
                .collect(Collectors.toList()));
        super.calculateNextId(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC).ifPresent(cursoredTagDTO::setNextCursor);
        cursoredTagDTO.setParent(cursorRequestDTO.getParent());
        return cursoredTagDTO;
    }

    @Override
    @Cacheable(cacheNames = {CacheConst.CACHE_NAME_PUBLISHED_DATES})
    @Transactional(timeout = PersistenceConst.TIMEOUT_DEMANDING_QUERIES_SECONDS, propagation = Propagation.REQUIRED)
    public List<ItemAndFrequencyDTO> retrieveAllPublishedDates() {
        return this.itemAndFrequencyMapper.toDTO(this.fileSystemItemRepository.retrievePublishedDatesInfo());
    }

    @Override
    public GenericCursoredResponseDTO<String, FileSystemItemDTO> retrieveByPublishedDate(GenericCursorRequestDTO<String> cursorRequestDTO) {
        Optional.ofNullable(cursorRequestDTO.getParent())
                .orElseThrow(() -> new EntityNotFoundException("Invalid parent"));
        List<FileSystemItem> children = this.fileSystemItemRepository.retrieveChildrenByCursoredPublishedDate(cursorRequestDTO);
        GenericCursoredResponseDTO<String, FileSystemItemDTO> cursoredTagDTO = new GenericCursoredResponseDTO<>();
        List<FileSystemItem> childrenList = limit(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC);
        cursoredTagDTO.setChildrenList(childrenList.stream()
                .map(this.fileSystemItemLessMapper::toDTOWithParentDTORecursively)
                .collect(Collectors.toList()));
        super.calculateNextId(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.ASC)
                .ifPresent(cursoredTagDTO::setNextCursor);
        cursoredTagDTO.setParent(cursorRequestDTO.getParent());
        return cursoredTagDTO;
    }

    @Override
    public GenericCursoredResponseDTO<String, FileSystemItemDTO> retrieveCursoredByRating(CursorCommonRequestDTO cursorRequestDTO) {
        GenericCursoredResponseDTO<String, FileSystemItemDTO> responseDTO = new GenericCursoredResponseDTO<>();
        responseDTO.setParent("By Top Rated");
        PairDTO<List<FileSystemItem>, Long> pair = this.fileSystemItemRepository.retrieveChildrenByCursoredRating(cursorRequestDTO);
        responseDTO.setChildrenList(pair.getVal1().stream()
                .map(fileSystemItemLessMapper::toDTOWithParentDTORecursively)
                .collect(Collectors.toList()));
        responseDTO.setNextCursor(pair.getVal2());
        return responseDTO;
    }

    @Override
    public GenericCursoredResponseDTO<String, FileSystemItemDTO> retrieveCursoredByDownloadCount(CursoredRequestByFileTypeDTO cursoredRequestByFileTypeDTO) {
        return this.genericRetrieveCursoredByDownloadCount(cursoredRequestByFileTypeDTO, fileSystemItemLessMapper::toDTOWithParentDTORecursively);
    }

    private <T> GenericCursoredResponseDTO<String, T> genericRetrieveCursoredByDownloadCount(CursoredRequestByFileTypeDTO cursoredRequestByFileTypeDTO, Function<FileSystemItem, T> toDTO) {
        GenericCursoredResponseDTO<String, T> responseDTO = new GenericCursoredResponseDTO<>();
        responseDTO.setParent("DownloadCount");
        PairDTO<List<FileSystemItem>, Long> pair = this.fileSystemItemRepository.retrieveCursoredByDownloadCount(cursoredRequestByFileTypeDTO);
        responseDTO.setChildrenList(pair.getVal1().stream()
                .map(toDTO)
                .collect(Collectors.toList()));
        responseDTO.setNextCursor(pair.getVal2());
        return responseDTO;
    }

    @Override
    public GenericCursoredResponseDTO<String, FileSystemItemHighlightDTO> retrieveCursoredByDownloadCountHighlight(CursoredRequestByFileTypeDTO cursoredRequestByFileTypeDTO) {
        return this.genericRetrieveCursoredByDownloadCount(cursoredRequestByFileTypeDTO, fileSystemItemLessMapper::toHighlightDTO);
    }

    @Override
    public GenericCursoredResponseDTO<String, FileSystemItemDTO> retrieveNewCursored(CursorCommonRequestDTO cursorRequestDTO) {
        return this.genericRetrieveNewCursored(cursorRequestDTO, this.fileSystemItemLessMapper::toDTOWithParentDTORecursively);
    }

    @Override
    public GenericCursoredResponseDTO<String, FileSystemItemHighlightDTO> retrieveNewCursoredHighlight(CursorCommonRequestDTO cursorRequestDTO) {
        return this.genericRetrieveNewCursored(cursorRequestDTO, this.fileSystemItemLessMapper::toHighlightDTO);
    }

    public <T> GenericCursoredResponseDTO<String, T> genericRetrieveNewCursored(CursorCommonRequestDTO cursorRequestDTO, Function<FileSystemItem, T> toDTO) {
        GenericCursoredResponseDTO<String, T> responseDTO = new GenericCursoredResponseDTO<>();
        responseDTO.setParent("New");
        List<FileSystemItem> children = this.fileSystemItemRepository.retrieveNewCursored(cursorRequestDTO);
        List<FileSystemItem> childrenList = limit(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.DESC);
        responseDTO.setChildrenList(childrenList.stream()
                .map(toDTO)
                .collect(Collectors.toList()));
        super.calculateNextId(children, cursorRequestDTO.getLimit(), ApplicationConst.FILE_SYSTEM_EXPLORER_MAX_ITEMS_RETRIEVE, OrderEnum.DESC)
                .ifPresent(responseDTO::setNextCursor);
        return responseDTO;
    }

}
