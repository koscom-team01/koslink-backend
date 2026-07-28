package com.koslink.corpus.dto;

/**
 * 캐시 워밍업용 DTO
 * 제목과 URL만 조회하여 메모리 효율성 확보
 */
public record NewsCorpusCacheDto(
        String url,
        String title
) {
}
