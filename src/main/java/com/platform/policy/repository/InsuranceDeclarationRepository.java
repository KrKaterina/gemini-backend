package com.platform.policy.repository;

import com.platform.policy.domain.InsuranceDeclaration;
import com.platform.policy.domain.DeclarationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface InsuranceDeclarationRepository extends MongoRepository<InsuranceDeclaration, String> {

    /**
     * Retrieves all policy declarations associated with a specific user.
     * Used for building the Customer Policy History view.
     */
    List<InsuranceDeclaration> findAllByUserId(String userId);

    /**
     * Filters policies by their current lifecycle status (e.g., PENDING, ACTIVE).
     * Critical for the Agent's "Review Queue" Discovery Logic.
     */
    List<InsuranceDeclaration> findByStatus(DeclarationStatus status);

    /**
     * Fetches the latest policy for a user with a specific status.
     * Essential for Eligibility Port checks during the Accident Submission process.
     */
    Optional<InsuranceDeclaration> findFirstByUserIdAndStatus(String userId, DeclarationStatus status);
}