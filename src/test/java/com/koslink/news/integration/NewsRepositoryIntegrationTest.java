package com.koslink.news.integration;

import com.koslink.news.dto.NewsItem;
import com.koslink.news.entity.News;
import com.koslink.news.entity.NewsStatus;
import com.koslink.news.repository.NewsRepository;
import com.koslink.news.stub.CrawledNewsStub;
import com.koslink.news.stub.NewsItemStub;
import com.koslink.news.util.DateParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("뉴스 저장 통합 테스트")
@SpringBootTest
@Transactional
class NewsRepositoryIntegrationTest {

    @Autowired
    private NewsRepository newsRepository;

    @Test
    @DisplayName("크롤링된 기사를 DB에 저장한다")
    void should_save_crawled_article_to_db() {
        // given
        NewsItem item = NewsItemStub.NAVER_NEWS_1;
        OffsetDateTime publishedAt = DateParser.parseNaverPubDate(item.pubDate()).orElseThrow();

        News news = News.of(
                item.title(),
                CrawledNewsStub.CRAWLED_NEWS_1.body(),
                item.link(),
                CrawledNewsStub.CRAWLED_NEWS_1.press(),
                publishedAt
        );

        // when
        News saved = newsRepository.save(news);

        // then
        assertThat(saved.getNewsId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo(item.title());
        assertThat(saved.getBody()).isEqualTo(CrawledNewsStub.CRAWLED_NEWS_1.body());
        assertThat(saved.getUrl()).isEqualTo(item.link());
        assertThat(saved.getPress()).isEqualTo("전자신문");
        assertThat(saved.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(saved.getStatus()).isEqualTo(NewsStatus.PENDING);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("여러 기사를 배치로 저장한다")
    void should_batch_save_multiple_articles() {
        // given
        long countBefore = newsRepository.count();

        NewsItem item1 = NewsItemStub.NAVER_NEWS_1;
        NewsItem item2 = NewsItemStub.NAVER_NEWS_2;

        News news1 = News.of(
                item1.title(),
                CrawledNewsStub.CRAWLED_NEWS_1.body(),
                item1.link(),
                CrawledNewsStub.CRAWLED_NEWS_1.press(),
                DateParser.parseNaverPubDate(item1.pubDate()).orElseThrow()
        );

        News news2 = News.of(
                item2.title(),
                CrawledNewsStub.CRAWLED_NEWS_2.body(),
                item2.link(),
                CrawledNewsStub.CRAWLED_NEWS_2.press(),
                DateParser.parseNaverPubDate(item2.pubDate()).orElseThrow()
        );

        // when
        List<News> saved = newsRepository.saveAll(List.of(news1, news2));
        newsRepository.flush();

        // then
        assertThat(saved).hasSize(2);
        assertThat(newsRepository.count()).isEqualTo(countBefore + 2);

        // 저장된 URL 검증
        Set<String> savedUrls = Set.of(
                saved.get(0).getUrl(),
                saved.get(1).getUrl()
        );
        assertThat(savedUrls).containsExactlyInAnyOrder(item1.link(), item2.link());
    }

    @Test
    @DisplayName("DB에 이미 존재하는 URL을 조회한다")
    void should_find_existing_urls() {
        // given
        NewsItem item1 = NewsItemStub.NAVER_NEWS_1;
        NewsItem item2 = NewsItemStub.NAVER_NEWS_2;

        News news1 = News.of(
                item1.title(),
                CrawledNewsStub.CRAWLED_NEWS_1.body(),
                item1.link(),
                CrawledNewsStub.CRAWLED_NEWS_1.press(),
                DateParser.parseNaverPubDate(item1.pubDate()).orElseThrow()
        );

        newsRepository.save(news1);
        newsRepository.flush();

        // when
        List<String> urlsToCheck = List.of(item1.link(), item2.link());
        Set<String> existingUrls = newsRepository.findExistingUrls(urlsToCheck);

        // then
        assertThat(existingUrls).hasSize(1);
        assertThat(existingUrls).contains(item1.link());
        assertThat(existingUrls).doesNotContain(item2.link());
    }

    @Test
    @DisplayName("언론사명이 null이어도 저장된다")
    void should_save_article_without_press() {
        // given
        NewsItem item = NewsItemStub.NAVER_NEWS_1;

        News news = News.of(
                item.title(),
                CrawledNewsStub.CRAWLED_NEWS_WITHOUT_PRESS.body(),
                item.link(),
                CrawledNewsStub.CRAWLED_NEWS_WITHOUT_PRESS.press(), // null
                DateParser.parseNaverPubDate(item.pubDate()).orElseThrow()
        );

        // when
        News saved = newsRepository.save(news);

        // then
        assertThat(saved.getNewsId()).isNotNull();
        assertThat(saved.getPress()).isNull();
    }

    @Test
    @DisplayName("중복 URL 체크 시 빈 리스트를 전달하면 빈 Set을 반환한다")
    void should_return_empty_set_when_checking_empty_url_list() {
        // given
        List<String> emptyUrls = List.of();

        // when
        Set<String> existingUrls = newsRepository.findExistingUrls(emptyUrls);

        // then
        assertThat(existingUrls).isEmpty();
    }
}
