package com.prairiegrade.janki.exception;

import com.prairiegrade.janki.dto.response.ErrorResponse;
import com.prairiegrade.janki.dto.response.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for both REST API (JSON) and Web UI (HTML) errors.
 *
 * <p>Uses Accept header to determine response format:</p>
 * <ul>
 *   <li>application/json → JSON error response</li>
 *   <li>text/html → HTML error page</li>
 * </ul>
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles DeckNotFoundException.
     * Returns JSON for API requests, HTML error page for web requests.
     */
    @ExceptionHandler(DeckNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Object handleDeckNotFound(DeckNotFoundException ex, HttpServletRequest request) {
        log.warn("Deck not found: {}", ex.getMessage());

        if (isJsonRequest(request)) {
            ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        // Return HTML error page for web requests
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    /**
     * Handles CardNotFoundException.
     * Returns JSON for API requests, HTML error page for web requests.
     */
    @ExceptionHandler(CardNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Object handleCardNotFound(CardNotFoundException ex, HttpServletRequest request) {
        log.warn("Card not found: {}", ex.getMessage());

        if (isJsonRequest(request)) {
            ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        // Return HTML error page for web requests
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    /**
     * Handles validation errors from @Valid annotations.
     * Returns detailed field-level error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.warn("Validation failed: {} errors", ex.getBindingResult().getErrorCount());

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                    (existing, replacement) -> existing // Keep first error if multiple for same field
                ));

        ValidationErrorResponse response = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles generic RuntimeException.
     * Logs full stack trace and returns generic error message to user.
     */
    @ExceptionHandler(RuntimeException.class)
    public Object handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.error("Unexpected runtime exception", ex);

        if (isJsonRequest(request)) {
            ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        // Return HTML error page for web requests
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", "An unexpected error occurred");
        return mav;
    }

    /**
     * Handles all other exceptions.
     * Logs full stack trace and returns generic error message to user.
     */
    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);

        if (isJsonRequest(request)) {
            ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        // Return HTML error page for web requests
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", "An unexpected error occurred");
        return mav;
    }

    /**
     * Determines if the request expects a JSON response.
     * Checks Accept header for application/json.
     */
    private boolean isJsonRequest(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        return acceptHeader != null && acceptHeader.contains("application/json");
    }
}
