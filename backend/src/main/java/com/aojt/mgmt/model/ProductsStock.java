package com.aojt.mgmt.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 製品在庫（テーブル {@code products_stock_info}）。カテゴリの種別は {@link Category#TYPE_PRODUCT}。
 */
@Entity
@Table(name = "products_stock_info")
public class ProductsStock extends StockItem {

    /** 製品コード（20文字、削除済みを含めて一意）。 */
    @Column(name = "product_code", nullable = false, length = 20)
    private String productCode;

    /**
     * 製品コードを返す。
     *
     * @return 製品コード
     */
    public String getProductCode() {
        return productCode;
    }

    /**
     * 製品コードを設定する。
     *
     * @param productCode 製品コード
     */
    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }
}
