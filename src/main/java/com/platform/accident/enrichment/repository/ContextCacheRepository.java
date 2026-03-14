package com.platform.accident.enrichment.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ContextCacheRepository extends MongoRepository<ExternalContextCache, String> {
    Optional<ExternalContextCache> findByGeoHashAndReferenceTime(String geoHash, Instant referenceTime);
}

