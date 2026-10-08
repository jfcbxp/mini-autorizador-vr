package com.br.vr.miniautorizador.exceptions.handler;

import com.br.vr.miniautorizador.exceptions.AuthorizationException;
import com.br.vr.miniautorizador.exceptions.CardAlreadyExistsException;
import com.br.vr.miniautorizador.exceptions.CardNotFoundException;
import com.br.vr.miniautorizador.records.responses.CreateCardResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(CardAlreadyExistsException.class)
    public ResponseEntity<CreateCardResponse> handleCardAlreadyExists(CardAlreadyExistsException ex) {
        log.info("RestExceptionHandler.handleCardAlreadyExists - card already exists");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getCard());
    }

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<Void> handleCardNotFound(CardNotFoundException ex) {
        log.info("RestExceptionHandler.handleCardNotFound - {}", ex.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<String> handleAuthorization(AuthorizationException ex) {
        log.info("RestExceptionHandler.handleAuthorization - error: {}", ex.getError());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.TEXT_PLAIN)
                .body(ex.getError().name());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Void> handleValidation(MethodArgumentNotValidException ex) {
        log.info("RestExceptionHandler.handleValidation - field errors: {}", ex.getBindingResult().getFieldErrors().size());
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Void> handleNotReadable(HttpMessageNotReadableException ex) {
        log.info("RestExceptionHandler.handleNotReadable - cause: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleAll(Exception ex) {
        log.error("RestExceptionHandler.handleAll - Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError().build();
    }
}
