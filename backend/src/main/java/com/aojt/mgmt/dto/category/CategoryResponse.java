package com.aojt.mgmt.dto.category;

import com.aojt.mgmt.model.Category;
import java.time.LocalDateTime;

/**
 * カテゴリのレスポンス。削除フラグは含めない。
 *
 * @param categoryId カテゴリID
 * @param categoryType カテゴリ種別（{@code part} または {@code product}）
 * @param categoryName カテゴリ名
 * @param description 説明
 * @param createDate 作成日時
 * @param updateDate 更新日時
 */
public record CategoryResponse(
        Integer categoryId,
        String categoryType,
        String categoryName,
        String description,
        LocalDateTime createDate,
        LocalDateTime updateDate) {

    /**
     * エンティティからレスポンスを生成する。
     *
     * @param c カテゴリ
     * @return レスポンス
     */
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(
                c.getCategoryId(),
                c.getCategoryType(),
                c.getCategoryName(),
                c.getDescription(),
                c.getCreateDate(),
                c.getUpdateDate());
    }
}
