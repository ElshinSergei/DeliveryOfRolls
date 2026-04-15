package org.example.deliveryofrolls.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Converter
public class PointsJsonConverter implements AttributeConverter<List<List<Double>>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<List<Double>> points) {
        if (points == null || points.isEmpty()) {
            log.debug("Converting null or empty points to JSON");
            return "[]";
        }
        try {
            String json = objectMapper.writeValueAsString(points);
            log.debug("Converting points to JSON: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            log.error("Error converting points to JSON", e);
            return "[]";
        }
    }

    @Override
    public List<List<Double>> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            log.debug("Converting null or empty JSON to points");
            return new ArrayList<>();
        }
        try {
            List<List<Double>> points = objectMapper.readValue(dbData, new TypeReference<List<List<Double>>>() {});
            log.debug("Converting JSON to points: {} points", points.size());
            return points;
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to points: {}", dbData, e);
            return new ArrayList<>();
        }
    }
}
