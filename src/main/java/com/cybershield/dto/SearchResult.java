package com.cybershield.dto;

/** One global search hit: record name, category, identifier, status and a link to its detail page. */
public record SearchResult(String category, String identifier, String name, String status, String url, String icon) {
}
