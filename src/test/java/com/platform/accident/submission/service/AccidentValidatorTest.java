package com.platform.accident.submission.service;

import com.platform.accident.submission.domain.Location;
import com.platform.accident.submission.exception.AssetOwnershipException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccidentValidatorTest {

    private final AccidentValidator validator = new AccidentValidator();

    @Test
    @DisplayName("Validation: Should pass for valid input data")
    void validateInput_Success() {
        AccidentReportInput input = new AccidentReportInput(
                Instant.now().minus(1, ChronoUnit.HOURS), // 1 ώρα πριν
                new Location(38.0, 23.0, "Athens"),
                "Valid description",
                Collections.emptyList()
        );

        assertThatCode(() -> validator.validateInput(input)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Validation: Should fail for future occurrence dates")
    void validateInput_FutureDate_ThrowsException() {
        AccidentReportInput input = new AccidentReportInput(
                Instant.now().plus(1, ChronoUnit.DAYS), // 1 μέρα στο μέλλον
                new Location(38.0, 23.0, "Athens"),
                "Test",
                Collections.emptyList()
        );

        assertThatThrownBy(() -> validator.validateInput(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(AccidentValidator.ERR_FUTURE_DATE);
    }

    @Test
    @DisplayName("Validation: Should fail for latitude outside bounds (-90 to 90)")
    void validateInput_InvalidLat_ThrowsException() {
        AccidentReportInput inputLower = new AccidentReportInput(
                Instant.now().minus(1, ChronoUnit.HOURS),
                new Location(-91.0, 23.0, "Invalid"),
                "Test",
                Collections.emptyList()
        );

        AccidentReportInput inputHigher = new AccidentReportInput(
                Instant.now().minus(1, ChronoUnit.HOURS),
                new Location(95.0, 23.0, "Invalid"),
                "Test",
                Collections.emptyList()
        );

        assertThatThrownBy(() -> validator.validateInput(inputLower))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(AccidentValidator.ERR_INVALID_COORDS);

        assertThatThrownBy(() -> validator.validateInput(inputHigher))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(AccidentValidator.ERR_INVALID_COORDS);
    }

    @Test
    @DisplayName("Validation: Should fail if description is missing or empty")
    void validateInput_EmptyDescription_ThrowsException() {
        AccidentReportInput input = new AccidentReportInput(
                Instant.now().minus(1, ChronoUnit.HOURS),
                new Location(0, 0, "Test"),
                "  ", // Blank
                Collections.emptyList()
        );

        assertThatThrownBy(() -> validator.validateInput(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Description is mandatory");
    }

    @Test
    @DisplayName("Assets: Should throw exception if more than 10 assets are attached")
    void verifyAssetOwnership_TooManyAssets_ThrowsException() {
        List<String> assetIds = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            assetIds.add("asset-" + i);
        }

        assertThatThrownBy(() -> validator.verifyAssetOwnership("user-1", assetIds))
                .isInstanceOf(AssetOwnershipException.class)
                .hasMessageContaining("Too many assets");
    }

    @Test
    @DisplayName("Assets: Should pass if 10 or fewer assets are attached")
    void verifyAssetOwnership_Success() {
        List<String> assetIds = List.of("1", "2", "3");

        assertThatCode(() -> validator.verifyAssetOwnership("user-1", assetIds))
                .doesNotThrowAnyException();
    }
}