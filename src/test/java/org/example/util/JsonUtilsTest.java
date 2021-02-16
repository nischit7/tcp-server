package org.example.util;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.Getter;
import lombok.Setter;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(MockitoExtension.class)
class JsonUtilsTest {
    private static final String TEST_OBJECT_JSON = "{\"val\":\"val1\",\"snake_case\":\"snake\"}";

    @Test
    @DisplayName("Basic object to json")
    public void whenToJsonSuccess() {
        final TestStringObject t = aTestStringObject();
        assertThat(JsonUtils.toJson(t), is(TEST_OBJECT_JSON));
    }

    @Test
    @DisplayName("Basic json to object")
    public void whenFromJsonSuccess() {
        final TestStringObject t = JsonUtils.fromJson(TEST_OBJECT_JSON, TestStringObject.class);
        assertThat(t.getVal(), is("val1"));
        assertThat(t.getSnakeCase(), is("snake"));
    }

    static Stream<Arguments> dataProviderInvalidEnumValues() {
        final String template = "{\"enum_val\": \"%s\"}";
        return Stream.of(
            Arguments.of(String.format(template, "A")),
            Arguments.of(String.format(template, "0")));
    }

    @ParameterizedTest
    @MethodSource("dataProviderInvalidEnumValues")
    @DisplayName("Json to object with non enum values")
    public void enumConversionInvalidValues(final String json) {
        Assertions.assertThrows(IllegalStateException.class, () -> JsonUtils.fromJson(json, TestStringObject.class));
    }

    static Stream<Arguments> dataProviderEnumValues() {
        final String template = "{\"enum_val\": \"%s\"}";
        return Stream.of(
            Arguments.of(String.format(template, " AAA"), TestEnum.AAA),
            Arguments.of(String.format(template, " aaa"), TestEnum.AAA),
            Arguments.of(String.format(template, " bbb"), TestEnum.BBB));
    }

    @ParameterizedTest
    @MethodSource("dataProviderEnumValues")
    @DisplayName("Json to object with valid enum values")
    public void enumConversionSuccess(final String json, final TestEnum expected) {
        final TestStringObject t = JsonUtils.fromJson(json, TestStringObject.class);
        assertThat(t.getEnumVal(), is(expected));
    }

    @Test
    @DisplayName("Validate if nested object deserialization works")
    public void whenTestGenericObjectToJsonSucceeds() {
        final TestGenericObject2 o = new TestGenericObject2();
        o.setKey(101);
        o.setKey2(201);
        o.setVal("value-101");
        o.setVal2("value-201");
        final String json = JsonUtils.toJson(o);
        final TestGenericObject2 actualObj = JsonUtils.fromJson(json, TestGenericObject2.class);
        assertThat(actualObj.getKey(), equalTo(101));
        assertThat(actualObj.getKey2(), equalTo(201));
        assertThat(actualObj.getVal(), equalTo("value-101"));
        assertThat(actualObj.getVal2(), equalTo("value-201"));
    }

    @Getter
    @Setter
    static class TestGenericObject {

        private Integer key;
        private String val;

        TestGenericObject() {

        }
    }

    @Getter
    @Setter
    static class TestGenericObject2 extends TestGenericObject {

        private Integer key2;
        private String val2;
        TestGenericObject2() {

        }
    }

    private TestStringObject aTestStringObject() {
        final TestStringObject t = new TestStringObject();
        t.setVal("val1");
        t.setSnakeCase("snake");
        return t;
    }

    enum TestEnum {
        AAA,
        BBB;
    }

    @Getter
    @Setter
    static class TestStringObject {

        private String val;
        private String snakeCase;
        private TestEnum enumVal;
        TestStringObject() {
        }
    }
}
