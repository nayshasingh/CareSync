package com.cts.healthcare_appointment_system.error;
 
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
 
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cts.healthcare_appointment_system.dto.ApiErrorResponseDTO;
 
import jakarta.servlet.http.HttpServletRequest;
 
@RestControllerAdvice
public class ApiExceptionHandler{
 
    // Handle Api exceptions
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleApiException(ApiException ex, HttpServletRequest req){
        return buildResponse("ApiException", ex.getMessage(), ex.getErrorCode(), req, null);
    }
 
    // Handle field validaiton exceptions
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest req){
        Map<String, String> fieldErrors = new HashMap<>();
 
        ex.getBindingResult().getFieldErrors().forEach(e -> {
            fieldErrors.put(e.getField(), e.getDefaultMessage());
        });
 
        return buildResponse("Field Validation Error", "Request validation failed", HttpStatus.BAD_REQUEST, req, fieldErrors);
    }
 
    // Handle Database integrity violation exceptions
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleDataIntegrityViolationException(DataIntegrityViolationException ex, HttpServletRequest req){
        String message = ex.getRootCause() != null ? ex.getRootCause().getMessage() : "Database constraint violation";
        return buildResponse("Database Constraint Violation", message, HttpStatus.BAD_REQUEST, req, null);
    }
 
    // Handle HTTP method not allowed exception
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest req){
        String message = "This HTTP method is not supported for this endpoint";
        if (ex.getSupportedMethods() != null && ex.getSupportedMethods().length > 0) {
            message = message + ". Supported methods: " + String.join(", ", ex.getSupportedMethods());
        }

        return buildResponse("Method Not Allowed", message, HttpStatus.METHOD_NOT_ALLOWED, req, null);
    }
 
    // Handle message not readable exception
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest req){
        return buildResponse("Invalid Request Body", ex.getMessage(), HttpStatus.BAD_REQUEST, req, null);
    }

    // Handle Bad credentials exception
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleBackCredentials(BadCredentialsException ex, HttpServletRequest req){
        return buildResponse(ex.getClass().getSimpleName(), "Email or password is incorrect", HttpStatus.BAD_REQUEST, req, null);
    }

    // Handle all other unhandled exceptions (generic exception handler)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDTO> handleGeneralException(Exception ex, HttpServletRequest req){
        return buildResponse(ex.getClass().getSimpleName(), ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, req, null);
    } 

    private ResponseEntity<ApiErrorResponseDTO> buildResponse(String error, String message, HttpStatus status, HttpServletRequest req, Map<String, String> fieldErrors) {
        ApiErrorResponseDTO response = new ApiErrorResponseDTO(
                LocalDateTime.now(),
                error,
                message,
                status.value(),
                req != null ? req.getRequestURI() : null,
                req != null ? req.getMethod() : null,
                fieldErrors);

        return ResponseEntity.status(status).body(response);
    }
}