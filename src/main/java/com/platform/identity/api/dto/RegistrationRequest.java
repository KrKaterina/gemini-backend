package com.platform.identity.api.dto;

public record RegistrationRequest(
        String username,
        String password,
        String firstName,
        String lastName,
        String externalReference
) {}
