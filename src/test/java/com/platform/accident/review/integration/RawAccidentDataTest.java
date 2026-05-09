package com.platform.accident.review.integration;

import com.platform.accident.submission.domain.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RawAccidentDataTest {

    @Test
    @DisplayName("Record Verification: Should correctly store and retrieve accident source data")
    void testRawAccidentDataProperties() {
        String description = "Frontal collision at traffic light";
        Location location = new Location(38.01, 23.82, "Leoforos Kifisias 10, Athens");
        Instant occurrenceTime = Instant.now();

        RawAccidentData data = new RawAccidentData(description, location, occurrenceTime);

        assertThat(data.description()).isEqualTo(description);
        assertThat(data.location()).isEqualTo(location);
        assertThat(data.location().address()).isEqualTo("Leoforos Kifisias 10, Athens");
        assertThat(data.occurrenceTime()).isEqualTo(occurrenceTime);
    }

    @Test
    @DisplayName("Record Equality: Verifies equals and hashCode for logic consistency")
    void testEqualityAndHashCode() {
        Location loc = new Location(1.0, 1.0, "Test");
        Instant time = Instant.parse("2024-01-01T12:00:00Z");

        RawAccidentData data1 = new RawAccidentData("Desc", loc, time);
        RawAccidentData data2 = new RawAccidentData("Desc", loc, time);
        RawAccidentData data3 = new RawAccidentData("Different", loc, time);

        assertThat(data1).isEqualTo(data2);
        assertThat(data1).isNotEqualTo(data3);

        assertThat(data1.hashCode()).isEqualTo(data2.hashCode());
    }

    @Test
    @DisplayName("Record ToString: Verifies toString output for coverage")
    void testToString() {
        Location loc = new Location(10, 20, "Address");
        RawAccidentData data = new RawAccidentData("Report", loc, Instant.now());

        assertThat(data.toString())
                .contains("description=Report")
                .contains("location=")
                .contains("occurrenceTime=");
    }
}