package com.aojt.mgmt.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 製品在庫一覧取得のクエリパラメータ。部品在庫の条件に、製品コードを追加する。
 */
public class ProductsStockSearchRequest extends StockSearchRequest {

    @Schema(description = "製品コード（部分一致）")
    @Size(max = 20, message = "製品コードは20文字以内で入力してください")
    private String productCode;

    /**
     * 製品コードの検索条件を返す。
     *
     * @return 製品コード（部分一致）。未指定は null
     */
    public String getProductCode() {
        return productCode;
    }

    /**
     * 製品コードの検索条件を設定する。空白だけの値は条件なしにする。
     *
     * @param productCode 製品コード
     */
    public void setProductCode(String productCode) {
        this.productCode = normalizeSearchText(productCode);
    }
}
