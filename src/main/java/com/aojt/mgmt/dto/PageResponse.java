package com.aojt.mgmt.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> data, Meta meta) {

    public record Meta(long total, int page, int size, int totalPages) {
    }

    /** Converts a zero-based Spring {@link Page} into the one-based API format. */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                new Meta(page.getTotalElements(), page.getNumber() + 1, page.getSize(), page.getTotalPages()));
    }
}
