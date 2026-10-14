package com.aojt.mgmt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "Taro") @NotBlank @Size(max = 100)
        @Pattern(regexp = "^[^<>]*$", message = "must not contain < or >") String firstname,
        @Schema(example = "Yamada") @NotBlank @Size(max = 100)
        @Pattern(regexp = "^[^<>]*$", message = "must not contain < or >") String lastname,
        @Schema(example = "taro@example.com") @NotBlank @Email @Size(max = 255) String email,
        @Schema(example = "password123", description = "8-72 characters") @NotBlank @Size(min = 8, max = 72)
        String password) {
}
