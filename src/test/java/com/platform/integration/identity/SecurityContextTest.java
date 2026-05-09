package com.platform.integration.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

class SecurityContextTest {

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
    }

    @Test
    @DisplayName("Should return IdentityContext when the USER_CONTEXT attribute is present")
    void getRequired_ReturnsContextWhenPresent() {
        IdentityContext mockContext = new IdentityContext(
                "user-123",
                "test-user",
                List.of("ROLE_USER"),
                List.of("READ_PERM"),
                "REF-001",
                true
        );

        request.setAttribute("USER_CONTEXT", mockContext);

        IdentityContext result = SecurityContext.getRequired(request);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo("user-123");
        assertThat(result).isEqualTo(mockContext);
    }

    @Test
    @DisplayName("Should throw 401 Unauthorized exception when USER_CONTEXT attribute is missing")
    void getRequired_ThrowsExceptionWhenMissing() {

        assertThatThrownBy(() -> SecurityContext.getRequired(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", UNAUTHORIZED) // Ελέγχουμε αν είναι 401
                .hasMessageContaining("Invalid or missing JWT token");
    }

    @Test
    @DisplayName("Should throw ClassCastException if the attribute is not of type IdentityContext")
    void getRequired_ThrowsExceptionOnWrongType() {
        request.setAttribute("USER_CONTEXT", "Wrong Type Object");

        assertThatThrownBy(() -> SecurityContext.getRequired(request))
                .isInstanceOf(ClassCastException.class);
    }
}