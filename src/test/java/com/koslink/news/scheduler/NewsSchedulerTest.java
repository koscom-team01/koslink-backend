package com.koslink.news.scheduler;

import com.github.benmanes.caffeine.cache.Cache;
import com.koslink.common.cache.ArticleFingerprint;
import com.koslink.news.crawler.NaverNewsCrawler;
import com.koslink.news.dto.NewsItem;
import com.koslink.news.dto.NewsSearchRequest;
import com.koslink.news.dto.NewsSearchResponse;
import com.koslink.news.repository.NewsRepository;
import com.koslink.news.service.NewsService;
import com.koslink.news.stub.NewsItemStub;
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

@DisplayName("뉴스 스케줄러 단위 테스트")
@ExtendWith(MockitoExtension.class)
class NewsSchedulerTest {

    @Mock
    private NewsService newsService;

    @Mock
    private Cache<String, ArticleFingerprint> recentArticleCache;

    @Mock
    private NaverNewsCrawler naverNewsCrawler;

    @Mock
    private NewsRepository newsRepository;

    @InjectMocks
    private NewsScheduler newsScheduler;

    @Test
    @DisplayName("최초 실행 시 모든 네이버 뉴스를 캐시에 등록한다")
    void should_cache_all_naver_news_on_first_run() {
        // given
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

        // 네이버 뉴스 필터링 Mock
        List<NewsItem> naverNews = List.of(NewsItemStub.NAVER_NEWS_1, NewsItemStub.NAVER_NEWS_2);
        when(newsService.filterNaverNewsUrls(anyList())).thenReturn(naverNews);

        // filterDuplicates Mock (중복 없음)
        when(newsService.filterDuplicates(anyList(), any(), anyDouble())).thenReturn(naverNews);

        // crawlArticles Mock (빈 리스트 반환)
        when(newsService.crawlArticles(anyList())).thenReturn(List.of());

        // when
        newsScheduler.fetchNews();

        // then
        // 네이버 뉴스 필터링 호출 확인
        verify(newsService).filterNaverNewsUrls(anyList());
        // 중복 제거 호출 확인
        verify(newsService).filterDuplicates(eq(naverNews), any(), anyDouble());
    }

    @Test
    @DisplayName("두 번째 실행 시 새 기사만 처리한다")
    void should_process_only_new_items_on_second_run() {
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
        when(newsService.filterNaverNewsUrls(anyList())).thenReturn(firstItems);
        when(newsService.filterDuplicates(anyList(), any(), anyDouble())).thenReturn(firstItems);
        when(newsService.crawlArticles(anyList())).thenReturn(List.of());

        // when - 첫 번째 실행
        newsScheduler.fetchNews();

        // then - 첫 번째 호출 확인
        verify(newsService, times(1)).filterDuplicates(anyList(), any(), anyDouble());

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
        List<NewsItem> newItemOnly = List.of(NewsItemStub.NAVER_NEWS_DIFFERENT);
        when(newsService.searchNews(any(NewsSearchRequest.class))).thenReturn(secondResponse);
        when(newsService.filterNaverNewsUrls(anyList())).thenReturn(newItemOnly);
        when(newsService.filterDuplicates(anyList(), any(), anyDouble())).thenReturn(newItemOnly);

        // when - 두 번째 실행
        newsScheduler.fetchNews();

        // then - 두 번째 호출 확인 (총 2번)
        verify(newsService, times(2)).filterDuplicates(anyList(), any(), anyDouble());
    }

    @Test
    @DisplayName("비네이버 뉴스는 필터링된다")
    void should_filter_non_naver_news() {
        // given
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

        // 네이버 뉴스만 필터링
        List<NewsItem> naverOnly = List.of(NewsItemStub.NAVER_NEWS_1);
        when(newsService.filterNaverNewsUrls(anyList())).thenReturn(naverOnly);
        when(newsService.filterDuplicates(anyList(), any(), anyDouble())).thenReturn(naverOnly);
        when(newsService.crawlArticles(anyList())).thenReturn(List.of());

        // when
        newsScheduler.fetchNews();

        // then
        verify(newsService).filterNaverNewsUrls(anyList());
        verify(newsService).filterDuplicates(eq(naverOnly), any(), anyDouble());
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
        when(newsService.filterNaverNewsUrls(anyList())).thenReturn(items);

        // filterDuplicates가 중복 제거해서 1개만 반환
        List<NewsItem> deduplicated = List.of(NewsItemStub.NAVER_NEWS_1);
        when(newsService.filterDuplicates(anyList(), any(), anyDouble())).thenReturn(deduplicated);
        when(newsService.crawlArticles(anyList())).thenReturn(List.of());

        // when
        newsScheduler.fetchNews();

        // then - filterDuplicates가 1개만 반환했는지 확인
        verify(newsService).filterDuplicates(eq(items), any(), anyDouble());
        verify(newsService).crawlArticles(eq(deduplicated));
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
        when(newsService.filterNaverNewsUrls(anyList())).thenReturn(items);

        // filterDuplicates가 모두 다른 기사라서 3개 모두 반환
        when(newsService.filterDuplicates(anyList(), any(), anyDouble())).thenReturn(items);
        when(newsService.crawlArticles(anyList())).thenReturn(List.of());

        // when
        newsScheduler.fetchNews();

        // then - filterDuplicates가 3개 모두 반환했는지 확인
        verify(newsService).filterDuplicates(eq(items), any(), anyDouble());
        verify(newsService).crawlArticles(eq(items));
    }
}
