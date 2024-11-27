package com.andreidodu.europealibrary.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Getter
@Setter
@Entity
@Immutable
@Table(name = "v_el_file_system_item_top_rated")
public class FileSystemItemTopRatedView {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "row_number", updatable = false, nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_system_item_id", referencedColumnName = "id")
    private FileSystemItem fileSystemItem;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_info_id", referencedColumnName = "id")
    private BookInfo bookInfo;

    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "ratings_count")
    private Long ratingsCount;

}
