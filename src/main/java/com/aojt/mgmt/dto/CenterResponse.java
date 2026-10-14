package com.aojt.mgmt.dto;

import com.aojt.mgmt.entity.InventoryCenter;
import java.time.LocalDateTime;

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

    public static CenterResponse from(InventoryCenter c) {
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
