package com.koslink.news.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("네이버 뉴스 URL 필터링 테스트")
class NaverNewsUrlFilterTest {

    @Test
    @DisplayName("n.news.naver.com 도메인은 네이버 뉴스로 판단한다")
    void should_return_true_for_n_news_naver_com() {
        String url = "https://n.news.naver.com/article/001/0014512345";

        boolean result = NaverNewsUrlFilter.isNaverNewsUrl(url);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("news.naver.com 도메인은 네이버 뉴스로 판단한다")
    void should_return_true_for_news_naver_com() {
        String url = "https://news.naver.com/main/read.nhn?oid=001&aid=0014512345";

        boolean result = NaverNewsUrlFilter.isNaverNewsUrl(url);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("HTTP 프로토콜도 네이버 뉴스로 판단한다")
    void should_return_true_for_http_protocol() {
        String url = "http://news.naver.com/article/001/123";

        boolean result = NaverNewsUrlFilter.isNaverNewsUrl(url);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("비네이버 도메인은 네이버 뉴스가 아니라고 판단한다")
    void should_return_false_for_non_naver_domains() {
        String url = "https://www.chosun.com/economy/tech/2024/01/01/article";

        boolean result = NaverNewsUrlFilter.isNaverNewsUrl(url);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("null URL은 네이버 뉴스가 아니라고 판단한다")
    void should_return_false_for_null_url() {
        boolean result = NaverNewsUrlFilter.isNaverNewsUrl(null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 URL은 네이버 뉴스가 아니라고 판단한다")
    void should_return_false_for_empty_url() {
        boolean result = NaverNewsUrlFilter.isNaverNewsUrl("");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("공백 URL은 네이버 뉴스가 아니라고 판단한다")
    void should_return_false_for_blank_url() {
        boolean result = NaverNewsUrlFilter.isNaverNewsUrl("   ");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("잘못된 형식의 URL은 네이버 뉴스가 아니라고 판단한다")
    void should_return_false_for_invalid_url_format() {
        String invalidUrl = "not-a-valid-url";

        boolean result = NaverNewsUrlFilter.isNaverNewsUrl(invalidUrl);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("host가 없는 URL은 네이버 뉴스가 아니라고 판단한다")
    void should_return_false_for_url_without_host() {
        String urlWithoutHost = "file:///local/path/file.html";

        boolean result = NaverNewsUrlFilter.isNaverNewsUrl(urlWithoutHost);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("naver.com이지만 뉴스 도메인이 아니면 false를 반환한다")
    void should_return_false_for_non_news_naver_domains() {
        String url = "https://www.naver.com";

        boolean result = NaverNewsUrlFilter.isNaverNewsUrl(url);

        assertThat(result).isFalse();
    }
}
