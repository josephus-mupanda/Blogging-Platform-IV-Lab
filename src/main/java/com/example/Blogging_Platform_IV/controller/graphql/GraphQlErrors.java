package com.example.Blogging_Platform_IV.controller.graphql;

import com.example.Blogging_Platform_IV.exception.BlogException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(annotations = Controller.class)
public class GraphQlErrors {
    @GraphQlExceptionHandler
    GraphQLError business(BlogException exception, DataFetchingEnvironment environment) {
        return error(environment, exception.getCode().name(), exception.getMessage());
    }

    /** @PreAuthorize refused the call: the user's role is not allowed to run this query or mutation. */
    @GraphQlExceptionHandler
    GraphQLError forbidden(AccessDeniedException exception, DataFetchingEnvironment environment) {
        return error(environment, "FORBIDDEN", "Your role is not allowed to do this.");
    }

    // Errors that make a transaction fail: Spring has already rolled the transaction back.

    @GraphQlExceptionHandler
    GraphQLError concurrentChange(ConcurrencyFailureException exception, DataFetchingEnvironment environment) {
        return error(environment, "CONFLICT",
                "The record was changed by another request. Your change was rolled back. Load it again and retry.");
    }

    @GraphQlExceptionHandler
    GraphQLError duplicateValue(DataIntegrityViolationException exception, DataFetchingEnvironment environment) {
        return error(environment, "CONFLICT",
                "A record already uses this value. The transaction was rolled back, nothing was saved.");
    }

    @GraphQlExceptionHandler
    GraphQLError transactionFailed(TransactionException exception, DataFetchingEnvironment environment) {
        return error(environment, "TRANSACTION_FAILED", "The transaction failed and was rolled back. Nothing was saved.");
    }

    @GraphQlExceptionHandler
    GraphQLError databaseError(DataAccessException exception, DataFetchingEnvironment environment) {
        return error(environment, "DATABASE_ERROR",
                "A database error happened. The transaction was rolled back, nothing was saved.");
    }

    @GraphQlExceptionHandler
    GraphQLError invalid(ConstraintViolationException exception, DataFetchingEnvironment environment) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .sorted().collect(Collectors.joining(", "));
        return error(environment, "BAD_REQUEST", message);
    }

    @GraphQlExceptionHandler
    GraphQLError invalidArguments(BindException exception, DataFetchingEnvironment environment) {
        return error(environment, "BAD_REQUEST", "Check the argument types and values.");
    }

    private GraphQLError error(DataFetchingEnvironment environment, String code, String message) {
        return GraphqlErrorBuilder.newError(environment).message(message).extensions(Map.of("code", code)).build();
    }
}
