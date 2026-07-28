package com.koslink.corpus.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * RAG 학습용 뉴스 코퍼스 엔티티
 * 백필(backfill)로 과거 뉴스 데이터를 수집하여 저장
 */
@Getter
@Entity
@Table(name = "news_corpus")
public class NewsCorpus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_corpus_id")
    private Long newsCorpusId;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "press", length = 100)
    private String press;

    @Column(name = "url", columnDefinition = "TEXT", unique = true, nullable = false)
    private String url;

    @Column(name = "published_at", nullable = false)
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "rag_embedded_at")
    private OffsetDateTime ragEmbeddedAt;

    @Column(name = "ontology_learned_at")
    private OffsetDateTime ontologyLearnedAt;

    protected NewsCorpus() {
    }

    private NewsCorpus(String title, String body, String url, String press, OffsetDateTime publishedAt) {
        this.title = title;
        this.body = body;
        this.url = url;
        this.press = press;
        this.publishedAt = publishedAt;
        this.createdAt = OffsetDateTime.now();
    }

    public static NewsCorpus of(String title, String body, String url, String press, OffsetDateTime publishedAt) {
        return new NewsCorpus(title, body, url, press, publishedAt);
    }
}
