package com.example.order_service.util;

import com.example.order_service.exception.ValidationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PageRequestFactory {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    public static final String DEFAULT_SORT = "createdAt,desc";

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("createdAt", "customerId", "product", "quantity", "status");

    private PageRequestFactory() {
    }

    public static Pageable of(int page, int size, String sort) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (page < 0) {
            errors.put("page", "page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            errors.put("size", "size must be between 1 and " + MAX_SIZE);
        }
        Sort parsedSort = parseSort(sort, errors);

        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid pagination parameters", errors);
        }
        return PageRequest.of(page, size, parsedSort);
    }

    private static Sort parseSort(String sort, Map<String, String> errors) {
        String value = (sort == null || sort.isBlank()) ? DEFAULT_SORT : sort;
        String[] parts = value.split(",", -1);

        String property = parts[0].trim();
        if (parts.length > 2 || !SORTABLE_FIELDS.contains(property)) {
            errors.put("sort", "sort must be '<field>[,asc|desc]' where field is one of "
                    + SORTABLE_FIELDS.stream().sorted().toList());
            return Sort.unsorted();
        }

        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length == 2) {
            String dir = parts[1].trim().toLowerCase(Locale.ROOT);
            if (dir.equals("desc")) {
                direction = Sort.Direction.DESC;
            } else if (!dir.equals("asc")) {
                errors.put("sort", "sort direction must be 'asc' or 'desc'");
                return Sort.unsorted();
            }
        }

        return Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }
}