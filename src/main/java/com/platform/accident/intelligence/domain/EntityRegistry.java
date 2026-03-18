package com.platform.accident.intelligence.domain;

import java.util.List;

public record EntityRegistry(
        List<VehicleEntity> vehicles,
        List<PersonEntity> persons,
        String identifiedLocation
) {}