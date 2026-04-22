package com.platform.identity.api;

import com.platform.identity.service.IdentityManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IdentityManagementService identityService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest req) {
        String token = identityService.authenticate(req.username(), req.password());
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
