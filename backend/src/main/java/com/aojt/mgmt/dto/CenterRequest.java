package com.aojt.mgmt.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body of center create/update. Limits follow docs/table-definitions.md (center_info).
 */
public record CenterRequest(
        @Schema(example = "大阪センター") @NotBlank @Size(max = 20)
        @Pattern(regexp = "^[^<>]*$", message = "must not contain < or >")
        String centerName,

        @Schema(example = "530-0001") @NotBlank @Size(max = 10)
        @Pattern(regexp = "^\\d{3}-\\d{4}$", message = "must be formatted as 123-4567")
        String postCode,

        @Schema(example = "大阪府大阪市北区梅田1-1-1") @NotBlank @Size(max = 255)
        @Pattern(regexp = "^[^<>]*$", message = "must not contain < or >")
        String address,

        @Schema(example = "06-1234-5678") @NotBlank @Size(max = 20)
        @Pattern(regexp = "^\\d{2,5}-\\d{1,4}-\\d{4}$", message = "must be formatted as 03-1234-5678")
        String phoneNumber,

        @Schema(example = "田中一郎") @NotBlank @Size(max = 100)
        @Pattern(regexp = "^[^<>]*$", message = "must not contain < or >")
        String managerName,

        @Schema(example = "0", description = "0: operating, 1: stopped") @NotNull @Min(0) @Max(1)
        Integer operationalStatus,

        @Schema(example = "800", description = "m3. Optional") @Min(0) @Max(999_999_999)
        Integer maxStorageCapacity,

        @Schema(example = "0", description = "m3. Optional") @Min(0) @Max(999_999_999)
        Integer currentStorageCapacity,

        @Schema(example = "") @Size(max = 255)
        @Pattern(regexp = "^[^<>]*$", message = "must not contain < or >")
        String notes) {

    @JsonIgnore
    @AssertTrue(message = "currentStorageCapacity must not exceed maxStorageCapacity")
    public boolean isCapacityConsistent() {
        if (maxStorageCapacity == null || currentStorageCapacity == null) {
            return true;
        }
        return currentStorageCapacity <= maxStorageCapacity;
    }
}
