package com.albertkingdom.shoppingwebsite.handler;

import com.albertkingdom.shoppingwebsite.exception.ConflictException;
import com.albertkingdom.shoppingwebsite.exception.ResourceNotFoundException;
import com.albertkingdom.shoppingwebsite.resource.FieldResource;
import com.albertkingdom.shoppingwebsite.resource.InvalidErrorResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * Handles Bean Validation failures on {@code @Valid @RequestBody} DTOs.
     * <p>
     * Response shape:
     * <pre>{@code
     * {
     *   "message": "Invalid parameter",
     *   "errors": [
     *     { "resource": "registerRequest", "field": "email", "code": "Email",
     *       "message": "Not a valid email format." },
     *     ...
     *   ]
     * }
     * }</pre>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        log.debug("request body validation failed count={}", fieldErrors.size());

        List<FieldResource> fieldResources = new ArrayList<>();
        for (FieldError fieldError : fieldErrors) {
            fieldResources.add(new FieldResource(
                    fieldError.getObjectName(),
                    fieldError.getField(),
                    fieldError.getCode(),
                    fieldError.getDefaultMessage()));
        }
        InvalidErrorResource ier = new InvalidErrorResource("Invalid parameter", fieldResources);
        return ResponseEntity.badRequest().body(ier);
    }

    /**
     * Resource lookup miss. Returns 404 with a small JSON envelope.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException e) {
        log.debug("resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Collections.singletonMap("message", e.getMessage()));
    }

    /**
     * Uniqueness / state conflicts (e.g. duplicate email on register).
     * Returns 409 with a small JSON envelope so clients can distinguish
     * client-recoverable conflicts from generic 500s.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(ConflictException e) {
        log.debug("conflict: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("message", e.getMessage()));
    }

    /**
     * Handles Bean Validation failures on {@code @RequestParam} / method-level
     * constraints (which throw ConstraintViolationException instead of
     * MethodArgumentNotValidException).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        log.debug("constraint violation count={}", violations.size());

        List<FieldResource> fieldResources = new ArrayList<>();
        for (ConstraintViolation<?> v : violations) {
            fieldResources.add(new FieldResource(null, v.getPropertyPath().toString(), null, v.getMessage()));
        }
        InvalidErrorResource ier = new InvalidErrorResource("Invalid parameter", fieldResources);
        return ResponseEntity.badRequest().body(ier);
    }
}
