package com.platform.accident.submission.repository;

import com.platform.accident.submission.domain.AccidentReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface AccidentRepository extends MongoRepository<AccidentReport, String> {
    Optional<AccidentReport> findByCaseId(String caseId);
}
