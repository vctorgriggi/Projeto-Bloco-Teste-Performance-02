package com.blog.shared.web;

import java.time.Instant;
import java.util.Map;

// formato unico de resposta de erro devolvido pela api. fieldErrors so e
// preenchido em falhas de validacao de dto.
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError ofValidation(int status, String error, String message, String path,
                                        Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }
}
