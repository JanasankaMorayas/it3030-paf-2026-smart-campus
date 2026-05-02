package com.sliit.paf.smart_campus.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

public final class PageableUtils {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageableUtils() {
    }

    public static Pageable sanitize(Pageable pageable, Sort defaultSort, Set<String> allowedSortProperties) {
        int page = pageable == null ? DEFAULT_PAGE : Math.max(pageable.getPageNumber(), DEFAULT_PAGE);
        int size = pageable == null ? DEFAULT_SIZE : pageable.getPageSize();

        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        size = Math.min(size, MAX_SIZE);

        Sort sort = pageable == null ? defaultSort : buildAllowedSort(pageable.getSort(), defaultSort, allowedSortProperties);
        return PageRequest.of(page, size, sort);
    }

    private static Sort buildAllowedSort(Sort requestedSort, Sort defaultSort, Set<String> allowedSortProperties) {
        if (requestedSort == null || requestedSort.isUnsorted()) {
            return defaultSort;
        }

        List<Sort.Order> validOrders = requestedSort.stream()
                .filter(order -> allowedSortProperties.contains(order.getProperty()))
                .toList();

        return validOrders.isEmpty() ? defaultSort : Sort.by(validOrders);
    }
}
