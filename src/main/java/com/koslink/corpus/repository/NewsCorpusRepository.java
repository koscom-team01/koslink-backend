package com.koslink.corpus.repository;

import com.koslink.corpus.dto.NewsCorpusCacheDto;
import com.koslink.corpus.entity.NewsCorpus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * NewsCorpus Repository
 * RAG 학습용 뉴스 코퍼스 데이터 접근
 */
@Repository
public interface NewsCorpusRepository extends JpaRepository<NewsCorpus, Long> {

    /**
     * URL 존재 여부 확인 (중복 체크용)
     *
     * @param url 뉴스 URL
     * @return 존재하면 true
     */
    boolean existsByUrl(String url);

    /**
     * 캐시 워밍업용: URL과 제목만 조회 (메모리 효율성)
     * body 필드는 제외하여 메모리 사용량 최소화
     *
     * @return URL과 제목 목록
     */
    @Query("SELECT new com.koslink.corpus.dto.NewsCorpusCacheDto(nc.url, nc.title) " +
           "FROM NewsCorpus nc ORDER BY nc.createdAt DESC")
    List<NewsCorpusCacheDto> findAllForCacheWarmup();

    /**
     * 최신 뉴스 조회 (첫 페이지)
     *
     * @param pageable 페이지 정보
     * @return 뉴스 목록
     */
    List<NewsCorpus> findAllByOrderByPublishedAtDesc(Pageable pageable);

    /**
     * 커서 이후 뉴스 조회 (다음 페이지)
     *
     * @param cursorId 커서 ID
     * @param pageable 페이지 정보
     * @return 뉴스 목록
     */
    List<NewsCorpus> findByNewsCorpusIdLessThanOrderByPublishedAtDesc(Long cursorId, Pageable pageable);
}
