package com.platform.integration.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleSecurityInterceptorTest {

    @Mock
    private IdentityClient identityClient;

    @InjectMocks
    private ModuleSecurityInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("PreHandle: OPTIONS request should always be allowed")
    void preHandle_OptionsRequest_ReturnsTrue() {
        request.setMethod("OPTIONS");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verifyNoInteractions(identityClient);
    }

    @Test
    @DisplayName("PreHandle: Login path should be exempted from security check")
    void preHandle_LoginPath_ReturnsTrue() {
        request.setRequestURI("/api/v1/auth/login");
        request.setMethod("POST");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verifyNoInteractions(identityClient);
    }

    @Test
    @DisplayName("PreHandle: Request with missing Authorization header should be denied with 401")
    void preHandle_MissingHeader_ReturnsFalseAnd401() {
        request.setRequestURI("/api/v1/protected-data");

        // Δεν προσθέτουμε Authorization header

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("PreHandle: Valid token should set USER_CONTEXT and allow request")
    void preHandle_ValidToken_SetsAttributeAndReturnsTrue() {
        String token = "valid-jwt-token";
        request.setRequestURI("/api/v1/cases");
        request.addHeader("Authorization", "Bearer " + token);

        IdentityContext mockContext = new IdentityContext(
                "user-1", "john_doe", List.of(), List.of(), "REF", true
        );

        when(identityClient.validateSession(token)).thenReturn(Optional.of(mockContext));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(request.getAttribute("USER_CONTEXT")).isEqualTo(mockContext);
    }

    @Test
    @DisplayName("PreHandle: Invalid or expired token should return false and 401")
    void preHandle_InvalidToken_ReturnsFalseAnd401() {
        String token = "expired-token";
        request.setRequestURI("/api/v1/cases");
        request.addHeader("Authorization", "Bearer " + token);

        when(identityClient.validateSession(token)).thenReturn(Optional.empty());

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(request.getAttribute("USER_CONTEXT")).isNull();
    }

    @Test
    @DisplayName("PreHandle: Header without Bearer prefix should be denied")
    void preHandle_InvalidHeaderFormat_ReturnsFalse() {
        request.setRequestURI("/api/v1/cases");
        request.addHeader("Authorization", "Basic c29tZXVzZXI6cGFzcw=="); // Λάθος format (Basic)

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }
}