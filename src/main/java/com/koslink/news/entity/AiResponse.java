package com.koslink.news.entity;

import com.koslink.news.converter.*;
import com.koslink.news.dto.*;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

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

    @Convert(converter = NewsSummaryConverter.class)
    @Column(name = "news_summary", columnDefinition = "jsonb")
    private List<String> newsSummary;

    @Convert(converter = NewsSourceConverter.class)
    @Column(name = "source", columnDefinition = "jsonb")
    private NewsSourceDto source;

    @Convert(converter = OriginStocksConverter.class)
    @Column(name = "origin_stocks", columnDefinition = "jsonb")
    private List<OriginStockDto> originStocks;

    @Convert(converter = RelatedStocksConverter.class)
    @Column(name = "related_stocks", columnDefinition = "jsonb")
    private List<RelatedStockDto> relatedStocks;

    @Column(name = "final_summary", columnDefinition = "TEXT")
    private String finalSummary;

    @Convert(converter = NewsGraphConverter.class)
    @Column(name = "graph", columnDefinition = "jsonb")
    private NewsGraphDto graph;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "done";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AiResponse() {
    }
}
