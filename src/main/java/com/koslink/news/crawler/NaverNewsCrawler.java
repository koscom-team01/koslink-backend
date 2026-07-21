package com.koslink.news.crawler;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * 네이버 뉴스 본문 크롤러
 * Jaccard 유사도 판별을 통과한 신규 기사의 본문만 크롤링
 */
@Slf4j
@Component
public class NaverNewsCrawler {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    /**
     * 네이버 뉴스 기사 본문 크롤링
     *
     * @param url 네이버 뉴스 URL (https://n.news.naver.com/...)
     * @return 본문 텍스트 (크롤링 실패 시 Optional.empty())
     */
    public Optional<String> crawlBody(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(5000)
                    .get();

            Element bodyElement = doc.selectFirst("#dic_area");
            if (bodyElement == null) {
                log.warn("본문 셀렉터(#dic_area)를 못 찾음. 페이지 구조 변경 의심: {}", url);
                return Optional.empty();
            }

            // 이미지 캡션 테이블 제거 (본문 텍스트 오염 방지)
            bodyElement.select("table.nbd_table").remove();

            String body = bodyElement.text();
            log.debug("크롤링 성공: {} (본문 길이: {} 글자)", url, body.length());

            return Optional.of(body);

        } catch (IOException e) {
            log.error("크롤링 실패: {}", url, e);
            return Optional.empty();
        }
    }
}
