package com.platform.integration.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InsurerSnapshotTest {

    @Test
    @DisplayName("Record Verification: Should correctly store and retrieve insurer metadata")
    void testInsurerSnapshotProperties() {
        // Arrange
        String declId = "DECL-123";
        String pCode = "ALLIANZ";
        String pNum = "POL-0001";
        String endpoint = "https://api.allianz.com/claims";

        // Act
        InsurerSnapshot snapshot = new InsurerSnapshot(declId, pCode, pNum, endpoint);

        // Assert
        assertThat(snapshot.declarationId()).isEqualTo(declId);
        assertThat(snapshot.providerCode()).isEqualTo(pCode);
        assertThat(snapshot.policyNumber()).isEqualTo(pNum);
        assertThat(snapshot.insurerNotificationEndpoint()).isEqualTo(endpoint);
    }

    @Test
    @DisplayName("Record Equality: Verifies equals and hashCode for logic consistency")
    void testEqualityAndHashCode() {
        InsurerSnapshot snap1 = new InsurerSnapshot("1", "A", "P", "E");
        InsurerSnapshot snap2 = new InsurerSnapshot("1", "A", "P", "E");
        InsurerSnapshot snap3 = new InsurerSnapshot("2", "A", "P", "E");

        // Equals
        assertThat(snap1).isEqualTo(snap2);
        assertThat(snap1).isNotEqualTo(snap3);

        // HashCode
        assertThat(snap1.hashCode()).isEqualTo(snap2.hashCode());
        assertThat(snap1.hashCode()).isNotEqualTo(snap3.hashCode());
    }

    @Test
    @DisplayName("Record ToString: Verifies toString output for logging coverage")
    void testToString() {
        InsurerSnapshot snapshot = new InsurerSnapshot("D1", "CODE", "123", "URL");
        String result = snapshot.toString();

        assertThat(result)
                .contains("declarationId=D1")
                .contains("providerCode=CODE")
                .contains("policyNumber=123")
                .contains("insurerNotificationEndpoint=URL");
    }
}