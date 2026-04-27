package com.platform.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CustomerDashboardResponse.class, name = "CUSTOMER"),
        @JsonSubTypes.Type(value = AgentDashboardResponse.class, name = "AGENT")
})
public sealed interface DashboardResponse
        permits CustomerDashboardResponse, AgentDashboardResponse {
    String role();
    String fullName();
}
