package com.koslink.news.stub;

import com.koslink.news.dto.NewsItem;

/**
 * NewsItem 테스트용 Stub 데이터
 */
public class NewsItemStub {

    // 네이버 뉴스 URL
    public static final NewsItem NAVER_NEWS_1 = new NewsItem(
            "삼성전자 3나노 수율 개선",
            "https://www.original-press.com/article/1",
            "https://n.news.naver.com/article/001/0014512345",
            "삼성전자가 3나노 공정 수율을 개선했다",
            "Mon, 21 Jan 2024 10:00:00 +0900"
    );

    public static final NewsItem NAVER_NEWS_2 = new NewsItem(
            "삼성전자 3나노 수율 개선 성공",
            "https://www.another-press.com/article/2",
            "https://news.naver.com/main/read.nhn?oid=002&aid=0014512346",
            "삼성전자가 3나노 공정 수율 개선에 성공했다",
            "Mon, 21 Jan 2024 10:05:00 +0900"
    );

    public static final NewsItem NAVER_NEWS_DIFFERENT = new NewsItem(
            "SK하이닉스 HBM 수주",
            "https://www.press3.com/article/3",
            "https://n.news.naver.com/article/003/0014512347",
            "SK하이닉스가 HBM을 수주했다",
            "Mon, 21 Jan 2024 10:10:00 +0900"
    );

    // 비네이버 뉴스 URL
    public static final NewsItem NON_NAVER_NEWS = new NewsItem(
            "반도체 시장 전망",
            "https://www.chosun.com/article/100",
            "https://www.chosun.com/article/100",
            "반도체 시장이 회복세를 보이고 있다",
            "Mon, 21 Jan 2024 10:15:00 +0900"
    );

    // 유사도 테스트용
    public static final NewsItem VERY_SIMILAR = new NewsItem(
            "삼성전자 3나노 공정 수율 크게 개선",
            "https://www.press4.com/article/4",
            "https://n.news.naver.com/article/004/0014512348",
            "삼성전자가 3나노 공정 수율을 크게 개선했다",
            "Mon, 21 Jan 2024 10:20:00 +0900"
    );

    public static final NewsItem SLIGHTLY_SIMILAR = new NewsItem(
            "삼성전자 반도체 기술 혁신",
            "https://www.press5.com/article/5",
            "https://n.news.naver.com/article/005/0014512349",
            "삼성전자가 반도체 기술 혁신을 이뤘다",
            "Mon, 21 Jan 2024 10:25:00 +0900"
    );

    public static final NewsItem COMPLETELY_DIFFERENT = new NewsItem(
            "현대차 전기차 판매 급증",
            "https://www.press6.com/article/6",
            "https://n.news.naver.com/article/006/0014512350",
            "현대차 전기차 판매가 급증하고 있다",
            "Mon, 21 Jan 2024 10:30:00 +0900"
    );
}
