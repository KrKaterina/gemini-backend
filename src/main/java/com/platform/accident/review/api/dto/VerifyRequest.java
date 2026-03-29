package com.platform.accident.review.api.dto;

import com.platform.accident.review.domain.Adjustment;
import java.util.List;

public record VerifyRequest(
        String finalSeverity,
        List<Adjustment> adjustments
) {}
