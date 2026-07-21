package com.koslink.news.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("제목 유사도 계산 테스트")
class TitleSimilarityTest {

    @Test
    @DisplayName("HTML 태그를 제거하고 토큰화한다")
    void should_remove_html_tags_and_tokenize() {
        String titleWithTags = "<b>삼성전자</b> 3나노 수율 <b>개선</b>";

        Set<String> tokens = TitleSimilarity.tokenize(titleWithTags);

        assertThat(tokens).containsExactlyInAnyOrder("삼성전자", "3나노", "수율", "개선");
    }

    @Test
    @DisplayName("특수문자를 제거하고 토큰화한다")
    void should_remove_special_characters_and_tokenize() {
        String titleWithSpecialChars = "[속보] 삼성전자 \"3나노\" 수율 개선";

        Set<String> tokens = TitleSimilarity.tokenize(titleWithSpecialChars);

        assertThat(tokens).containsExactlyInAnyOrder("속보", "삼성전자", "3나노", "수율", "개선");
    }

    @Test
    @DisplayName("공백을 정규화하고 토큰화한다")
    void should_normalize_whitespace_and_tokenize() {
        String titleWithExtraSpaces = "삼성전자    3나노   수율  개선";

        Set<String> tokens = TitleSimilarity.tokenize(titleWithExtraSpaces);

        assertThat(tokens).containsExactlyInAnyOrder("삼성전자", "3나노", "수율", "개선");
    }

    @Test
    @DisplayName("완전히 같은 제목은 Jaccard 유사도가 1.0이다")
    void should_return_1_0_for_identical_titles() {
        Set<String> tokens1 = TitleSimilarity.tokenize("삼성전자 3나노 수율 개선");
        Set<String> tokens2 = TitleSimilarity.tokenize("삼성전자 3나노 수율 개선");

        double similarity = TitleSimilarity.jaccard(tokens1, tokens2);

        assertThat(similarity).isEqualTo(1.0);
    }

    @Test
    @DisplayName("매우 유사한 제목은 Jaccard 유사도가 높다 (0.8 이상)")
    void should_return_high_similarity_for_very_similar_titles() {
        Set<String> tokens1 = TitleSimilarity.tokenize("삼성전자 3나노 수율 개선");
        Set<String> tokens2 = TitleSimilarity.tokenize("삼성전자 3나노 수율 개선 성공");

        double similarity = TitleSimilarity.jaccard(tokens1, tokens2);

        assertThat(similarity).isGreaterThanOrEqualTo(0.8);
    }

    @Test
    @DisplayName("약간 유사한 제목은 Jaccard 유사도가 낮다 (0.1~0.2)")
    void should_return_low_similarity_for_slightly_similar_titles() {
        Set<String> tokens1 = TitleSimilarity.tokenize("삼성전자 3나노 수율 개선");
        Set<String> tokens2 = TitleSimilarity.tokenize("삼성전자 반도체 기술 혁신");

        double similarity = TitleSimilarity.jaccard(tokens1, tokens2);

        assertThat(similarity).isBetween(0.1, 0.2);
    }

    @Test
    @DisplayName("완전히 다른 제목은 Jaccard 유사도가 0에 가깝다")
    void should_return_near_zero_similarity_for_completely_different_titles() {
        Set<String> tokens1 = TitleSimilarity.tokenize("삼성전자 3나노 수율 개선");
        Set<String> tokens2 = TitleSimilarity.tokenize("현대차 전기차 판매 급증");

        double similarity = TitleSimilarity.jaccard(tokens1, tokens2);

        assertThat(similarity).isLessThan(0.1);
    }

    @Test
    @DisplayName("빈 제목끼리는 Jaccard 유사도가 0이다")
    void should_return_0_for_empty_token_sets() {
        Set<String> tokens1 = Set.of();
        Set<String> tokens2 = Set.of();

        double similarity = TitleSimilarity.jaccard(tokens1, tokens2);

        assertThat(similarity).isEqualTo(0.0);
    }

    @Test
    @DisplayName("한쪽이 빈 제목이면 Jaccard 유사도가 0이다")
    void should_return_0_when_one_token_set_is_empty() {
        Set<String> tokens1 = TitleSimilarity.tokenize("삼성전자 3나노");
        Set<String> tokens2 = Set.of();

        double similarity = TitleSimilarity.jaccard(tokens1, tokens2);

        assertThat(similarity).isEqualTo(0.0);
    }
}
