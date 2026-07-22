package com.koslink.news.crawler;

import com.koslink.news.dto.CrawledNews;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * 네이버 뉴스 본문 크롤러
 * Jaccard 유사도 판별을 통과한 신규 기사의 본문 + 언론사명 크롤링
 */
@Slf4j
@Component
public class NaverNewsCrawler {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    /**
     * 네이버 뉴스 기사 크롤링 (본문 + 언론사명)
     *
     * @param url 네이버 뉴스 URL (https://n.news.naver.com/...)
     * @return 크롤링된 뉴스 데이터 (크롤링 실패 시 Optional.empty())
     */
    public Optional<CrawledNews> crawl(String url) {
        try {
            Document doc = fetchDocument(url);

            String body = parseBody(doc, url);
            if (body == null) {
                return Optional.empty();
            }

            String press = parsePress(doc, url);

            log.debug("크롤링 성공: {} (본문: {} 글자, 언론사: {})", url, body.length(), press);

            return Optional.of(new CrawledNews(body, press));

        } catch (IOException e) {
            log.error("크롤링 실패: {}", url, e);
            return Optional.empty();
        }
    }

    /**
     * Jsoup으로 HTML 문서 가져오기
     *
     * @param url 네이버 뉴스 URL
     * @return HTML 문서
     * @throws IOException 네트워크 오류 시
     */
    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(5000)
                .get();
    }

    /**
     * 본문 파싱
     *
     * @param doc HTML 문서
     * @param url 로그용 URL
     * @return 본문 텍스트 (파싱 실패 시 null)
     */
    private String parseBody(Document doc, String url) {
        Element bodyElement = doc.selectFirst("#dic_area");
        if (bodyElement == null) {
            log.warn("본문 셀렉터(#dic_area)를 못 찾음. 페이지 구조 변경 의심: {}", url);
            return null;
        }

        // 이미지 캡션 테이블 제거 (본문 텍스트 오염 방지)
        bodyElement.select("table.nbd_table").remove();

        return bodyElement.text();
    }

    /**
     * 언론사명 파싱
     *
     * @param doc HTML 문서
     * @param url 로그용 URL
     * @return 언론사명 (파싱 실패 시 null)
     */
    private String parsePress(Document doc, String url) {
        Element pressElement = doc.selectFirst(".media_end_head_top_press");

        if (pressElement == null) {
            log.warn("언론사명 셀렉터(.media_end_head_top_press)를 못 찾음: {}", url);
            return null;
        }

        return pressElement.text();
    }
}
