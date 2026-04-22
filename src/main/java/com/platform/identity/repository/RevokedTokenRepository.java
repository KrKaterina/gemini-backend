package com.platform.identity.repository;

import com.platform.identity.domain.RevokedToken;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RevokedTokenRepository extends MongoRepository<RevokedToken, String> {
    boolean existsByToken(String token);
}