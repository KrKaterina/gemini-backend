package com.platform.identity.api;


import com.platform.identity.api.dto.RegistrationRequest;
import com.platform.identity.api.dto.StaffOnboardRequest;
import com.platform.identity.service.IdentityManagementService;
import com.platform.integration.identity.IdentityClient;
import com.platform.integration.identity.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserManagementController {
    private final IdentityManagementService identityService;
    private final IdentityClient identityClient;

    @PostMapping("/staff")
    public ResponseEntity<Void> onboard(@RequestBody StaffOnboardRequest req, HttpServletRequest request) {
        var adminCtx = SecurityContext.getRequired(request);

        // ENFORCEMENT: Check specific capability, not just role
        if (!identityClient.hasPermission(adminCtx.userId(), "USER_MANAGEMENT")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Insufficient privileges for staff onboarding");
        }

        // Logical logic for Agent creation
        identityService.registerUser(new RegistrationRequest(
                        req.username(), req.password(), req.firstName(), req.lastName(), "INTERNAL_STAFF"),
                req.roles());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<UserDashboardProfile>> getAllUsers(HttpServletRequest request) {
        var adminCtx = SecurityContext.getRequired(request);

        // Έλεγχος δικαιωμάτων: Μόνο όσοι έχουν USER_MANAGEMENT βλέπουν τη λίστα
        if (!identityClient.hasPermission(adminCtx.userId(), "USER_MANAGEMENT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Missing USER_MANAGEMENT permission");
        }

        return ResponseEntity.ok(identityService.getAllUsers());
    }
}

