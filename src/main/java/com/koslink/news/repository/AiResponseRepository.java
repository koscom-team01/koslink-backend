package com.koslink.news.repository;

import com.koslink.news.entity.AiResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI 분석 응답 Repository
 */
@Repository
public interface AiResponseRepository extends JpaRepository<AiResponse, Long> {

    /**
     * news_id로 AI 응답 조회
     *
     * @param newsId 뉴스 ID
     * @return AI 응답
     */
    Optional<AiResponse> findByNewsId(Long newsId);
}
