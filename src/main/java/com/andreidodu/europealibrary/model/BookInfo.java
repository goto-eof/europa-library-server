package com.andreidodu.europealibrary.model;

import com.andreidodu.europealibrary.model.common.ModelCommon;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "el_book_info", uniqueConstraints = {@UniqueConstraint(columnNames = {"file_meta_info_id", "authors", "publisher"})})
@EntityListeners(AuditingEntityListener.class)
public class BookInfo extends ModelCommon {
    @Id
    @GeneratedValue(generator = "el_book_info_seq", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = "el_book_info_seq", sequenceName = "el_book_info_seq", allocationSize = 50)
    private Long id;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String authors;

    @Column(length = 2000)
    private String note;

    @Column(length = 10)
    private String isbn10;

    @Column(length = 13)
    private String isbn13;

    @Column(length = 100)
    private String publisher;

    @Column(length = 10)
    private String language;

    @Column(name = "num_pages")
    private Integer numberOfPages;

    @Column(name = "published_date", length = 50)
    private String publishedDate;

    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "ratings_count")
    private Long ratingsCount;

    @Column(name = "file_extraction_status")
    private Integer fileExtractionStatus;

    @Column(name = "web_retrievement_status")
    private Integer webRetrievementStatus;

    @Column(name = "manual_lock")
    private Integer manualLock;

    @Column(name = "record_status")
    private Integer recordStatus;

    @Column(name = "is_corrupted")
    private Boolean isCorrupted;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_meta_info_id", referencedColumnName = "id")
    private FileMetaInfo fileMetaInfo;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            uniqueConstraints = {@UniqueConstraint(columnNames = {"book_info_id", "category_id"})},
            name = "el_book_info_category",
            joinColumns = {@JoinColumn(name = "book_info_id")},
            inverseJoinColumns = {@JoinColumn(name = "category_id")}
    )
    private List<Category> categoryList;

    @OneToOne(mappedBy = "bookInfo", fetch = FetchType.LAZY, optional = false)
    private FileSystemItemTopRatedView fileSystemItemTopRatedView;

    @Override
    public String toString() {
        return "BookInfo{" +
                "id=" + id +
                '}';
    }
}
