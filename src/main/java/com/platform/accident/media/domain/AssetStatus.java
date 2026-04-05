package com.platform.accident.media.domain;

public enum AssetStatus {
    PENDING,  // Uploaded but not linked to a report
    LINKED,   // Permanent evidence attached to a CaseId
    ARCHIVED, // Moved to long-term cold storage
    DELETED   // Logically deleted
}