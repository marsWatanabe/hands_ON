package com.aojt.mgmt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthenticationResponse(String accessToken, String refreshToken, UserResponse user) {
}
