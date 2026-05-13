package dev.reddragon.app.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Map;

/**
 * Converts unhandled exceptions into a clean JSON error envelope instead of
 * Spring's default HTML stack trace. All responses follow the same shape:
 * <pre>
 * {
 *   "status": 400,
 *   "error":  "Bad Request",
 *   "message": "...",
 *   "path":   "/api/pipeline/manual",
 *   "timestamp": "2026-05-12T..."
 * }
 * </pre>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("Bad request at {}: {}", request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String message = "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue();
        log.warn("Type mismatch at {}: {}", request.getRequestURI(), message);
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedOperation(
            UnsupportedOperationException ex,
            HttpServletRequest request
    ) {
        log.warn("Unsupported operation at {}: {}", request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.NOT_IMPLEMENTED, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        Map<String, Object> body = Map.of(
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message,
                "path", request.getRequestURI(),
                "timestamp", Instant.now().toString()
        );
        return ResponseEntity.status(status).body(body);
    }
}
