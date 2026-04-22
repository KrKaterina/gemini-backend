package com.platform.identity.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

//@Value
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "identity_audit_log")
public class IdentityEvent {
    @Id String id;
    String userId;
    String action; // LOGIN_SUCCESS, LOGIN_FAILURE, ROLE_CHANGE
    Instant timestamp;
    String ipAddress;
    String details;
}
