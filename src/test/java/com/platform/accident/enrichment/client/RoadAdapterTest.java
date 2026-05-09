package com.platform.accident.enrichment.client;

import com.platform.accident.enrichment.client.dto.OverpassResponse;
import com.platform.accident.enrichment.domain.RoadGeometry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoadAdapterTest {

    private RoadAdapter roadAdapter;
    private WebClient webClientMock;

    @BeforeEach
    void setUp() {
        roadAdapter = new RoadAdapter();
        webClientMock = mock(WebClient.class);
        ReflectionTestUtils.setField(roadAdapter, "webClient", webClientMock);
    }

    @Test
    void testFetchRoadMetadata_Success() {
        var uriSpecMock = mock(WebClient.RequestBodyUriSpec.class);
        var bodySpecMock = mock(WebClient.RequestBodySpec.class);
        var headersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        var responseSpecMock = mock(WebClient.ResponseSpec.class);

        when(webClientMock.post()).thenReturn(uriSpecMock);
        when(uriSpecMock.uri(any(String.class))).thenReturn(bodySpecMock);
        when(bodySpecMock.contentType(any())).thenReturn(bodySpecMock);
        when(bodySpecMock.body(any())).thenReturn(headersSpecMock);
        when(headersSpecMock.retrieve()).thenReturn(responseSpecMock);

        var element = new OverpassResponse.Element(Map.of(
                "name", "Kifisias Ave",
                "highway", "primary",
                "maxspeed", "70km/h"
        ));
        var mockResponse = new OverpassResponse(List.of(element));
        when(responseSpecMock.bodyToMono(OverpassResponse.class)).thenReturn(Mono.just(mockResponse));

        RoadGeometry result = roadAdapter.fetchRoadMetadata(37.9, 23.7).block();

        assertEquals("Kifisias Ave", result.streetName());
        assertEquals("70", result.speedLimit());
        assertEquals("primary", result.roadType());
    }

    @Test
    void testFetchRoadMetadata_Empty() {
        var uriSpecMock = mock(WebClient.RequestBodyUriSpec.class);
        var bodySpecMock = mock(WebClient.RequestBodySpec.class);
        var headersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        var responseSpecMock = mock(WebClient.ResponseSpec.class);
        when(webClientMock.post()).thenReturn(uriSpecMock);
        when(uriSpecMock.uri(any(String.class))).thenReturn(bodySpecMock);
        when(bodySpecMock.contentType(any())).thenReturn(bodySpecMock);
        when(bodySpecMock.body(any())).thenReturn(headersSpecMock);
        when(headersSpecMock.retrieve()).thenReturn(responseSpecMock);

        var mockResponse = new OverpassResponse(List.of());
        when(responseSpecMock.bodyToMono(OverpassResponse.class)).thenReturn(Mono.just(mockResponse));

        RoadGeometry result = roadAdapter.fetchRoadMetadata(0, 0).block();

        assertEquals("Off-road Area", result.streetName());
        assertEquals("unpaved", result.roadType());
    }
}