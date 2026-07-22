package com.koslink.news.stub;

import com.koslink.news.dto.CrawledNews;

/**
 * CrawledNews 테스트용 Stub 데이터
 */
public class CrawledNewsStub {

    public static final CrawledNews CRAWLED_NEWS_1 = new CrawledNews(
            "삼성전자가 3나노 공정 기술을 적용한 차세대 반도체 생산에 성공했다. 업계는 이번 성과가 글로벌 반도체 시장에서 삼성의 경쟁력을 한층 강화할 것으로 전망하고 있다.",
            "전자신문"
    );

    public static final CrawledNews CRAWLED_NEWS_2 = new CrawledNews(
            "SK하이닉스가 차세대 HBM(고대역폭메모리) 개발에 성공하며 AI 반도체 시장 공략에 나섰다. 이번 HBM은 기존 제품 대비 성능이 2배 향상됐다.",
            "디지털타임스"
    );

    public static final CrawledNews CRAWLED_NEWS_WITHOUT_PRESS = new CrawledNews(
            "반도체 업계가 침체기를 극복하고 회복세를 보이고 있다. 전문가들은 AI 수요 증가가 주요 원인이라고 분석했다.",
            null  // 언론사명 파싱 실패 케이스
    );
}
