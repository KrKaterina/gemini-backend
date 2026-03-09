package com.platform.accident.submission.exception;

public class AccidentNotFoundException extends RuntimeException {
    public AccidentNotFoundException(String caseId) {
        super("Accident report not found for Case ID: " + caseId);
    }
}
