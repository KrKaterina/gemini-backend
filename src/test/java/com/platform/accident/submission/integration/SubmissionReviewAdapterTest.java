package com.platform.accident.submission.integration;

import com.platform.accident.submission.domain.AccidentReport;
import com.platform.accident.submission.repository.AccidentRepository;
import com.platform.integration.review.AccidentSnapshotView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionReviewAdapterTest {

    @Mock
    private AccidentRepository repository;

    @Mock
    private AccidentSnapshotMapper mapper;

    @InjectMocks
    private SubmissionReviewAdapter adapter;

    @Test
    @DisplayName("Viewer: Should return a mapped view when accident report exists")
    void getRawData_WhenReportExists_ReturnsMappedView() {
        String caseId = "ACC-X";
        AccidentReport report = new AccidentReport();
        AccidentSnapshotView mockView = mock(AccidentSnapshotView.class);

        when(repository.findByCaseId(caseId)).thenReturn(Optional.of(report));
        when(mapper.mapToView(report)).thenReturn(mockView);

        Optional<AccidentSnapshotView> result = adapter.getRawData(caseId);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(mockView);

        verify(repository).findByCaseId(caseId);
        verify(mapper).mapToView(report);
    }

    @Test
    @DisplayName("Viewer: Should return empty Optional when accident report is missing")
    void getRawData_WhenReportMissing_ReturnsEmpty() {
        String caseId = "MISSING-ID";
        when(repository.findByCaseId(caseId)).thenReturn(Optional.empty());

        Optional<AccidentSnapshotView> result = adapter.getRawData(caseId);

        assertThat(result).isEmpty();

        verifyNoInteractions(mapper);
    }
}