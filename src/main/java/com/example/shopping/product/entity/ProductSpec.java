package com.example.shopping.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 商品規格表的一列,例如「材質:純棉」 */
@Entity
@Table(name = "product_spec")
@Getter
@Setter
@NoArgsConstructor
public class ProductSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "spec_name", nullable = false, length = 30)
    private String name;

    /** 欄位不叫 value:在 H2 / 部分資料庫是保留字 */
    @Column(name = "spec_value", nullable = false, length = 200)
    private String value;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
