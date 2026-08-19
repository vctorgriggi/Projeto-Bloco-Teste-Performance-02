package com.blog.engagement.shared;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// mesma estrategia do monolito: a traducao de excecao para status http fica em um
// lugar so. num sistema distribuido isso pesa mais do que num monolito, porque o
// status que sai daqui e o que o chamador usa para decidir se o erro foi culpa da
// requisicao (4xx, repassa) ou do servico (5xx, trata como indisponibilidade).
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        ApiError body = ApiError.of(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest req) {
        ApiError body = ApiError.of(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // a restricao uk_reactions_post_reader_type estourando numa corrida entre dois
    // cliques, ou o travamento otimista pegando duas gravacoes concorrentes: nos
    // dois casos e conflito de dados, nao falha do servico.
    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiError> handleConflitoDeDados(Exception ex, HttpServletRequest req) {
        ApiError body = ApiError.of(HttpStatus.CONFLICT.value(), "Conflict",
                "A operacao conflita com o estado atual dos dados. Recarregue e tente novamente.",
                req.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        ApiError body = ApiError.ofValidation(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Falha de validacao nos campos enviados", req.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // tipo de reacao invalido no corpo (ex: {"type":"APLAUSO"}). o jackson recusa o
    // valor antes de qualquer validacao, e sem este handler o spring responderia 400 com
    // um corpo que nao e o nosso envelope -- entao quem chama nao acha o campo "message"
    // e perde a explicacao. como o monolito repassa justamente essa mensagem ao leitor, a
    // consistencia do envelope em TODOS os erros e o que sustenta a integracao.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleCorpoIlegivel(HttpMessageNotReadableException ex,
                                                       HttpServletRequest req) {
        ApiError body = ApiError.of(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                explicar(ex), req.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    // quando da, diz qual campo recusou o valor e, para um enum, quais valores ele
    // aceita. e a diferenca entre "corpo invalido" e uma mensagem que resolve o problema
    // de quem esta chamando.
    private String explicar(HttpMessageNotReadableException ex) {
        if (!(ex.getCause() instanceof InvalidFormatException falha)) {
            return "Corpo da requisicao invalido";
        }

        String campo = falha.getPath().isEmpty()
                ? null
                : falha.getPath().get(falha.getPath().size() - 1).getFieldName();
        String mensagem = campo == null
                ? "Valor invalido no corpo da requisicao"
                : "Valor invalido para o campo " + campo;

        Class<?> tipoEsperado = falha.getTargetType();
        if (tipoEsperado != null && tipoEsperado.isEnum()) {
            mensagem += ". Valores aceitos: " + Arrays.stream(tipoEsperado.getEnumConstants())
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
        }
        return mensagem;
    }

    // tipo de reacao invalido na url (ex: /reactions/APLAUSO). sem este handler o
    // spring devolveria 500 para o que e, na verdade, uma requisicao malformada.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTipoInvalido(MethodArgumentTypeMismatchException ex,
                                                      HttpServletRequest req) {
        ApiError body = ApiError.of(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Valor invalido para o parametro " + ex.getName(), req.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }
}
