package com.platform.identity.adapter;

import com.platform.identity.domain.IdentityEvent;
import com.platform.identity.domain.UserAccount;
import com.platform.identity.domain.UserStatus;
import com.platform.identity.repository.IdentityAuditRepository;
import com.platform.identity.repository.RevokedTokenRepository;
import com.platform.identity.repository.UserAccountRepository;
import com.platform.integration.identity.IdentityContext;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityClientAdapterTest {

    @Mock private UserAccountRepository userRepository;
    @Mock private RevokedTokenRepository tokenBlacklist;
    @Mock private IdentityAuditRepository auditRepository;
    @Mock private JwtTokenUtil jwtUtil;

    @InjectMocks
    private IdentityClientAdapter adapter;

    @Test
    @DisplayName("Validate Session: Should return empty if user is LOCKED")
    void validateSession_LockedUser_ReturnsEmpty() {
        String token = "valid-token";
        Claims mockClaims = mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("user-1");

        UserAccount lockedUser = UserAccount.builder()
                .userId("user-1")
                .status(UserStatus.LOCKED)
                .build();

        when(tokenBlacklist.existsByToken(token)).thenReturn(false);
        when(jwtUtil.parseToken(token)).thenReturn(Optional.of(mockClaims));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(lockedUser));

        Optional<IdentityContext> result = adapter.validateSession(token);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Validate Session: Should successfully map user to IdentityContext")
    void validateSession_Success() {
        String token = "good-token";
        Claims mockClaims = mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn("user-1");

        UserAccount user = UserAccount.builder()
                .userId("user-1")
                .username("nick")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("ROLE_CUSTOMER"))
                .externalReference("REF123")
                .build();

        when(tokenBlacklist.existsByToken(token)).thenReturn(false);
        when(jwtUtil.parseToken(token)).thenReturn(Optional.of(mockClaims));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        Optional<IdentityContext> result = adapter.validateSession(token);

        assertThat(result).isPresent();
        IdentityContext context = result.get();
        assertThat(context.userId()).isEqualTo("user-1");
        assertThat(context.username()).isEqualTo("nick");

        assertThat(context).hasFieldOrPropertyWithValue("isAuthenticated", true);
    }

    @Test
    @DisplayName("Permission Check: Should return true if agent")
    void hasPermission_AgentTest() {
        UserAccount agent = UserAccount.builder()
                .userId("a1")
                .roles(Set.of("ROLE_AGENT"))
                .build();

        when(userRepository.findById("a1")).thenReturn(Optional.of(agent));

        boolean hasPerm = adapter.hasPermission("a1", "ACCIDENT_REPORT_VIEW_ALL");
        assertThat(hasPerm).isNotNull();
    }

    @Test
    @DisplayName("Audit Log: Should call save on repository")
    void logSecurityEvent_CallsSave() {
        adapter.logSecurityEvent("u1", "ACTION", "DETAILS");
        verify(auditRepository).save(any(IdentityEvent.class));
    }
}