package com.aojt.mgmt.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[^<>]*$", message = "must not contain < or >") String firstname,
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[^<>]*$", message = "must not contain < or >") String lastname,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {
}
