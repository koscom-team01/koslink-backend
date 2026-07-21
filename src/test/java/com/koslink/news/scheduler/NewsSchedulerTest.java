package com.koslink.news.scheduler;

import com.github.benmanes.caffeine.cache.Cache;
import com.koslink.news.cache.ArticleFingerprint;
import com.koslink.news.dto.NewsItem;
import com.koslink.news.dto.NewsSearchRequest;
import com.koslink.news.dto.NewsSearchResponse;
import com.koslink.news.service.NewsService;
import com.koslink.news.stub.NewsItemStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("뉴스 스케줄러 통합 테스트")
@ExtendWith(MockitoExtension.class)
class NewsSchedulerTest {

    @Mock
    private NewsService newsService;

    @Mock
    private Cache<String, ArticleFingerprint> recentArticleCache;

    @InjectMocks
    private NewsScheduler newsScheduler;

    @Test
    @DisplayName("최초 실행 시 모든 네이버 뉴스를 캐시에 등록한다")
    void should_cache_all_naver_news_on_first_run() {
        // given
        when(recentArticleCache.asMap()).thenReturn(new java.util.concurrent.ConcurrentHashMap<>());

        List<NewsItem> items = List.of(
                NewsItemStub.NAVER_NEWS_1,
                NewsItemStub.NAVER_NEWS_2,
                NewsItemStub.NON_NAVER_NEWS
        );
        NewsSearchResponse response = new NewsSearchResponse(
                "Mon, 21 Jan 2024 10:00:00 +0900",
                3,
                1,
                100,
                items
        );
        when(newsService.searchNews(any(NewsSearchRequest.class))).thenReturn(response);

        // when
        newsScheduler.fetchNews();

        // then
        // 네이버 뉴스만 캐시에 등록되어야 함 (2개)
        verify(recentArticleCache, times(2)).put(anyString(), any(ArticleFingerprint.class));
        // 비네이버 뉴스는 필터링됨 (1개 제외)
        verify(recentArticleCache, never()).put(eq(NewsItemStub.NON_NAVER_NEWS.link()), any());
    }

    @Test
    @DisplayName("두 번째 실행 시 새 기사만 처리한다")
    void should_process_only_new_items_on_second_run() {
        // given
        when(recentArticleCache.asMap()).thenReturn(new java.util.concurrent.ConcurrentHashMap<>());

        // given - 첫 번째 실행
        List<NewsItem> firstItems = List.of(
                NewsItemStub.NAVER_NEWS_1,
                NewsItemStub.NAVER_NEWS_2
        );
        NewsSearchResponse firstResponse = new NewsSearchResponse(
                "Mon, 21 Jan 2024 10:00:00 +0900",
                2,
                1,
                100,
                firstItems
        );
        when(newsService.searchNews(any(NewsSearchRequest.class))).thenReturn(firstResponse);

        // when - 첫 번째 실행
        newsScheduler.fetchNews();

        // then - 2개 캐시 등록
        verify(recentArticleCache, times(2)).put(anyString(), any(ArticleFingerprint.class));

        // given - 두 번째 실행 (새 기사 1개 추가)
        List<NewsItem> secondItems = List.of(
                NewsItemStub.NAVER_NEWS_DIFFERENT, // 새 기사
                NewsItemStub.NAVER_NEWS_1           // 이전 기사 (커서)
        );
        NewsSearchResponse secondResponse = new NewsSearchResponse(
                "Mon, 21 Jan 2024 10:10:00 +0900",
                3,
                1,
                100,
                secondItems
        );
        when(newsService.searchNews(any(NewsSearchRequest.class))).thenReturn(secondResponse);

        // when - 두 번째 실행
        newsScheduler.fetchNews();

        // then - 새 기사 1개만 추가로 캐시 등록 (총 3번 호출)
        verify(recentArticleCache, times(3)).put(anyString(), any(ArticleFingerprint.class));
    }

    @Test
    @DisplayName("비네이버 뉴스는 필터링된다")
    void should_filter_non_naver_news() {
        // given
        when(recentArticleCache.asMap()).thenReturn(new java.util.concurrent.ConcurrentHashMap<>());

        List<NewsItem> items = List.of(
                NewsItemStub.NAVER_NEWS_1,
                NewsItemStub.NON_NAVER_NEWS
        );
        NewsSearchResponse response = new NewsSearchResponse(
                "Mon, 21 Jan 2024 10:00:00 +0900",
                2,
                1,
                100,
                items
        );
        when(newsService.searchNews(any(NewsSearchRequest.class))).thenReturn(response);

        // when
        newsScheduler.fetchNews();

        // then
        verify(recentArticleCache, times(1)).put(anyString(), any(ArticleFingerprint.class));
        verify(recentArticleCache, never()).put(eq(NewsItemStub.NON_NAVER_NEWS.link()), any());
    }

