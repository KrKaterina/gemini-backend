package com.platform.accident.enrichment.domain;

public record RoadGeometry(
        String streetName,
        String roadType,
        String speedLimit
) {}
