package com.platform.accident.enrichment.client;

import com.platform.accident.enrichment.client.dto.WeatherResponse;
import com.platform.accident.enrichment.domain.WeatherMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WeatherAdapterTest {

    private WeatherAdapter weatherAdapter;
    private WebClient webClientMock;

    @BeforeEach
    void setUp() {
        weatherAdapter = new WeatherAdapter();
        webClientMock = mock(WebClient.class);

        // Βάζουμε το mock μέσα στον adapter με το ζόρι (λόγω private final)
        ReflectionTestUtils.setField(weatherAdapter, "webClient", webClientMock);
    }

    @Test
    void testFetchHistoricalWeather() {
        // 1. Προετοιμασία των Mocks για την αλυσίδα του WebClient
        var uriSpecMock = mock(WebClient.RequestHeadersUriSpec.class);
        var headersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        var responseSpecMock = mock(WebClient.ResponseSpec.class);

        when(webClientMock.get()).thenReturn(uriSpecMock);
        when(uriSpecMock.uri(any(java.util.function.Function.class))).thenReturn(headersSpecMock);
        when(headersSpecMock.retrieve()).thenReturn(responseSpecMock);

        // 2. Φτιάχνουμε την ψεύτικη απάντηση
        // Αν τα DTO σου είναι records/κλάσεις, φτιάξε τα αντίστοιχα objects
        WeatherResponse.Hourly hourly = mock(WeatherResponse.Hourly.class);
        when(hourly.temperatures()).thenReturn(List.of(10.0, 11.0, 20.5));
        when(hourly.weatherCodes()).thenReturn(List.of(0, 1, 2));

        WeatherResponse response = mock(WeatherResponse.class);
        when(response.hourly()).thenReturn(hourly);

        when(responseSpecMock.bodyToMono(WeatherResponse.class)).thenReturn(Mono.just(response));

        // 3. ΕΚΤΕΛΕΣΗ (Χρήση .block() για να πάρουμε το αποτέλεσμα άμεσα)
        Instant timestamp = Instant.parse("2023-01-01T02:00:00Z"); // Ώρα 2 -> Index 2
        WeatherMetrics result = weatherAdapter.fetchHistoricalWeather(0, 0, timestamp).block();

        // 4. ΕΛΕΓΧΟΣ
        assertEquals("CODE_2", result.condition());
        assertEquals(20.5, result.temperature());
    }

    @Test
    void testFetchWeatherInterpolation() {
        // Όλο το παραπάνω setup των mocks πάλι (get -> uri -> retrieve -> bodyToMono)
        var uriSpecMock = mock(WebClient.RequestHeadersUriSpec.class);
        var headersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        var responseSpecMock = mock(WebClient.ResponseSpec.class);
        when(webClientMock.get()).thenReturn(uriSpecMock);
        when(uriSpecMock.uri(any(java.util.function.Function.class))).thenReturn(headersSpecMock);
        when(headersSpecMock.retrieve()).thenReturn(responseSpecMock);

        // Mock δεδομένα: Ώρα 0 -> 10 βαθμοί, Ώρα 1 -> 20 βαθμοί
        WeatherResponse.Hourly hourly = mock(WeatherResponse.Hourly.class);
        when(hourly.temperatures()).thenReturn(List.of(10.0, 20.0));
        when(hourly.weatherCodes()).thenReturn(List.of(1, 1));

        WeatherResponse response = mock(WeatherResponse.class);
        when(response.hourly()).thenReturn(hourly);
        when(responseSpecMock.bodyToMono(WeatherResponse.class)).thenReturn(Mono.just(response));

        // Εκτέλεση για Ώρα 0 και 30 λεπτά (00:30)
        // Η μέση τιμή ανάμεσα σε 10 και 20 είναι 15.0
        Instant timestamp = Instant.parse("2023-01-01T00:30:00Z");
        WeatherMetrics result = weatherAdapter.fetchWeatherAtTimestamp(0, 0, timestamp).block();

        assertEquals(15.0, result.temperature());
    }
}