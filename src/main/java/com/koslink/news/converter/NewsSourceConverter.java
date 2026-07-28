package com.koslink.news.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koslink.news.dto.NewsSourceDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * source JSONB 컨버터
 * NewsSourceDto <-> String(JSON)
 */
@Component
@Converter
@RequiredArgsConstructor
public class NewsSourceConverter implements AttributeConverter<NewsSourceDto, String> {

    private final ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(NewsSourceDto attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert news source to JSON", e);
        }
    }

    @Override
    public NewsSourceDto convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, NewsSourceDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert JSON to news source", e);
        }
    }
}
