package org.example.util;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

/**
 * Basic json converted. Allows one to convert object to json or json to object.
 */
public final class JsonUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtils() {
        // Nothing to do.
    }

    /**
     * Serialize a JAVA object to a JSON string.
     *
     * @param value The object to serialize.
     * @return A JSON string.
     */
    public static String toJson(final Object value) {
        try {
            return getDefaultObjectMapper().writeValueAsString(value);
        } catch (final JsonProcessingException exn) {
            throw new IllegalStateException(exn);
        }
    }

    /**
     * Deserialize a string to a JAVA object.
     *
     * @param json A JSON string.
     * @param valueType The class type to deserialize to.
     * @param <T> Generic parameter type.
     * @return An object of type &lt;T&gt;.
     */
    public static <T> T fromJson(final String json, final Class<T> valueType) {
        try {
            return getDefaultObjectMapper().readValue(json, valueType);
        } catch (final IOException exn) {
            throw new IllegalStateException(exn);
        }
    }

    /**
     * Builds a common jackon object mapper.
     * While doing so, it adds
     *  - A mapper which can evaluate enum's in case insensitive fashion
     *  - Supports snake case naming convention
     *
     * @return An instance of {@link ObjectMapper}
     */
    public static ObjectMapper getDefaultObjectMapper() {
        return new ObjectMapper()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    /**
     * Does exactly same as {@link JsonUtils#getDefaultObjectMapper()}, but overrides with camel case property naming case.
     *
     * @return An instance of {@link ObjectMapper}.
     */
    public static ObjectMapper getCamelCaseObjectMapper() {
        return getDefaultObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
    }
}
