package com.platform.accident.submission.integration;

import com.platform.accident.submission.repository.AccidentRepository;
import com.platform.accident.review.integration.ReportViewerClient;
import com.platform.integration.review.AccidentSnapshotView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SubmissionReviewAdapter implements ReportViewerClient {

    private final AccidentRepository repository;
    private final AccidentSnapshotMapper mapper; // Inject ο Mapper

    @Override
    public Optional<AccidentSnapshotView> getRawData(String caseId) {
        return repository.findByCaseId(caseId)
                .map(mapper::mapToView); // Μια γραμμή, μηδενικό noise
    }
}