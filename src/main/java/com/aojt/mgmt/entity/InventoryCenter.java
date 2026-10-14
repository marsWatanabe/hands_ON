package com.aojt.mgmt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "inventory_center",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_center_name", columnNames = "center_name"))
public class InventoryCenter {

    public static final int NOT_DELETED = 0;
    public static final int DELETED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "center_id")
    private Integer centerId;

    @Column(name = "center_name", nullable = false, length = 20)
    private String centerName;

    @Column(name = "post_code", length = 8)
    private String postCode;

    @Column(name = "address", length = 100)
    private String address;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "manager_name", length = 20)
    private String managerName;

    /** 0: operating, 1: stopped. */
    @Column(name = "operational_status", nullable = false)
    private Integer operationalStatus;

    @Column(name = "max_storage_capacity", nullable = false)
    private Integer maxStorageCapacity;

    @Column(name = "current_storage_capacity", nullable = false)
    private Integer currentStorageCapacity;

    @Column(name = "notes", length = 200)
    private String notes;

    @Column(name = "delete_flag", nullable = false)
    private Integer deleteFlag = NOT_DELETED;

    @CreationTimestamp
    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "update_date", nullable = false)
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
