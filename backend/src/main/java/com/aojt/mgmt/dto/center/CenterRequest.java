package com.aojt.mgmt.dto.center;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 在庫センターの作成・更新リクエスト。文字数の上限は {@code center_info} テーブルの桁数に合わせる。
 *
 * <p>現在保管容量が最大保管容量を超えないこと（業務ルール）は、サービス層で検証する。
 *
 * @param centerName 在庫センター名（20文字以内）
 * @param postCode 郵便番号（{@code 123-4567} の形式）
 * @param address 住所
 * @param phoneNumber 電話番号（{@code 03-1234-5678} の形式）
 * @param managerName 管理者名
 * @param operationalStatus 稼働状況（0: 稼働中、1: 稼働停止）
 * @param maxStorageCapacity 最大保管容量（m3）。任意
 * @param currentStorageCapacity 現在保管容量（m3）。任意
 * @param notes 備考
 */
public record CenterRequest(
        @Schema(example = "大阪センター") @NotBlank(message = "在庫センター名は必須です")
        @Size(max = 20, message = "在庫センター名は20文字以内で入力してください")
        @Pattern(regexp = "^[^<>]*$", message = "在庫センター名に < または > は使用できません")
        String centerName,

        @Schema(example = "530-0001") @NotBlank(message = "郵便番号は必須です")
        @Size(max = 10, message = "郵便番号は10文字以内で入力してください")
        @Pattern(regexp = "^\\d{3}-\\d{4}$", message = "郵便番号は 123-4567 の形式で入力してください")
        String postCode,

        @Schema(example = "大阪府大阪市北区梅田1-1-1") @NotBlank(message = "住所は必須です")
        @Size(max = 255, message = "住所は255文字以内で入力してください")
        @Pattern(regexp = "^[^<>]*$", message = "住所に < または > は使用できません")
        String address,

        @Schema(example = "06-1234-5678") @NotBlank(message = "電話番号は必須です")
        @Size(max = 20, message = "電話番号は20文字以内で入力してください")
        @Pattern(regexp = "^\\d{2,5}-\\d{1,4}-\\d{4}$", message = "電話番号は 03-1234-5678 の形式で入力してください")
        String phoneNumber,

        @Schema(example = "田中一郎") @NotBlank(message = "管理者名は必須です")
        @Size(max = 100, message = "管理者名は100文字以内で入力してください")
        @Pattern(regexp = "^[^<>]*$", message = "管理者名に < または > は使用できません")
        String managerName,

        @Schema(example = "0", description = "0: 稼働中、1: 稼働停止")
        @NotNull(message = "稼働状況は必須です")
        @Min(value = 0, message = "稼働状況は 0 または 1 を指定してください")
        @Max(value = 1, message = "稼働状況は 0 または 1 を指定してください")
        Integer operationalStatus,

        @Schema(example = "800", description = "m3。任意")
        @Min(value = 0, message = "最大保管容量は 0 以上で入力してください")
        @Max(value = 999_999_999, message = "最大保管容量が大きすぎます")
        Integer maxStorageCapacity,

        @Schema(example = "0", description = "m3。任意")
        @Min(value = 0, message = "現在保管容量は 0 以上で入力してください")
        @Max(value = 999_999_999, message = "現在保管容量が大きすぎます")
        Integer currentStorageCapacity,

        @Schema(example = "") @Size(max = 255, message = "備考は255文字以内で入力してください")
        @Pattern(regexp = "^[^<>]*$", message = "備考に < または > は使用できません")
        String notes) {
}
