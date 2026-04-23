package com.platform.identity.api;

import com.platform.identity.api.dto.RegistrationRequest;
import com.platform.identity.repository.UserAccountRepository;
import com.platform.identity.service.IdentityManagementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IdentityManagementService identityService;

    private final UserAccountRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest req) {
        String token = identityService.authenticate(req.username(), req.password());
        return ResponseEntity.ok(new TokenResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegistrationRequest req) {
        // ENFORCEMENT: Force-assign ROLE_CUSTOMER only. Ignore body-provided roles.
        identityService.registerUser(req, Set.of("ROLE_CUSTOMER"));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDashboardProfile> getMyProfile(HttpServletRequest request) {
        // DERIVE IDENTITY: Derives user from verified context
        var ctx = com.platform.integration.identity.SecurityContext.getRequired(request);

        // Logic: Fetch additional metadata from Identity Module database
//        return userRepository.findById(ctx.userId())
//                .map(u -> ResponseEntity.ok(new UserDashboardProfile(
//                        u.getUserId(), u.getUsername(), u.getRoles(),
//                        u.getProfile().getFirstName() + " " + u.getProfile().getLastName()
//                )))
//                .orElse(ResponseEntity.status(401).build());
        return userRepository.findById(ctx.userId())
                .map(u -> {
                    // Safe check αν το profile είναι null
                    String fullName = (u.getProfile() != null)
                            ? u.getProfile().getFirstName() + " " + u.getProfile().getLastName()
                            : "Unknown User";

                    return ResponseEntity.ok(new UserDashboardProfile(
                            u.getUserId(), u.getUsername(), u.getRoles(), fullName
                    ));
                })
                .orElse(ResponseEntity.status(401).build());
    }
}
