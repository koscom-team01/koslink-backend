package com.koslink.news.dto;

import com.koslink.exception.InvalidDisplayRangeException;
import com.koslink.exception.InvalidSortValueException;
import com.koslink.exception.InvalidStartRangeException;
import com.koslink.exception.QueryRequiredException;

public record NewsSearchRequest(
        String query,
        Integer display,
        Integer start,
        String sort
) {
    public NewsSearchRequest {
        if (query == null || query.trim().isEmpty()) {
            throw new QueryRequiredException();
        }
        if (display != null && (display < 1 || display > 100)) {
            throw new InvalidDisplayRangeException();
        }
        if (start != null && (start < 1 || start > 1000)) {
            throw new InvalidStartRangeException();
        }
        if (sort != null && !sort.equals("sim") && !sort.equals("date")) {
            throw new InvalidSortValueException();
        }
    }

    public static NewsSearchRequest of(String query, Integer display, Integer start, String sort) {
        return new NewsSearchRequest(query, display, start, sort);
    }
}
