package com.platform.identity.repository;

import com.platform.identity.domain.IdentityEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IdentityAuditRepository extends MongoRepository<IdentityEvent, String> {
}