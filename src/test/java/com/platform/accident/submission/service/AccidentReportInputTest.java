package com.platform.accident.submission.service;

import com.platform.accident.submission.domain.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccidentReportInputTest {

    @Test
    @DisplayName("Record Integrity: Should store and return data correctly")
    void recordConstructorAndAccessors() {
        Instant time = Instant.now();
        Location location = new Location(10.0, 20.0, "Test Address");
        String desc = "Accident description";
        List<String> assets = List.of("id-1");

        AccidentReportInput input = new AccidentReportInput(time, location, desc, assets);

        assertThat(input.occurrenceTime()).isEqualTo(time);
        assertThat(input.location()).isEqualTo(location);
        assertThat(input.description()).isEqualTo(desc);
        assertThat(input.assetIds()).isEqualTo(assets);
    }

    @Test
    @DisplayName("Value Equality: Two records with same data should be equal")
    void recordEquality() {
        Instant time = Instant.parse("2023-10-01T10:00:00Z");
        Location loc = new Location(1, 1, "Place");

        AccidentReportInput input1 = new AccidentReportInput(time, loc, "Test", List.of("1"));
        AccidentReportInput input2 = new AccidentReportInput(time, loc, "Test", List.of("1"));

        assertThat(input1).isEqualTo(input2);
        assertThat(input1.hashCode()).isEqualTo(input2.hashCode());
    }
}