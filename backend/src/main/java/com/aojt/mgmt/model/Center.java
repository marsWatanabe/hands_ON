package com.aojt.mgmt.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * 在庫センター（テーブル {@code center_info}）。
 *
 * <p>スキーマは Flyway（{@code db/migration}）が管理し、本クラスは起動時に構造を検証される。
 * 削除は論理削除（{@code delete_flag}）で行う。
 */
@Entity
@Table(name = "center_info")
public class Center {

    /** 削除フラグ: 未削除。 */
    public static final int NOT_DELETED = 0;

    /** 削除フラグ: 削除済み。 */
    public static final int DELETED = 1;

    /** 稼働状況: 稼働中。 */
    public static final int STATUS_OPERATING = 0;

    /** 稼働状況: 稼働停止。 */
    public static final int STATUS_STOPPED = 1;

    /** 在庫センターID（自動採番）。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "center_id")
    private Integer centerId;

    /** 在庫センター名（20文字、重複禁止）。 */
    @Column(name = "center_name", nullable = false, length = 20)
    private String centerName;

    /** 郵便番号。 */
    @Column(name = "post_code", nullable = false, length = 10)
    private String postCode;

    /** 住所。 */
    @Column(name = "address", nullable = false, length = 255)
    private String address;

    /** 電話番号。 */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /** 管理者名。 */
    @Column(name = "manager_name", nullable = false, length = 100)
    private String managerName;

    /** 稼働状況（0: 稼働中、1: 稼働停止）。 */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "operational_status", nullable = false)
    private Integer operationalStatus;

    /** 最大保管容量（m3）。未設定は null。 */
    @Column(name = "max_storage_capacity")
    private Integer maxStorageCapacity;

    /** 現在保管容量（m3）。未設定は null。 */
    @Column(name = "current_storage_capacity")
    private Integer currentStorageCapacity;

    /** 備考。 */
    @Column(name = "notes", length = 255)
    private String notes;

    /** 削除フラグ（0: 未削除、1: 削除済み）。 */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "delete_flag", nullable = false)
    private Integer deleteFlag = NOT_DELETED;

    /** 作成日時。 */
    @CreationTimestamp
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    /** 更新日時。 */
    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    /**
     * 在庫センターIDを返す。
     *
     * @return 在庫センターID
     */
    public Integer getCenterId() {
        return centerId;
    }

    /**
     * 在庫センター名を返す。
     *
     * @return 在庫センター名
     */
    public String getCenterName() {
        return centerName;
    }

    /**
     * 在庫センター名を設定する。
     *
     * @param centerName 在庫センター名
     */
    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    /**
     * 郵便番号を返す。
     *
     * @return 郵便番号
     */
    public String getPostCode() {
        return postCode;
    }

    /**
     * 郵便番号を設定する。
     *
     * @param postCode 郵便番号
     */
    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    /**
     * 住所を返す。
     *
     * @return 住所
     */
    public String getAddress() {
        return address;
    }

    /**
     * 住所を設定する。
     *
     * @param address 住所
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * 電話番号を返す。
     *
     * @return 電話番号
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * 電話番号を設定する。
     *
     * @param phoneNumber 電話番号
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * 管理者名を返す。
     *
     * @return 管理者名
     */
    public String getManagerName() {
        return managerName;
    }

    /**
     * 管理者名を設定する。
     *
     * @param managerName 管理者名
     */
    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    /**
     * 稼働状況を返す。
     *
     * @return 稼働状況（0: 稼働中、1: 稼働停止）
     */
    public Integer getOperationalStatus() {
        return operationalStatus;
    }

    /**
     * 稼働状況を設定する。
     *
     * @param operationalStatus 稼働状況（0: 稼働中、1: 稼働停止）
     */
    public void setOperationalStatus(Integer operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    /**
     * 最大保管容量を返す。
     *
     * @return 最大保管容量（m3）。未設定は null
     */
    public Integer getMaxStorageCapacity() {
        return maxStorageCapacity;
    }

    /**
     * 最大保管容量を設定する。
     *
     * @param maxStorageCapacity 最大保管容量（m3）
     */
    public void setMaxStorageCapacity(Integer maxStorageCapacity) {
        this.maxStorageCapacity = maxStorageCapacity;
    }

    /**
     * 現在保管容量を返す。
     *
     * @return 現在保管容量（m3）。未設定は null
     */
    public Integer getCurrentStorageCapacity() {
        return currentStorageCapacity;
    }

    /**
     * 現在保管容量を設定する。
     *
     * @param currentStorageCapacity 現在保管容量（m3）
     */
    public void setCurrentStorageCapacity(Integer currentStorageCapacity) {
        this.currentStorageCapacity = currentStorageCapacity;
    }

    /**
     * 備考を返す。
     *
     * @return 備考
     */
    public String getNotes() {
        return notes;
    }

    /**
     * 備考を設定する。
     *
     * @param notes 備考
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * 削除フラグを返す。
     *
     * @return 削除フラグ（0: 未削除、1: 削除済み）
     */
    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    /** 論理削除済みにする。 */
    public void markDeleted() {
        this.deleteFlag = DELETED;
    }

    /**
     * 作成日時を返す。
     *
     * @return 作成日時
     */
    public LocalDateTime getCreateDate() {
        return createDate;
    }

    /**
     * 更新日時を返す。
     *
     * @return 更新日時
     */
    public LocalDateTime getUpdateDate() {
        return updateDate;
    }
}
