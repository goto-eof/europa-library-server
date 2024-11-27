package com.andreidodu.europealibrary.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "el_application_settings")
public class ApplicationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_lock")
    private Boolean applicationLock;

    @Column(name = "protected_downloads")
    private Boolean protectedDownloadsEnabled;

    @Column(name = "custom_description_enabled")
    private Boolean customDescriptionEnabled;

    @Column(name = "home_page_w_featured_books_enabled")
    private Boolean featuredBooksWidgetEnabled;

    @Column(name = "home_page_w_featured_book_enabled")
    private Boolean featuredBookWidgetEnabled;

    @Column(name = "home_page_w_new_books_enabled")
    private Boolean newBooksWidgetEnabled;

    @Column(name = "home_page_w_popular_books_enabled")
    private Boolean popularBooksWidgetEnabled;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "featured_file_meta_info_id", referencedColumnName = "id")
    private FileMetaInfo featuredFileMetaInfo;

}