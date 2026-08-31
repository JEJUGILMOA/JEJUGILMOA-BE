package com.example.jejugilmoa.global.external.tourapi.dto;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;

/**
 * KorService2 API가 데이터 없을 때 "items":"" (빈 문자열)을 반환하는 경우를 null로 처리.
 * Jackson 3.x는 빈 문자열 → POJO 변환을 거부하므로 이 deserializer로 우회.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class NullableItemsDeserializer extends StdDeserializer<TourApiResponse.Items> {

    private JavaType targetType;

    NullableItemsDeserializer() {
        super(TourApiResponse.Items.class);
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        NullableItemsDeserializer d = new NullableItemsDeserializer();
        d.targetType = (property != null) ? property.getType() : ctxt.getContextualType();
        return d;
    }

    @Override
    public TourApiResponse.Items deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            // KorService2 API가 결과 없을 때 "items":"" 반환 — 빈 문자열만 null로 처리
            if (p.getText().isEmpty()) {
                return null;
            }
            return ctxt.reportInputMismatch(TourApiResponse.Items.class,
                    "items 필드에 예상치 못한 문자열 값: \"%s\"", p.getText());
        }
        if (targetType != null) {
            return (TourApiResponse.Items) ctxt.readValue(p, targetType);
        }
        return (TourApiResponse.Items) ctxt.readValue(p, TourApiResponse.Items.class);
    }
}
