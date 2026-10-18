package com.thalesoliveira.cep.api.error;

import com.thalesoliveira.cep.domain.exception.AddressAlreadyExistsException;
import com.thalesoliveira.cep.domain.exception.CepNotFoundException;
import com.thalesoliveira.cep.domain.exception.ExternalServiceException;
import com.thalesoliveira.cep.domain.exception.InvalidAddressException;
import com.thalesoliveira.cep.domain.exception.InvalidCepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Converte exceções em respostas padronizadas (RFC 7807, application/problem+json).
 * Regras: nunca devolver stack trace, nome de classe, SQL ou o valor recebido;
 * erros inesperados recebem um código de referência que liga a resposta ao log do servidor.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({InvalidCepException.class, InvalidAddressException.class})
    public ResponseEntity<ProblemDetail> handleInvalidInput(RuntimeException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage());
    }

    @ExceptionHandler(CepNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(CepNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "CEP não encontrado", ex.getMessage());
    }

    @ExceptionHandler(AddressAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleAlreadyExists(AddressAlreadyExistsException ex) {
        return problem(HttpStatus.CONFLICT, "Endereço já cadastrado", ex.getMessage());
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ProblemDetail> handleExternalService(ExternalServiceException ex) {
        log.warn("Provedores de CEP indisponíveis (último: {})", ex.provider(), ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Serviço indisponível",
                "Serviço de CEP temporariamente indisponível. Tente novamente em instantes.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        // Devolve só o nome do campo e a regra violada, nunca o valor enviado.
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        ResponseEntity<ProblemDetail> response =
                problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Um ou mais campos são inválidos.");
        response.getBody().setProperty("errors", errors);
        return response;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ProblemDetail> handleMalformedRequest(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", "Formato da requisição inválido.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido",
                "Método HTTP não suportado para este recurso.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteúdo não suportado",
                "Envie o corpo como application/json.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", "O recurso solicitado não existe.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade de dados", ex);
        return problem(HttpStatus.CONFLICT, "Conflito", "A operação conflita com dados já existentes.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        String reference = UUID.randomUUID().toString();
        log.error("Erro inesperado [referência={}]", reference, ex);
        ResponseEntity<ProblemDetail> response = problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Informe o código de referência ao suporte.");
        response.getBody().setProperty("reference", reference);
        return response;
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        body.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
