package dev.reddragon.app.models;

public record ApiFieldError(
        String field,
        String message,
        Object rejectedValue
) {
}
