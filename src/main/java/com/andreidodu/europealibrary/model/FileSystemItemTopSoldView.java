package com.andreidodu.europealibrary.model;

import com.andreidodu.europealibrary.model.stripe.StripeProduct;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Getter
@Setter
@Entity
@Immutable
@Table(name = "v_el_file_system_item_top_sold")
public class FileSystemItemTopSoldView {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "row_number", updatable = false, nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_system_item_id", referencedColumnName = "id")
    private FileSystemItem fileSystemItem;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_meta_info_id", referencedColumnName = "id")
    private FileMetaInfo fileMetaInfo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stripe_product_id", referencedColumnName = "id")
    private StripeProduct stripeProduct;

    @Column(name = "sales_count")
    private Long salesCount;

}
