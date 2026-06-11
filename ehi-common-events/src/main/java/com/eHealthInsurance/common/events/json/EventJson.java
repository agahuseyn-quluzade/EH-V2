package com.eHealthInsurance.common.events.json;

import com.eHealthInsurance.common.events.DomainEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class EventJson {
    private static final ObjectMapper BASE_MAPPER = createObjectMapper();

    private EventJson() {
    }

    public static ObjectMapper objectMapper() {
        return BASE_MAPPER.copy();
    }

    public static String write(DomainEvent event) throws JsonProcessingException {
        return BASE_MAPPER.writeValueAsString(event);
    }

    public static <T extends DomainEvent> T read(String json, Class<T> eventType) throws JsonProcessingException {
        return BASE_MAPPER.readValue(json, eventType);
    }

    private static ObjectMapper createObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .build();
    }
}
