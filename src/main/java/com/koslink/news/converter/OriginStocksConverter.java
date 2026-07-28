package com.koslink.news.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koslink.news.dto.OriginStockDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * origin_stocks JSONB 컨버터
 * List<OriginStockDto> <-> String(JSON)
 */
@Component
@Converter
@RequiredArgsConstructor
public class OriginStocksConverter implements AttributeConverter<List<OriginStockDto>, String> {

    private final ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(List<OriginStockDto> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert origin stocks to JSON", e);
        }
    }

    @Override
    public List<OriginStockDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert JSON to origin stocks", e);
        }
    }
}
