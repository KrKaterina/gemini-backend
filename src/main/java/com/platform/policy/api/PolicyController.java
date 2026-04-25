package com.platform.policy.api;

import com.platform.integration.identity.IdentityContext;
import com.platform.integration.identity.SecurityContext;
import com.platform.policy.api.dto.DeclarationRequest;
import com.platform.policy.domain.DeclarationStatus;
import com.platform.policy.domain.InsuranceDeclaration;
import com.platform.policy.repository.InsuranceDeclarationRepository;
import com.platform.policy.service.PolicyManagementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyManagementService policyService;

    @PostMapping("/declare")
    public ResponseEntity<String> declareInsurance(
            @RequestBody DeclarationRequest request,
            HttpServletRequest httpRequest) {

        IdentityContext identity = SecurityContext.getRequired(httpRequest);

        // Ανάθεση της δημιουργίας στο Service
        String id = policyService.createDeclaration(identity.userId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    /**
     * Staff-only: Verification endpoint.
     * The Agent corrects the expiration date based on the document.
     */
    @PatchMapping("/{id}/verify")
    public ResponseEntity<Void> verify(
            @PathVariable String id,
            @RequestBody VerificationRequest req,
            HttpServletRequest httpRequest) {

        IdentityContext agentCtx = SecurityContext.getRequired(httpRequest);

        policyService.verifyAndCorrectPolicy(id, req.verifiedExpiry(), agentCtx.userId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending")
    public ResponseEntity<List<InsuranceDeclaration>> getPending(HttpServletRequest httpRequest) {
        IdentityContext agentCtx = SecurityContext.getRequired(httpRequest);
        return ResponseEntity.ok(policyService.getPendingPolicies(agentCtx.userId()));
    }
}

