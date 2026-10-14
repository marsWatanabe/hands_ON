package com.aojt.mgmt.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CenterRequest(
        @NotBlank @Size(max = 20) @Pattern(regexp = NO_ANGLE_BRACKETS, message = "must not contain < or >")
        String centerName,

        @Pattern(regexp = "^(\\d{3}-\\d{4})?$", message = "must be formatted as 123-4567")
        String postCode,

        @Size(max = 100) @Pattern(regexp = NO_ANGLE_BRACKETS, message = "must not contain < or >")
        String address,

        @Pattern(regexp = "^(\\d{2,5}-\\d{1,4}-\\d{4})?$", message = "must be formatted as 03-1234-5678")
        String phoneNumber,

        @Size(max = 20) @Pattern(regexp = NO_ANGLE_BRACKETS, message = "must not contain < or >")
        String managerName,

        @NotNull @Min(0) @Max(1)
        Integer operationalStatus,

        @NotNull @Min(0) @Max(999_999_999)
        Integer maxStorageCapacity,

        @NotNull @Min(0) @Max(999_999_999)
        Integer currentStorageCapacity,

        @Size(max = 200) @Pattern(regexp = NO_ANGLE_BRACKETS, message = "must not contain < or >")
        String notes) {

    private static final String NO_ANGLE_BRACKETS = "^[^<>]*$";

    @AssertTrue(message = "currentStorageCapacity must not exceed maxStorageCapacity")
    public boolean isCapacityConsistent() {
        if (maxStorageCapacity == null || currentStorageCapacity == null) {
            return true;
        }
        return currentStorageCapacity <= maxStorageCapacity;
    }
}
