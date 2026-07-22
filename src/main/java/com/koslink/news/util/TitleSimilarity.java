package com.koslink.news.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 제목 유사도 계산 유틸리티
 * Jaccard 유사도를 사용하여 같은 사건 여부 판별
 */
public class TitleSimilarity {

    /**
     * 제목을 정규화하고 토큰화
     * - HTML 태그 제거 (<b> 등)
     * - 특수문자 제거
     * - 공백 정규화
     *
     * @param title 원본 제목
     * @return 토큰 집합
     */
    public static Set<String> tokenize(String title) {
        String normalized = title
                .replaceAll("<[^>]*>", "")                  // <b> 태그 제거
                .replaceAll("[\\[\\]()\"']", "")            // 특수문자 제거
                .replaceAll("\\s+", " ")                    // 공백 정규화
                .trim();

        return new HashSet<>(Arrays.asList(normalized.split(" ")));
    }

    /**
     * Jaccard 유사도 계산
     * 두 집합의 교집합 크기 / 합집합 크기
     *
     * @param a 첫 번째 토큰 집합
     * @param b 두 번째 토큰 집합
     * @return 유사도 (0.0 ~ 1.0)
     */
    public static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0.0;
        }

        // 교집합
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);

        // 합집합
        Set<String> union = new HashSet<>(a);
        union.addAll(b);

        return (double) intersection.size() / union.size();
    }
}
