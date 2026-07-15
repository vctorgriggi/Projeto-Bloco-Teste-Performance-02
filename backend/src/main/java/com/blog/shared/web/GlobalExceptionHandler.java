package com.blog.shared.web;

import com.blog.shared.exception.BusinessRuleException;
import com.blog.shared.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// centraliza a traducao de excecoes em respostas http. mantem os controllers
// livres de try/catch e garante um formato de erro consistente em toda a api.
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

    // travamento otimista: duas gravacoes concorrentes sobre o mesmo registro. e
    // um conflito recuperavel pelo cliente (basta recarregar e tentar de novo),
    // entao responde 409 em vez de deixar virar 500.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest req) {
        ApiError body = ApiError.of(HttpStatus.CONFLICT.value(), "Conflict",
                "O registro foi alterado por outra operacao. Recarregue e tente novamente.", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // violacao de restricao do banco (ex: email unico numa corrida entre dois
    // cadastros). tambem e um conflito de dados, entao 409.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        ApiError body = ApiError.of(HttpStatus.CONFLICT.value(), "Conflict",
                "A operacao viola uma restricao de integridade dos dados.", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // dispara quando um dto anotado com @Valid nao passa nas validacoes
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
}
