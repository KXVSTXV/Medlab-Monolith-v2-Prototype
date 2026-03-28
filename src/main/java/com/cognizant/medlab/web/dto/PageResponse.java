package com.cognizant.medlab.web.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard paged response envelope returned by all list endpoints.
 *
 * Example:
 * {
 *   "content": [...],
 *   "pageNumber": 0,
 *   "pageSize": 20,
 *   "totalElements": 150,
 *   "totalPages": 8,
 *   "last": false
 * }
 */
public record PageResponse<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }
}
