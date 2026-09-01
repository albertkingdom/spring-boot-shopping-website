package com.albertkingdom.shoppingwebsite.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generic wrapper for a paged response so pagination shape is defined in
 * one place and future paged endpoints can reuse it.
 */
public class PageResponse<T> {

    private final List<T> content;
    private final int totalPages;
    private final long totalElements;

    public PageResponse(List<T> content, int totalPages, long totalElements) {
        this.content = content;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    public static <E, R> PageResponse<R> of(Page<E> page, Function<E, R> mapper) {
        List<R> mapped = page.getContent().stream().map(mapper).collect(Collectors.toList());
        return new PageResponse<>(mapped, page.getTotalPages(), page.getTotalElements());
    }

    public List<T> getContent() {
        return content;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }
}
