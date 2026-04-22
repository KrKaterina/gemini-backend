package com.platform.accident.intelligence.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiLogRepository extends MongoRepository<AiAnalysisLog, String> {}