package com.koslink.news.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Getter
@Entity
@Table(name = "news")
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long newsId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "press", length = 100)
    private String press;

    @Column(name = "published_at", nullable = false)
    private OffsetDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NewsStatus status;

    @Column(name = "analyzed_at")
    private OffsetDateTime analyzedAt;

    @Column(name = "rag_embedded_at")
    private OffsetDateTime ragEmbeddedAt;

    @Column(name = "ontology_learned_at")
    private OffsetDateTime ontologyLearnedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected News() {
    }

    private News(String title, String body, String url, String press, OffsetDateTime publishedAt) {
        this.title = title;
        this.body = body;
        this.url = url;
        this.press = press;
        this.publishedAt = publishedAt;
        this.status = NewsStatus.PENDING;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public static News of(String title, String body, String url, String press, OffsetDateTime publishedAt) {
        return new News(title, body, url, press, publishedAt);
    }
}
