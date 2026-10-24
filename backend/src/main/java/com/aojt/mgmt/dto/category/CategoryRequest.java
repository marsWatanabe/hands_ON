package com.aojt.mgmt.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * カテゴリの作成・更新リクエスト。文字数の上限は {@code category_info} テーブルの桁数に合わせる。
 *
 * @param categoryType カテゴリ種別（{@code part} または {@code product}）
 * @param categoryName カテゴリ名（20文字以内。同じ種別の中で重複禁止）
 * @param description 説明（255文字以内）。任意
 */
public record CategoryRequest(
        @Schema(example = "part") @NotBlank(message = "カテゴリ種別は必須です")
        @Pattern(regexp = "part|product", message = "カテゴリ種別は part または product を指定してください")
        String categoryType,

        @Schema(example = "フレーム") @NotBlank(message = "カテゴリ名は必須です")
        @Size(max = 20, message = "カテゴリ名は20文字以内で入力してください")
        @Pattern(regexp = "^[^<>]*$", message = "カテゴリ名に < または > は使用できません")
        String categoryName,

        @Schema(example = "ドローンの機体フレーム") @Size(max = 255, message = "説明は255文字以内で入力してください")
        @Pattern(regexp = "^[^<>]*$", message = "説明に < または > は使用できません")
        String description) {
}
