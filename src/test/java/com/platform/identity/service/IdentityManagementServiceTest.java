package com.platform.identity.service;

import com.platform.accident.review.exception.ResourceNotFoundException;
import com.platform.identity.api.UserDashboardProfile;
import com.platform.identity.api.dto.RegistrationRequest;
import com.platform.identity.domain.UserAccount;
import com.platform.identity.domain.UserStatus;
import com.platform.identity.exception.AccountLockedException;
import com.platform.identity.exception.UnauthorizedException;
import com.platform.identity.exception.UserConflictException;
import com.platform.identity.repository.UserAccountRepository;
import com.platform.integration.identity.IdentityClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityManagementServiceTest {

    @Mock private UserAccountRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private IdentityClient identityClient;

    @InjectMocks
    private IdentityManagementService identityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(identityService, "jwtSecret", "MySuperSecretKeyForJWTThatIsLongEnoughToAvoidErrors123456");
    }

    @Test
    @DisplayName("Login Success: Should return JWT token")
    void authenticate_Success() {
        UserAccount user = UserAccount.builder()
                .userId("u1")
                .username("user@test.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("ROLE_CUSTOMER"))
                .build();

        when(userRepository.findByUsername("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw_pass", "hashed")).thenReturn(true);

        String token = identityService.authenticate("user@test.com", "raw_pass");

        assertThat(token).isNotBlank();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Login Failure: Account is locked")
    void authenticate_AccountLocked() {
        UserAccount user = UserAccount.builder().status(UserStatus.LOCKED).build();
        when(userRepository.findByUsername("locked@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> identityService.authenticate("locked@test.com", "pass"))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    @DisplayName("Login Failure: Increment failed attempts and lock at 5")
    void authenticate_WrongPassword_Locking() {
        UserAccount user = UserAccount.builder()
                .username("test@test.com")
                .passwordHash("hashed")
                .failedAttempts(4)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByUsername("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> identityService.authenticate("test@test.com", "wrong"))
                .isInstanceOf(UnauthorizedException.class);

        assertThat(user.getFailedAttempts()).isEqualTo(5);
        assertThat(user.getStatus()).isEqualTo(UserStatus.LOCKED);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Register: Should fail if user already exists")
    void registerUser_AlreadyExists() {
        RegistrationRequest req = new RegistrationRequest("dup@test.com", "p", "f", "l", "r");
        when(userRepository.existsByUsername("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> identityService.registerUser(req, Set.of("ROLE_CUSTOMER")))
                .isInstanceOf(UserConflictException.class);
    }

    @Test
    @DisplayName("Register: Should save new user account")
    void registerUser_Success() {
        RegistrationRequest req = new RegistrationRequest("new@test.com", "pass", "John", "Doe", "REF");
        when(userRepository.existsByUsername("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("hashed_pass");

        identityService.registerUser(req, Set.of("ROLE_CUSTOMER"));

        verify(userRepository).save(any(UserAccount.class));
        verify(identityClient).logSecurityEvent(any(), eq("USER_REGISTERED"), any());
    }

    @Test
    @DisplayName("Status Update: Prevent self-lockout")
    void updateUserStatus_SelfLockPrevented() {
        String adminId = "admin_001";
        UserAccount admin = UserAccount.builder().userId(adminId).build();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> identityService.updateUserStatus(adminId, UserStatus.LOCKED, adminId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Administrative self-lockout is prohibited");
    }

    @Test
    @DisplayName("Status Update: Successful change")
    void updateUserStatus_Success() {
        String adminId = "admin";
        String targetId = "user";
        UserAccount target = UserAccount.builder().userId(targetId).status(UserStatus.ACTIVE).build();

        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        identityService.updateUserStatus(targetId, UserStatus.LOCKED, adminId);

        assertThat(target.getStatus()).isEqualTo(UserStatus.LOCKED);
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("Delete: Success")
    void deleteUser_Success() {
        when(userRepository.existsById("u1")).thenReturn(true);
        identityService.deleteUserAccount("u1", "admin");
        verify(userRepository).deleteById("u1");
    }

    @Test
    @DisplayName("GetAllUsers: Verifies Mapper logic (Display Name & Permissions)")
    void getAllUsers_Success() {
        UserAccount u1 = UserAccount.builder()
                .username("john@test.com")
                .profile(new UserAccount.UserProfile("John", "Constantine"))
                .roles(Set.of("ROLE_CUSTOMER"))
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findAll()).thenReturn(List.of(u1));

        List<UserDashboardProfile> result = identityService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fullName()).isEqualTo("John Constantine");
        assertThat(result.get(0).permissions()).contains("ACCIDENT_REPORT_CREATE");
    }
}