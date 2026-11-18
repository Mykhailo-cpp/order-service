package com.example.order_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Response for paginated data")
public record PageResponse<T>(
        @Schema(description = "List of items on the current page")
        List<T> content,

        @Schema(description = "Zero-based page index", example = "0")
        int page,

        @Schema(description = "requested page size", example = "10")
        int size,

        @Schema(description = "Total number of items across all pages", example = "153")
        long totalElements,

        @Schema(description = "Total number of pages", example = "8")
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> pageData){
        return new PageResponse<>(
                pageData.getContent(),
                pageData.getNumber(),
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }
}
