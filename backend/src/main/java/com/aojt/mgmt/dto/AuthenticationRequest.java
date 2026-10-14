package com.aojt.mgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AuthenticationRequest(
        @Schema(example = "taro@example.com") @NotBlank String email,
        @Schema(example = "password123") @NotBlank String password) {
}
