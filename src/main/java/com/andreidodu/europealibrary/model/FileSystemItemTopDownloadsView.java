package com.andreidodu.europealibrary.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Getter
@Setter
@Entity
@Immutable
@Table(name = "v_el_file_system_item_top_downloads")
public class FileSystemItemTopDownloadsView {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "row_number", updatable = false, nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_system_item_id", referencedColumnName = "id")
    private FileSystemItem fileSystemItem;

    @Column(name = "download_count")
    private Long downloadsCount;

}
