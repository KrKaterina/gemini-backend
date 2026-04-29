package com.platform.identity.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginRequest(@JsonProperty("email") String username, String password) {

}
