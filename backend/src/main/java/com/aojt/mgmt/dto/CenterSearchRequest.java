package com.aojt.mgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Query parameters of {@code GET /api/v1/centers}. Bound via {@code @ModelAttribute}. */
public class CenterSearchRequest {

    @Size(max = 20)
    private String centerName;

    @Size(max = 100)
    private String address;

    @Schema(description = "Operational status filter (0: operating, 1: stopped)")
    @Min(0)
    @Max(1)
    private Integer operationalStatus;

    @Schema(description = "1-based page number", defaultValue = "1")
    @Min(1)
    private Integer page = 1;

    @Schema(description = "Page size (1-100)", defaultValue = "20")
    @Min(1)
    @Max(100)
    private Integer size = 20;

    @Schema(defaultValue = "centerId", allowableValues = {"centerId", "centerName", "address",
            "operationalStatus", "maxStorageCapacity", "currentStorageCapacity", "createDate", "updateDate"})
    @Pattern(regexp = "centerId|centerName|address|operationalStatus|maxStorageCapacity"
            + "|currentStorageCapacity|createDate|updateDate",
            message = "unsupported sort field")
    private String sort = "centerId";

    @Schema(defaultValue = "desc", allowableValues = {"asc", "desc"})
    @Pattern(regexp = "(?i)asc|desc", message = "must be asc or desc")
    private String order = "desc";

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(Integer operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }
}
