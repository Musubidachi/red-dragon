package dev.reddragon.app.models;

import java.util.List;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        String path,
        String timestamp,
        List<ApiFieldError> fieldErrors
) {
}
