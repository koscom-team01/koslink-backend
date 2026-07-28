package com.koslink.news.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * AI 분석 응답 엔티티
 * 뉴스 요약, 핵심 종목, 파생 종목 정보 저장
 */
@Getter
@Entity
@Table(name = "ai_responses")
public class AiResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "news_id", nullable = false, unique = true)
    private Long newsId;

    @Column(name = "news_summary", columnDefinition = "jsonb")
    private String newsSummary;

    @Column(name = "source", columnDefinition = "jsonb")
    private String source;

    @Column(name = "origin_stocks", columnDefinition = "jsonb")
    private String originStocks;

    @Column(name = "related_stocks", columnDefinition = "jsonb")
    private String relatedStocks;

    @Column(name = "final_summary", columnDefinition = "TEXT")
    private String finalSummary;

    @Column(name = "graph", columnDefinition = "jsonb")
    private String graph;

    @Column(name = "evidence_debug", columnDefinition = "jsonb")
    private String evidenceDebug;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "done";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AiResponse() {
    }
}
