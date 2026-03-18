package com.platform.accident.intelligence.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AiLogRepository extends MongoRepository<AiAnalysisLog, String> {}