    @Test
    @DisplayName("API 응답이 비어있으면 캐시에 아무것도 등록하지 않는다")
    void should_not_cache_anything_when_api_response_is_empty() {
        // given
        NewsSearchResponse response = new NewsSearchResponse(
                "Mon, 21 Jan 2024 10:00:00 +0900",
                0,
                1,
                100,
                List.of()
        );
        when(newsService.searchNews(any(NewsSearchRequest.class))).thenReturn(response);

        // when
        newsScheduler.fetchNews();

        // then
        verify(recentArticleCache, never()).put(anyString(), any(ArticleFingerprint.class));
    }

    @Test
    @DisplayName("예외 발생 시 스케줄러가 중단되지 않는다")
    void should_not_stop_scheduler_on_exception() {
        // given
        when(newsService.searchNews(any(NewsSearchRequest.class)))
                .thenThrow(new RuntimeException("API Error"));

        // when & then - 예외가 발생해도 스케줄러 메서드는 정상 종료
        newsScheduler.fetchNews();

        // 캐시 등록이 시도되지 않음
        verify(recentArticleCache, never()).put(anyString(), any(ArticleFingerprint.class));
    }

    @Test
    @DisplayName("최초 실행 시 100개 안에 중복된 제목이 있으면 하나만 캐시에 등록한다")
    void should_deduplicate_within_first_100_items() {
        // given - 매우 유사한 제목의 기사 2개
        List<NewsItem> items = List.of(
                NewsItemStub.NAVER_NEWS_1,      // "삼성전자 3나노 수율 개선"
                NewsItemStub.VERY_SIMILAR       // "삼성전자 3나노 공정 수율 크게 개선"
        );
        NewsSearchResponse response = new NewsSearchResponse(
                "Mon, 21 Jan 2024 10:00:00 +0900",
                2,
                1,
                100,
                items
        );
        when(newsService.searchNews(any(NewsSearchRequest.class))).thenReturn(response);

        // 실제 캐시 동작 모킹
        java.util.concurrent.ConcurrentHashMap<String, ArticleFingerprint> mockCache =
                new java.util.concurrent.ConcurrentHashMap<>();
        when(recentArticleCache.asMap()).thenReturn(mockCache);
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            ArticleFingerprint value = invocation.getArgument(1);
            mockCache.put(key, value);
            return null;
        }).when(recentArticleCache).put(anyString(), any(ArticleFingerprint.class));

        // when
        newsScheduler.fetchNews();

        // then - 유사도가 높아서 1개만 캐시에 등록됨
        assertThat(mockCache).hasSize(1);
        assertThat(mockCache).containsKey(NewsItemStub.NAVER_NEWS_1.link());
    }

    @Test
    @DisplayName("완전히 다른 제목의 기사는 모두 캐시에 등록한다")
    void should_cache_all_completely_different_articles() {
        // given
        List<NewsItem> items = List.of(
                NewsItemStub.NAVER_NEWS_1,          // 삼성전자 3나노
                NewsItemStub.NAVER_NEWS_DIFFERENT,  // SK하이닉스 HBM
                NewsItemStub.COMPLETELY_DIFFERENT   // 현대차 전기차
        );
        NewsSearchResponse response = new NewsSearchResponse(
                "Mon, 21 Jan 2024 10:00:00 +0900",
                3,
                1,
                100,
                items
        );
        when(newsService.searchNews(any(NewsSearchRequest.class))).thenReturn(response);

        // 실제 캐시 동작 모킹
        java.util.concurrent.ConcurrentHashMap<String, ArticleFingerprint> mockCache =
                new java.util.concurrent.ConcurrentHashMap<>();
        when(recentArticleCache.asMap()).thenReturn(mockCache);
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            ArticleFingerprint value = invocation.getArgument(1);
            mockCache.put(key, value);
            return null;
        }).when(recentArticleCache).put(anyString(), any(ArticleFingerprint.class));

        // when
        newsScheduler.fetchNews();

        // then - 모두 다른 사건이므로 3개 모두 캐시에 등록됨
        assertThat(mockCache).hasSize(3);
        assertThat(mockCache)
                .containsKeys(
                        NewsItemStub.NAVER_NEWS_1.link(),
                        NewsItemStub.NAVER_NEWS_DIFFERENT.link(),
                        NewsItemStub.COMPLETELY_DIFFERENT.link()
                );
    }
}
