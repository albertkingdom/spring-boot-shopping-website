package com.albertkingdom.shoppingwebsite.handler;

import com.albertkingdom.shoppingwebsite.Exception.InvalidRequestException;
import com.albertkingdom.shoppingwebsite.resource.FieldResource;
import com.albertkingdom.shoppingwebsite.resource.InvalidErrorResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     *  deal with invalid request parameters
     *  return following error json to front-end
     * {
     *     "message": "Invalid parameter",
     *     "errors": [
     *         {
     *             "resource": "user",
     *             "field": "name",
     *             "code": "NotEmpty",
     *             "message": "Name should not be empty."
     *         },
     *         {
     *             "resource": "user",
     *             "field": "email",
     *             "code": "Email",
     *             "message": "Not a valid email format."
     *         },
     *         {
     *             "resource": "user",
     *             "field": "password",
     *             "code": "Size",
     *             "message": "Password length should be at least 6 characters."
     *         }
     *     ]
     * }
     * @param e
     * @return
     */
    @ExceptionHandler(InvalidRequestException.class)
    @ResponseBody
    public ResponseEntity<?> handleInvalidRequest(InvalidRequestException e) {
        Errors errors = e.getErrors();
        List<FieldError> fieldErrors = errors.getFieldErrors();
        List<FieldResource> fieldResources = new ArrayList<>();
        for (FieldError fieldError : fieldErrors) {
            FieldResource fieldResource = new FieldResource(fieldError.getObjectName(), fieldError.getField(), fieldError.getCode(), fieldError.getDefaultMessage());
            fieldResources.add(fieldResource);
        }
        InvalidErrorResource ier = new InvalidErrorResource(e.getMessage(), fieldResources);

        return new ResponseEntity<>(ier, HttpStatus.BAD_REQUEST);
    }

    /** For @requestParam validation, which throw ConstraintViolationException
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException e) {
        System.out.println("ConstraintViolationException" + e.getConstraintViolations());
        Set<ConstraintViolation<?>> errMsgs = e.getConstraintViolations();

        List<FieldResource> fieldResources = new ArrayList<>();
        for(ConstraintViolation<?> errMsg: errMsgs){
            FieldResource fieldResource = new FieldResource(null, errMsg.getPropertyPath().toString(), null, errMsg.getMessage());
            fieldResources.add(fieldResource);
        }
        InvalidErrorResource ier = new InvalidErrorResource(e.getMessage(), fieldResources);
        return ResponseEntity.badRequest().body(ier);
    }

}
