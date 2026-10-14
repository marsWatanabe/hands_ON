package com.aojt.mgmt.dto.center;

import com.aojt.mgmt.model.Center;
import java.time.LocalDateTime;

/**
 * 在庫センターのレスポンス。削除フラグは含めない。
 *
 * @param centerId 在庫センターID
 * @param centerName 在庫センター名
 * @param postCode 郵便番号
 * @param address 住所
 * @param phoneNumber 電話番号
 * @param managerName 管理者名
 * @param operationalStatus 稼働状況（0: 稼働中、1: 稼働停止）
 * @param maxStorageCapacity 最大保管容量（m3）
 * @param currentStorageCapacity 現在保管容量（m3）
 * @param notes 備考
 * @param createDate 作成日時
 * @param updateDate 更新日時
 */
public record CenterResponse(
        Integer centerId,
        String centerName,
        String postCode,
        String address,
        String phoneNumber,
        String managerName,
        Integer operationalStatus,
        Integer maxStorageCapacity,
        Integer currentStorageCapacity,
        String notes,
        LocalDateTime createDate,
        LocalDateTime updateDate) {

    /**
     * エンティティからレスポンスを生成する。
     *
     * @param c 在庫センター
     * @return レスポンス
     */
    public static CenterResponse from(Center c) {
        return new CenterResponse(
                c.getCenterId(),
                c.getCenterName(),
                c.getPostCode(),
                c.getAddress(),
                c.getPhoneNumber(),
                c.getManagerName(),
                c.getOperationalStatus(),
                c.getMaxStorageCapacity(),
                c.getCurrentStorageCapacity(),
                c.getNotes(),
                c.getCreateDate(),
                c.getUpdateDate());
    }
}
