package com.example.Blogging_Platform_IV.controller.rest;

import com.example.Blogging_Platform_IV.exception.BlogException;
import com.example.Blogging_Platform_IV.service.SecurityEvents;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionException;
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

    private final SecurityEvents events;

    public ApiErrors(SecurityEvents events) {
        this.events = events;
    }

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

    // @PreAuthorize refused the call: logged in, but the role is not allowed. Without this it would be a 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden(AccessDeniedException exception, HttpServletRequest request) {
        events.accessDenied(SecurityContextHolder.getContext().getAuthentication().getName(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Not allowed: wrong role, or this belongs to another user.", null));
    }

    /** Wrong password or unknown username at /auth/login. The message does not say which one was wrong. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> badCredentials(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid username or password.", null));
    }

    // ----- Errors that make a transaction fail. In every case Spring has already rolled the -----
    // ----- transaction back before we get here, so the database is unchanged.               -----

    // Two requests saved the same post at the same time (@Version). The second one is rolled back.
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiResponse<Void>> concurrentChange(ConcurrencyFailureException exception) {
        LOG.warn("Concurrent update refused, transaction rolled back: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(
                "The record was changed by another request. Your change was rolled back. Load it again and retry.",
                null));
    }

    /** A unique column (post title, tag name, username, email) already has this value. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> duplicateValue(DataIntegrityViolationException exception) {
        LOG.warn("Constraint violated, transaction rolled back: {}", exception.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(
                "A record already uses this value. The transaction was rolled back, nothing was saved.", null));
    }

    // The transaction could not start or commit, for example because the database is down.
    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<ApiResponse<Void>> transactionFailed(TransactionException exception) {
        LOG.error("Transaction failed and was rolled back", exception);
        return ResponseEntity.internalServerError().body(ApiResponse.error(
                "The transaction failed and was rolled back. Nothing was saved.", null));
    }

    /** Any other database error (the most general Spring data error). */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> databaseError(DataAccessException exception) {
        LOG.error("Database error, transaction rolled back", exception);
        return ResponseEntity.internalServerError().body(ApiResponse.error(
                "A database error happened. The transaction was rolled back, nothing was saved.", null));
    }

    /** The client asked to sort by a field that does not exist, for example ?sort=color. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiResponse<Void>> unknownSortField(PropertyReferenceException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Cannot sort by '" + exception.getPropertyName() + "'.", null));
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

    // An address that matches no endpoint (like /favicon.ico). A normal 404, so it is not logged as an error.
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
