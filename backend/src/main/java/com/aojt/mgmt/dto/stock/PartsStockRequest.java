package com.aojt.mgmt.dto.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 部品在庫の作成・更新リクエスト。文字数の上限は {@code parts_stock_info} テーブルの桁数に合わせる。
 *
 * <p>カテゴリが部品の種別であること、センターが存在することは、サービス層で検証する。
 *
 * @param categoryId カテゴリID（種別が {@code part} のカテゴリ）
 * @param name 名称（255文字以内。同じセンターの中で重複禁止）
 * @param centerId 在庫センターID
 * @param description 説明（255文字以内）。任意
 * @param amount 数量（個）
 */
public record PartsStockRequest(
        @Schema(example = "1") @NotNull(message = "カテゴリは必須です")
        @Min(value = 1, message = "カテゴリIDは 1 以上を指定してください")
        Integer categoryId,

        @Schema(example = "カーボンフレーム 450mm") @NotBlank(message = "名称は必須です")
        @Size(max = 255, message = "名称は255文字以内で入力してください")
        @Pattern(regexp = "^[^<>]*$", message = "名称に < または > は使用できません")
        String name,

        @Schema(example = "1") @NotNull(message = "在庫センターは必須です")
        @Min(value = 1, message = "在庫センターIDは 1 以上を指定してください")
        Integer centerId,

        @Schema(example = "クアッドコプター用カーボンフレーム")
        @Size(max = 255, message = "説明は255文字以内で入力してください")
        @Pattern(regexp = "^[^<>]*$", message = "説明に < または > は使用できません")
        String description,

        @Schema(example = "85") @NotNull(message = "在庫数は必須です")
        @Min(value = 0, message = "在庫数は 0 以上で入力してください")
        @Max(value = 999_999_999, message = "在庫数が大きすぎます")
        Integer amount) {
}
