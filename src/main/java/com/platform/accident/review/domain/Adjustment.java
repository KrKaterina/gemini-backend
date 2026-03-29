package com.platform.accident.review.domain;

import lombok.Value;

@Value
public class Adjustment {
    String field;
    String originalValue;
    String correctedValue;
    String reason;
}
