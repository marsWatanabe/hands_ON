package com.aojt.mgmt.dto.stock;

import com.aojt.mgmt.model.ProductsStock;
import java.time.LocalDateTime;

/**
 * 製品在庫のレスポンス。部品在庫の項目に、製品コードを追加する。
 *
 * @param stockId 在庫ID
 * @param productCode 製品コード
 * @param categoryId カテゴリID（未設定の既存行は null）
 * @param categoryName カテゴリ名（未設定は null）
 * @param name 名称
 * @param centerId 在庫センターID（未設定の既存行は null）
 * @param centerName 在庫センター名（未設定は null）
 * @param description 説明
 * @param amount 数量（個）
 * @param createDate 作成日時
 * @param updateDate 更新日時
 */
public record ProductsStockResponse(
        Integer stockId,
        String productCode,
        Integer categoryId,
        String categoryName,
        String name,
        Integer centerId,
        String centerName,
        String description,
        Integer amount,
        LocalDateTime createDate,
        LocalDateTime updateDate) {

    /**
     * エンティティからレスポンスを生成する。
     *
     * @param s 製品在庫
     * @return レスポンス
     */
    public static ProductsStockResponse from(ProductsStock s) {
        return new ProductsStockResponse(
                s.getStockId(),
                s.getProductCode(),
                s.getCategory() == null ? null : s.getCategory().getCategoryId(),
                s.getCategory() == null ? null : s.getCategory().getCategoryName(),
                s.getName(),
                s.getCenter() == null ? null : s.getCenter().getCenterId(),
                s.getCenter() == null ? null : s.getCenter().getCenterName(),
                s.getDescription(),
                s.getAmount(),
                s.getCreateDate(),
                s.getUpdateDate());
    }
}
