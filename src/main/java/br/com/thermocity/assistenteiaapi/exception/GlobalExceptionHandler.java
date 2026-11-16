package br.com.thermocity.assistenteiaapi.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AssistenteException.class)
    public ResponseEntity<ProblemDetail> handleAssistente(AssistenteException ex) {
        if (ex.getCause() != null) {
            log.error(ex.getMessage(), ex.getCause());
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(problema);
    }

    // Erros do @Valid viram RequisicaoInvalidaException, com a mensagem do primeiro campo inválido
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Requisição inválida");
        return handleAssistente(new RequisicaoInvalidaException(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleBadInput(HttpMessageNotReadableException ex) {
        return handleAssistente(new RequisicaoInvalidaException("Requisição inválida"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return handleAssistente(new RequisicaoInvalidaException("Parâmetro inválido: " + ex.getName()));
    }
}