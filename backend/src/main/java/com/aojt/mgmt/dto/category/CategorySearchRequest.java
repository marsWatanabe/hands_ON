package com.aojt.mgmt.dto.category;

import com.aojt.mgmt.dto.common.PagingRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * カテゴリ一覧取得のクエリパラメータ。
 *
 * <p>未指定の項目は、既定値（{@code page=1}、{@code size=20}、{@code sort=categoryType}、{@code order=desc}）になる。
 */
public class CategorySearchRequest extends PagingRequest {

    /** ソート項目の許可リスト（正規表現）。サービス層の許可リストと一致させる。 */
    private static final String SORT_PATTERN = "categoryId|categoryType|categoryName|createDate|updateDate";

    @Schema(description = "カテゴリ種別（part: 部品、product: 製品）。未指定はすべて")
    @Pattern(regexp = "part|product", message = "カテゴリ種別は part または product を指定してください")
    private String categoryType;

    @Schema(description = "カテゴリ名（部分一致）")
    @Size(max = 20, message = "カテゴリ名は20文字以内で入力してください")
    private String categoryName;

    @Schema(defaultValue = "categoryType", allowableValues = {"categoryId", "categoryType", "categoryName",
            "createDate", "updateDate"})
    @Pattern(regexp = SORT_PATTERN, message = "並び替え項目が不正です")
    private String sort = "categoryType";

    /**
     * カテゴリ種別の検索条件を返す。
     *
     * @return {@code part}、{@code product}。未指定は null
     */
    public String getCategoryType() {
        return categoryType;
    }

    /**
     * カテゴリ種別の検索条件を設定する。空白だけの値は条件なしにする。
     *
     * @param categoryType カテゴリ種別
     */
    public void setCategoryType(String categoryType) {
        this.categoryType = normalizeSearchText(categoryType);
    }

    /**
     * カテゴリ名の検索条件を返す。
     *
     * @return カテゴリ名（部分一致）
     */
    public String getCategoryName() {
        return categoryName;
    }

    /**
     * カテゴリ名の検索条件を設定する。空白だけの値は条件なしにする。
     *
     * @param categoryName カテゴリ名
     */
    public void setCategoryName(String categoryName) {
        this.categoryName = normalizeSearchText(categoryName);
    }

    /**
     * ソート項目を返す。
     *
     * @return ソート項目
     */
    public String getSort() {
        return sort;
    }

    /**
     * ソート項目を設定する。
     *
     * @param sort ソート項目
     */
    public void setSort(String sort) {
        this.sort = sort;
    }
}
