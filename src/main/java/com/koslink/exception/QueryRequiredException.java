package com.koslink.exception;

public class QueryRequiredException extends BadRequestException {
    public QueryRequiredException() {
        super("query parameter is required", "검색어를 입력해주세요.");
    }
}
