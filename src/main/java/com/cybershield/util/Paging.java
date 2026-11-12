package com.cybershield.util;

import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds a Pageable from request parameters. Only white-listed property names may be used for sorting,
 * so a user cannot sort by arbitrary entity paths.
 */
public final class Paging {

    public static final int DEFAULT_SIZE = 10;

    private Paging() {
    }

    public static Pageable of(int page, String sort, String direction, Set<String> allowedSorts, String defaultSort) {
        String property = (sort != null && allowedSorts.contains(sort)) ? sort : defaultSort;
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(page, 0), DEFAULT_SIZE, Sort.by(dir, property));
    }

    public static Pageable of(int page, Sort sort) {
        return PageRequest.of(Math.max(page, 0), DEFAULT_SIZE, sort);
    }
}
