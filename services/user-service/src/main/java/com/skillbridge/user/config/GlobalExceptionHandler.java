package com.skillbridge.user.config;

import com.skillbridge.user.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .sorted(Comparator.comparing(FieldError::getField))
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest()
            .body(ApiResponse.error("validation", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest()
            .body(ApiResponse.error("validation", message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status)
            .body(ApiResponse.error(errorCodeFor(status), ex.getReason()));
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        MethodArgumentTypeMismatchException.class,
        MissingRequestHeaderException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("bad_request", readableMessage(ex)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("conflict", "Requested data conflicts with an existing record"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("not_found", "Resource not found: " + ex.getResourcePath()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("internal_error", "Unexpected error while processing " + request.getRequestURI()));
    }

    private String errorCodeFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "not_found";
            case UNAUTHORIZED -> "unauthorized";
            case FORBIDDEN -> "forbidden";
            case CONFLICT -> "conflict";
            case BAD_REQUEST -> "bad_request";
            default -> "error";
        };
    }

    private String readableMessage(Exception ex) {
        if (ex instanceof MissingRequestHeaderException missingHeader) {
            return "Missing required header: " + missingHeader.getHeaderName();
        }
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return "Invalid value for parameter: " + mismatch.getName();
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Request body is missing or malformed";
        }
        return ex.getMessage();
    }
}
