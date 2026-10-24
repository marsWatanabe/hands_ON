package com.aojt.mgmt.dto.stock;

import com.aojt.mgmt.dto.common.PagingRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 部品在庫一覧取得のクエリパラメータ。製品在庫は {@link ProductsStockSearchRequest}（製品コードの条件を追加）。
 *
 * <p>未指定の項目は、既定値（{@code page=1}、{@code size=20}、{@code sort=categoryName}、{@code order=asc}）になる。
 */
public class StockSearchRequest extends PagingRequest {

    /**
     * ソート項目の許可リスト（正規表現）。部品と製品の和集合で、実際に使える項目はサービス層の許可リストで
     * 絞る（製品コードは製品在庫のみ）。
     */
    private static final String SORT_PATTERN =
            "stockId|name|amount|categoryName|centerName|createDate|updateDate|productCode";

    @Schema(description = "カテゴリID（完全一致）")
    @Min(value = 1, message = "カテゴリIDは 1 以上を指定してください")
    private Integer categoryId;

    @Schema(description = "在庫センターID（完全一致）")
    @Min(value = 1, message = "在庫センターIDは 1 以上を指定してください")
    private Integer centerId;

    @Schema(description = "名称（部分一致）")
    @Size(max = 100, message = "名称は100文字以内で入力してください")
    private String name;

    @Schema(description = "数量の下限（以上）")
    @Min(value = 0, message = "個数（以上）は 0 以上を指定してください")
    private Integer amountMin;

    @Schema(description = "数量の上限（以下）")
    @Min(value = 0, message = "個数（以下）は 0 以上を指定してください")
    private Integer amountMax;

    @Schema(defaultValue = "categoryName", allowableValues = {"stockId", "name", "amount", "categoryName",
            "centerName", "createDate", "updateDate"})
    @Pattern(regexp = SORT_PATTERN, message = "並び替え項目が不正です")
    private String sort = "categoryName";

    /** 既定のソート順（昇順）にする。 */
    public StockSearchRequest() {
        setOrder("asc");
    }

    /**
     * カテゴリIDの検索条件を返す。
     *
     * @return カテゴリID。未指定は null
     */
    public Integer getCategoryId() {
        return categoryId;
    }

    /**
     * カテゴリIDの検索条件を設定する。
     *
     * @param categoryId カテゴリID
     */
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * 在庫センターIDの検索条件を返す。
     *
     * @return 在庫センターID。未指定は null
     */
    public Integer getCenterId() {
        return centerId;
    }

    /**
     * 在庫センターIDの検索条件を設定する。
     *
     * @param centerId 在庫センターID
     */
    public void setCenterId(Integer centerId) {
        this.centerId = centerId;
    }

    /**
     * 名称の検索条件を返す。
     *
     * @return 名称（部分一致）
     */
    public String getName() {
        return name;
    }

    /**
     * 名称の検索条件を設定する。空白だけの値は条件なしにする。
     *
     * @param name 名称
     */
    public void setName(String name) {
        this.name = normalizeSearchText(name);
    }

    /**
     * 数量の下限を返す。
     *
     * @return 数量の下限（以上）。未指定は null
     */
    public Integer getAmountMin() {
        return amountMin;
    }

    /**
     * 数量の下限を設定する。
     *
     * @param amountMin 数量の下限
     */
    public void setAmountMin(Integer amountMin) {
        this.amountMin = amountMin;
    }

    /**
     * 数量の上限を返す。
     *
     * @return 数量の上限（以下）。未指定は null
     */
    public Integer getAmountMax() {
        return amountMax;
    }

    /**
     * 数量の上限を設定する。
     *
     * @param amountMax 数量の上限
     */
    public void setAmountMax(Integer amountMax) {
        this.amountMax = amountMax;
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
