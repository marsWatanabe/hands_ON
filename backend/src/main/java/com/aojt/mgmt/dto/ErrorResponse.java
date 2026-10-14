package com.aojt.mgmt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record ErrorResponse(Error error) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new Error(code, message, LocalDateTime.now()));
    }

    public record Error(
            @JsonProperty("code") String code,
            @JsonProperty("message") String message,
            @JsonProperty("timestamp") LocalDateTime timestamp) {
    }
}
