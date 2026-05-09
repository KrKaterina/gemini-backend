package com.platform.accident.enrichment.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherCodeMapperTest {

    @Test
    @DisplayName("getDescription: Should map integer codes to correct descriptions")
    void getDescription_ValidAndInvalid() {
        assertThat(WeatherCodeMapper.getDescription(0)).isEqualTo("Clear sky");
        assertThat(WeatherCodeMapper.getDescription(95)).isEqualTo("Thunderstorm");

        assertThat(WeatherCodeMapper.getDescription(999)).isEqualTo("Unknown Weather (Code 999)");
    }

    @Test
    @DisplayName("translate: Should handle null, empty or blank strings")
    void translate_NullEmptyBlank() {
        assertThat(WeatherCodeMapper.translate(null)).isEqualTo("Unknown conditions");
        assertThat(WeatherCodeMapper.translate("")).isEqualTo("Unknown conditions");
        assertThat(WeatherCodeMapper.translate("   ")).isEqualTo("Unknown conditions");
    }

    @ParameterizedTest
    @DisplayName("translate: Should correctly parse valid CODE_ patterns")
    @CsvSource({
            "CODE_0, Clear sky",
            "CODE_3, Overcast",
            "CODE_51, Light drizzle",
            "CODE_80, Slight rain showers"
    })
    void translate_ValidPatterns(String input, String expected) {
        assertThat(WeatherCodeMapper.translate(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("translate: Should return fallback for valid pattern with unknown ID")
    void translate_UnknownCodeWithPrefix() {
        assertThat(WeatherCodeMapper.translate("CODE_777")).isEqualTo("Unknown conditions");
    }

    @Test
    @DisplayName("translate: Should catch NumberFormatException and return fallback")
    void translate_InvalidNumber() {
        assertThat(WeatherCodeMapper.translate("CODE_ABC")).isEqualTo("Unspecified weather");
    }

    @Test
    @DisplayName("translate: Should return raw string if CODE_ prefix is missing")
    void translate_NoPrefix() {
        String customWeather = "Stormy with hail";
        assertThat(WeatherCodeMapper.translate(customWeather)).isEqualTo(customWeather);
    }
}