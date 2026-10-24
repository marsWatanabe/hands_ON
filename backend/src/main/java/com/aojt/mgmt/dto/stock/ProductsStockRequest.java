package com.aojt.mgmt.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 製品在庫の作成・更新リクエスト。部品在庫の項目に、製品コードを追加する。
 *
 * <p>カテゴリが製品の種別であること、センターが存在することは、サービス層で検証する。
 *
 * @param productCode 製品コード（20文字以内の半角英数字・ハイフン・アンダースコア。削除済みを含めて重複禁止）
 * @param categoryId カテゴリID（種別が {@code product} のカテゴリ）
 * @param name 名称（255文字以内。同じセンターの中で重複禁止）
 * @param centerId 在庫センターID
 * @param description 説明（255文字以内）。任意
 * @param amount 数量（個）
 */
public record ProductsStockRequest(
        @Schema(example = "PRD-000001") @NotBlank(message = "製品コードは必須です")
        @Size(max = 20, message = "製品コードは20文字以内で入力してください")
        @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "製品コードは半角英数字、ハイフン、アンダースコアで入力してください")
        String productCode,

        @Schema(example = "11") @NotNull(message = "カテゴリは必須です")
        @Min(value = 1, message = "カテゴリIDは 1 以上を指定してください")
        Integer categoryId,

        @Schema(example = "災害用ドローン-X Pro") @NotBlank(message = "名称は必須です")
        @Size(max = 255, message = "名称は255文字以内で入力してください")
        @Pattern(regexp = "^[^<>]*$", message = "名称に < または > は使用できません")
        String name,

        @Schema(example = "1") @NotNull(message = "在庫センターは必須です")
        @Min(value = 1, message = "在庫センターIDは 1 以上を指定してください")
        Integer centerId,

        @Schema(example = "耐候性・長時間飛行対応災害救助ドローン")
        @Size(max = 255, message = "説明は255文字以内で入力してください")
        @Pattern(regexp = "^[^<>]*$", message = "説明に < または > は使用できません")
        String description,

        @Schema(example = "12") @NotNull(message = "在庫数は必須です")
        @Min(value = 0, message = "在庫数は 0 以上で入力してください")
        @Max(value = 999_999_999, message = "在庫数が大きすぎます")
        Integer amount) {
}
