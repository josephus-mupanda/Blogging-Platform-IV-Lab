package com.example.Blogging_Platform_III.controller.rest;

import com.example.Blogging_Platform_III.exception.BlogException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** One place that turns every REST error into the status, message and data envelope. */
@RestControllerAdvice
public class ApiErrors {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrors.class);

    @ExceptionHandler(BlogException.class)
    public ResponseEntity<ApiResponse<Void>> businessError(BlogException exception) {
        HttpStatus status;
        if (exception.getCode() == BlogException.Code.NOT_FOUND) {
            status = HttpStatus.NOT_FOUND;
        } else if (exception.getCode() == BlogException.Code.CONFLICT) {
            status = HttpStatus.CONFLICT;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(ApiResponse.error(exception.getMessage(), null));
    }

    /**
     * Two requests changed the same post at the same time. PostService.update runs with
     * REPEATABLE_READ isolation, so PostgreSQL refuses the second change instead of letting it
     * silently overwrite the first. The transaction is rolled back and the client can retry.
     */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiResponse<Void>> concurrentChange(ConcurrencyFailureException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("The record was changed by another request. Load it again and retry.", null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> invalidBody(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
            fields.putIfAbsent("request", error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Check the request fields.", fields));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> wrongType(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("The value of '" + exception.getName() + "' is not valid.", null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> unreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error("The request body could not be read.", null));
    }

    /**
     * An address that matches no endpoint, such as the /favicon.ico a browser asks for on its own.
     * This is a normal 404, not a server fault, so it is logged quietly and without a stack trace.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> unknownAddress(NoResourceFoundException exception) {
        LOG.debug("No endpoint for {}", exception.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("This address does not exist on the server.", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception exception) {
        LOG.error("REST request failed", exception);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("The request could not be completed.", null));
    }
}
