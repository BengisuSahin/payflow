package com.payflow.orders.paymentorder.infrastructure.web;

import com.payflow.orders.paymentorder.application.IdempotencyKeyReusedException;
import com.payflow.orders.paymentorder.application.PaymentOrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Returns every error as an RFC 9457 ProblemDetail. The base class already
 * covers Spring's own errors, such as validation failures (400).
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(PaymentOrderNotFoundException.class)
    public ProblemDetail handleNotFound(PaymentOrderNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(IdempotencyKeyReusedException.class)
    public ProblemDetail handleIdempotencyKeyReused(IdempotencyKeyReusedException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage()
        );
    }
}
