package com.koslink.corpus.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * 중복 판정된 기사 기록
 * 유사도 중복으로 제외된 기사들의 메타데이터 저장
 */
@Getter
@Entity
@Table(name = "news_duplicates")
public class NewsDuplicates {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_duplicates_id")
    private Long newsDuplicatesId;

    @Column(name = "link", length = 500, nullable = false)
    private String link;

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Column(name = "pub_date", nullable = false)
    private OffsetDateTime pubDate;

    @Column(name = "matched_query", length = 100, nullable = false)
    private String matchedQuery;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected NewsDuplicates() {
    }

    private NewsDuplicates(String link, String title, OffsetDateTime pubDate, String matchedQuery) {
        this.link = link;
        this.title = title;
        this.pubDate = pubDate;
        this.matchedQuery = matchedQuery;
        this.createdAt = OffsetDateTime.now();
    }

    public static NewsDuplicates of(String link, String title, OffsetDateTime pubDate, String matchedQuery) {
        return new NewsDuplicates(link, title, pubDate, matchedQuery);
    }
}
