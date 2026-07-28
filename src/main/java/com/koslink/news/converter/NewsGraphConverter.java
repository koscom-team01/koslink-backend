package com.koslink.news.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koslink.news.dto.NewsGraphDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * graph JSONB 컨버터
 * NewsGraphDto <-> String(JSON)
 */
@Component
@Converter
@RequiredArgsConstructor
public class NewsGraphConverter implements AttributeConverter<NewsGraphDto, String> {

    private final ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(NewsGraphDto attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert news graph to JSON", e);
        }
    }

    @Override
    public NewsGraphDto convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, NewsGraphDto.class);
        } catch (JsonProcessingException e) {
            System.err.println("NewsGraphConverter error - dbData: " + dbData);
            e.printStackTrace();
            throw new IllegalArgumentException("Failed to convert JSON to news graph", e);
        }
    }
}
