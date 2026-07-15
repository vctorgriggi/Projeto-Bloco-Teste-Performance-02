package com.blog.shared.web;

import com.blog.shared.exception.BusinessRuleException;
import com.blog.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

// testa o mapeamento de excecoes para status http direto no handler, sem subir
// contexto. cobre em especial as violacoes de integridade/concorrencia que a
// segunda entrega passou a tratar como 409.
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts/1");

    @Test
    void recursoInexistente_viraNotFound() {
        ResponseEntity<ApiError> response =
                handler.handleNotFound(new ResourceNotFoundException("Post 1 nao encontrado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void regraDeNegocio_viraConflict() {
        ResponseEntity<ApiError> response =
                handler.handleBusinessRule(new BusinessRuleException("Email ja cadastrado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
    }

    @Test
    void travamentoOtimista_viraConflict() {
        ResponseEntity<ApiError> response =
                handler.handleOptimisticLock(new OptimisticLockingFailureException("versao defasada"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
    }

    @Test
    void violacaoDeIntegridade_viraConflict() {
        ResponseEntity<ApiError> response =
                handler.handleDataIntegrity(new DataIntegrityViolationException("unique index violado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
    }
}
