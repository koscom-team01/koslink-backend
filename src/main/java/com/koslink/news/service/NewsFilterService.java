package com.koslink.news.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koslink.exception.NewsFilterException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

/**
 * LLM 기반 뉴스 필터링 서비스
 * 반도체 산업 관련 뉴스만 선별
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class NewsFilterService {

    private final ChatClient openAiChatClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are a semiconductor industry news classifier.
            You will receive a JSON array of news titles.
            Return ONLY the titles that are related to the semiconductor industry.

            Consider relevant:
            - Semiconductor companies (Samsung, TSMC, Intel, etc.)
            - Chip manufacturing, design, equipment
            - Memory, foundry, fabless businesses
            - Semiconductor supply chain
            - Related technologies (AI chips, automotive chips)

            Consider irrelevant:
            - General IT news unrelated to semiconductors
            - Entertainment, sports, politics
            - Other industries

            IMPORTANT: Respond with a raw JSON array ONLY. Do NOT wrap it in markdown code blocks or any other formatting.
            Correct: ["title1", "title3", "title5"]
            Wrong: ```json["title1"]``` or ```["title1"]```
            """;

    /**
     * 뉴스 제목 목록에서 반도체 산업 관련 제목만 필터링
     *
     * @param titles 뉴스 제목 목록
     * @return 반도체 산업 관련 제목만 포함된 목록
     * @throws NewsFilterException LLM 호출 또는 JSON 파싱 실패 시
     */
    public List<String> filterRelevantTitles(List<String> titles) {
        if (titles.isEmpty()) {
            return List.of();
        }

        log.info("Filtering {} news titles with LLM", titles.size());

        try {
            String titlesJson = objectMapper.writeValueAsString(titles);

            String response = openAiChatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(titlesJson)
                    .call()
                    .content();

            log.debug("LLM response: {}", response);

            if (response == null || response.isBlank()) {
                log.warn("LLM returned empty response, returning empty list");
                return List.of();
            }

            // 마크다운 코드 블록 제거
            String cleanedResponse = cleanMarkdownCodeBlock(response);

            List<String> relevantTitles = objectMapper.readValue(
                    cleanedResponse,
                    new TypeReference<>() {}
            );

            int filteredCount = titles.size() - relevantTitles.size();
            log.info("LLM filtering completed - relevant: {}, filtered: {}",
                    relevantTitles.size(), filteredCount);

            return relevantTitles;
        } catch (Exception e) {
            log.error("Failed to filter news titles", e);
            throw new NewsFilterException();
        }
    }

    /**
     * 마크다운 코드 블록 제거
     * LLM이 ```json ... ``` 또는 ``` ... ``` 형식으로 반환할 수 있음
     *
     * @param response LLM 응답
     * @return 정제된 JSON 문자열
     */
    private String cleanMarkdownCodeBlock(String response) {
        String cleaned = response.trim();

        // ```json ... ``` 형식 제거
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        // ``` ... ``` 형식 제거
        else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }
}
