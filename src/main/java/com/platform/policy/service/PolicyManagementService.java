package com.platform.policy.service;

import com.platform.integration.identity.IdentityClient;
import com.platform.integration.policy.*;
import com.platform.policy.api.dto.DeclarationRequest;
import com.platform.policy.domain.*;
import com.platform.policy.exception.*;
import com.platform.policy.repository.InsuranceDeclarationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyManagementService implements PolicyPort {

    private final InsuranceDeclarationRepository repository;
    private final IdentityClient identityClient;

    /**
     * Δημιουργία νέας δήλωσης (Refactored logic από τον Controller)
     */
    @Transactional
    public String createDeclaration(String userId, DeclarationRequest request) {
        InsuranceDeclaration declaration = InsuranceDeclaration.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .providerCode(request.providerCode())
                .policyNumber(request.policyNumber())
                .validFrom(request.validFrom())
                .reportedExpirationDate(request.expiryDate()) // Mapping του DTO στο Domain
                .proofAssetId(request.proofAssetId())
                .status(DeclarationStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        repository.save(declaration);
        log.info("New policy declaration saved for user {}", userId);
        return declaration.getId();
    }

    /**
     * Υλοποίηση του PolicyPort για το Submission Module.
     * Επιστρέφει πλήρες EligibilityStatus για να ξέρει το UI γιατί απορρίφθηκε η αίτηση.
     */
    @Override
    public EligibilityStatus checkEligibility(String userId, Instant occurrenceTime) {
        return repository.findAllByUserId(userId).stream()
                .filter(p -> p.getStatus() == DeclarationStatus.ACTIVE)
                .filter(p -> p.getVerifiedExpirationDate() != null) // Σιγουρευόμαστε ότι υπάρχει λήξη
                .filter(p -> {
                    // Αν δεν έχει οριστεί έναρξη (validFrom), θεωρούμε ότι η κάλυψη ισχύει από την αρχή του χρόνου
                    Instant start = (p.getValidFrom() != null) ? p.getValidFrom() : Instant.MIN;
                    return !occurrenceTime.isBefore(start) && !occurrenceTime.isAfter(p.getVerifiedExpirationDate());
                })
                .findFirst()
                .map(p -> new EligibilityStatus(true, "VALID", p.getPolicyNumber(), p.getProviderCode()))
                .orElse(EligibilityStatus.denied("NO_ACTIVE_POLICY_FOR_DATE"));
    }

    /**
     * Επιστρέφει snapshot της ασφάλειας για εξωτερικές ειδοποιήσεις.
     */
    @Override
    public Optional<InsurerSnapshot> getActiveDeclaration(String userId, Instant incidentTime) {
        return repository.findAllByUserId(userId).stream()
                .filter(p -> p.getStatus() == DeclarationStatus.ACTIVE)
                .filter(p -> !incidentTime.isBefore(p.getValidFrom()) &&
                        !incidentTime.isAfter(p.getVerifiedExpirationDate()))
                .findFirst()
                .map(p -> new InsurerSnapshot(p.getId(), p.getProviderCode(), p.getPolicyNumber(), "https://api.external.com"));
    }

    /**
     * Η βασική logic για την επικύρωση από Agent (Gatekeeper).
     */
    @Transactional
    public void verifyAndCorrectPolicy(String id, Instant correctedExpiry, String agentId) {
        if (!identityClient.hasPermission(agentId, "POLICY_VERIFY")) {
            throw new UnauthorizedPolicyAccessException("Missing permission POLICY_VERIFY");
        }

        InsuranceDeclaration decl = repository.findById(id)
                .orElseThrow(() -> new PolicyNotFoundException(id));

        String auditDetail = String.format("Agent %s corrected expiry from %s to %s",
                agentId, decl.getReportedExpirationDate(), correctedExpiry);

        decl.setVerifiedExpirationDate(correctedExpiry);
        decl.setVerifiedByAgentId(agentId);
        decl.setVerifiedAt(Instant.now());
        decl.setStatus(DeclarationStatus.ACTIVE);

        repository.save(decl);
        identityClient.logSecurityEvent(agentId, "POLICY_VERIFIED_AND_CORRECTED", auditDetail);
    }

    /**
     * Discovery logic για το Agent Dashboard.
     */
    public List<InsuranceDeclaration> getPendingPolicies(String agentId) {
        if (!identityClient.hasPermission(agentId, "POLICY_VIEW_QUEUE")) {
            throw new UnauthorizedPolicyAccessException("Agent not authorized to view policy queue.");
        }
        return repository.findByStatus(DeclarationStatus.PENDING);
    }

    /**
     * Ασύγχρονη αποστολή δεδομένων στην ασφαλιστική.
     */
    @Async
    public void notifyInsurerOfVerifiedClaim(String userId, String caseId, Object claimData) {
        getActiveDeclaration(userId, Instant.now()).ifPresent(snapshot -> {
            log.info("NOTIFYING INSURER: Case {} for Provider {}. Endpoint: {}",
                    caseId, snapshot.providerCode(), snapshot.insurerNotificationEndpoint());

            // Εδώ μπαίνει η WebClient υλοποίηση
        });
    }
}