package com.albertkingdom.shoppingwebsite.handler;

import com.albertkingdom.shoppingwebsite.resource.FieldResource;
import com.albertkingdom.shoppingwebsite.resource.InvalidErrorResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
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
