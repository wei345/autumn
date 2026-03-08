package io.liuwei.autumn.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.util.Collection;
import java.util.Map;

/**
 * Enhanced Jackson Wrapper for Spring Boot 4.1 (Jakarta EE 11).
 * Optimized for JDK 24 and Data Processing.
 */
public class JsonMapper {

    // Using the modern fluent builder
    public static final JsonMapper INSTANCE = new JsonMapper(Include.ALWAYS);
    private static final Logger logger = LoggerFactory.getLogger(JsonMapper.class);
    private final ObjectMapper mapper;

    public JsonMapper() {
        this(Include.ALWAYS);
    }

    public JsonMapper(Include include) {
        this.mapper = tools.jackson.databind.json.JsonMapper.builder()
                .changeDefaultPropertyInclusion(value -> value.withValueInclusion(include))
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // Prevent failing on empty beans
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .build();
    }

    public static JsonMapper nonNullMapper() {
        return new JsonMapper(Include.NON_NULL);
    }

    public static JsonMapper nonEmptyMapper() {
        return new JsonMapper(Include.NON_EMPTY);
    }

    public String toJson(Object object) {
        if (object == null) return "null";
        try {
            return mapper.writeValueAsString(object);
        } catch (JacksonException e) {
            logger.warn("Write to JSON string error: {}", object, e);
            return null;
        }
    }

    public <T> T fromJson(@Nullable String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString)) return null;

        try {
            return mapper.readValue(jsonString, clazz);
        } catch (JacksonException e) {
            logger.warn("Parse JSON string error: {}", jsonString, e);
            return null;
        }
    }

    /**
     * Optimized for complex data processing collections.
     */
    @SuppressWarnings("unchecked")
    public <T> T fromJson(@Nullable String jsonString, JavaType javaType) {
        if (StringUtils.isEmpty(jsonString)) return null;

        try {
            return (T) mapper.readValue(jsonString, javaType);
        } catch (JacksonException e) {
            logger.warn("Parse JSON complex type error: {}", jsonString, e);
            return null;
        }
    }

    public JavaType buildCollectionType(Class<? extends Collection> collectionClass, Class<?> elementClass) {
        return mapper.getTypeFactory().constructCollectionType(collectionClass, elementClass);
    }

    public JavaType buildMapType(Class<? extends Map> mapClass, Class<?> keyClass, Class<?> valueClass) {
        return mapper.getTypeFactory().constructMapType(mapClass, keyClass, valueClass);
    }

    public void update(String jsonString, Object object) {
        try {
            mapper.readerForUpdating(object).readValue(jsonString);
        } catch (JacksonException e) {
            logger.warn("Update JSON string error for object: {}", object, e);
        }
    }

    /**
     * JSONP is deprecated. If you truly need it for legacy support,
     * use a String template instead of the removed JSONPObject.
     */
    public String toJsonP(String functionName, Object object) {
        return functionName + "(" + toJson(object) + ");";
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}