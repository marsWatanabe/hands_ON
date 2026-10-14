package com.aojt.mgmt.entity;

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
 * 在庫センター（テーブル center_info）。スキーマは Flyway（db/migration）が管理し、本クラスは起動時に構造を検証する。
 */
@Entity
@Table(name = "center_info")
public class InventoryCenter {

    public static final int NOT_DELETED = 0;
    public static final int DELETED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "center_id")
    private Integer centerId;

    @Column(name = "center_name", nullable = false, length = 20)
    private String centerName;

    @Column(name = "post_code", nullable = false, length = 10)
    private String postCode;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "manager_name", nullable = false, length = 100)
    private String managerName;

    /** 0: operating, 1: stopped. */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "operational_status", nullable = false)
    private Integer operationalStatus;

    @Column(name = "max_storage_capacity")
    private Integer maxStorageCapacity;

    @Column(name = "current_storage_capacity")
    private Integer currentStorageCapacity;

    @Column(name = "notes", length = 255)
    private String notes;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "delete_flag", nullable = false)
    private Integer deleteFlag = NOT_DELETED;

    @CreationTimestamp
    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    public Integer getCenterId() {
        return centerId;
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public Integer getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(Integer operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public Integer getMaxStorageCapacity() {
        return maxStorageCapacity;
    }

    public void setMaxStorageCapacity(Integer maxStorageCapacity) {
        this.maxStorageCapacity = maxStorageCapacity;
    }

    public Integer getCurrentStorageCapacity() {
        return currentStorageCapacity;
    }

    public void setCurrentStorageCapacity(Integer currentStorageCapacity) {
        this.currentStorageCapacity = currentStorageCapacity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void markDeleted() {
        this.deleteFlag = DELETED;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }
}
