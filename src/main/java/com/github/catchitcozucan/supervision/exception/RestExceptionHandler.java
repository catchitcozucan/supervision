/**
 *    Original work by Ola Aronsson 2020
 *    Courtesy of nollettnoll AB &copy; 2012 - 2020
 *
 *    Licensed under the Creative Commons Attribution 4.0 International (the "License")
 *    you may not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *                https://creativecommons.org/licenses/by/4.0/
 *
 *    The software is provided “as is”, without warranty of any kind, express or
 *    implied, including but not limited to the warranties of merchantability,
 *    fitness for a particular purpose and noninfringement. In no event shall the
 *    authors or copyright holders be liable for any claim, damages or other liability,
 *    whether in an action of contract, tort or otherwise, arising from, out of or
 *    in connection with the software or the use or other dealings in the software.
 */
package com.github.catchitcozucan.supervision.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler {

    private static final String JSON_ERROR = "{ \"error\": \"%s\" \"exception\": \"%s\"}";
    private static final String JSON_ERROR_WITH_CODE = "{ \"errorCode\": %d \"error\": \"%s\" \"exception\": \"%s\"}";

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(final Exception ex, final WebRequest request) {
        if (ex instanceof BadRequestException || ex instanceof HttpMessageNotReadableException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(toJsonError(request, ex));
        }
        if (ex instanceof ForbiddenRequestException || ex instanceof AuthorizationDeniedException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(toJsonError(request, ex));
        }
        if (ex instanceof NoResourceFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(toJsonError(request, ex));
        }
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(toJsonError(request, ex));
        }

        // TODO handle all defaults etc.... 404,415,etc,etc
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(toJsonError(request, ex));
    }

    private static String toJsonError(WebRequest request, Throwable ex) {
        if (ex instanceof CatchitSupervisionRuntimeException) {
            CatchitSupervisionRuntimeException e = (CatchitSupervisionRuntimeException) ex;
            if (e.getErrorCode() > CatchitSupervisionRuntimeException.NO_ERROR) {
                return String.format(JSON_ERROR_WITH_CODE, e.getErrorCode(), request.toString(), ex.getClass().getSimpleName() + " - " + ex.getMessage());
            }
        }
        return String.format(JSON_ERROR, request.toString(), ex.getMessage(), ex.getClass().getSimpleName() + " - " + ex.getMessage());
    }
}
