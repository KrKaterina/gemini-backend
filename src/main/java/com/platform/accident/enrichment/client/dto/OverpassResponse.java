package com.platform.accident.enrichment.client.dto;

import java.util.List;
import java.util.Map;

public record OverpassResponse(List<Element> elements) {
    public record Element(Map<String, String> tags) {}
}