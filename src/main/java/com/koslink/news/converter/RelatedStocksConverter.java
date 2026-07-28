package com.koslink.news.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koslink.news.dto.RelatedStockDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * related_stocks JSONB 컨버터
 * List<RelatedStockDto> <-> String(JSON)
 */
@Component
@Converter
@RequiredArgsConstructor
public class RelatedStocksConverter implements AttributeConverter<List<RelatedStockDto>, String> {

    private final ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(List<RelatedStockDto> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert related stocks to JSON", e);
        }
    }

    @Override
    public List<RelatedStockDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert JSON to related stocks", e);
        }
    }
}
