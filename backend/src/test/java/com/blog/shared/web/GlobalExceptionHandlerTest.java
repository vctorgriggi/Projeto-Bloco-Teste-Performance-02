package com.blog.shared.web;

import com.blog.shared.exception.BusinessRuleException;
import com.blog.shared.exception.InvalidRequestException;
import com.blog.shared.exception.ResourceNotFoundException;
import com.blog.shared.exception.ServiceUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

// testa o mapeamento de excecoes para status http direto no handler, sem subir
// contexto. cobre as violacoes de integridade/concorrencia que a segunda entrega
// passou a tratar como 409 e, agora, os dois status que a integracao distribuida
// trouxe: 503 quando o outro servico nao responde e 400 quando ele recusa o pedido.
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

    // 503, e nao 500: a requisicao estava correta, o sistema e que esta com uma peca
    // faltando. e o status que diz ao cliente "tente de novo mais tarde".
    @Test
    void engajamentoIndisponivel_viraServiceUnavailable() {
        ResponseEntity<ApiError> response = handler.handleServicoIndisponivel(
                new ServiceUnavailableException("O servico de engajamento esta indisponivel.", null), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().status()).isEqualTo(503);
        assertThat(response.getBody().message()).contains("indisponivel");
    }

    // recusa de validacao de outro servico continua sendo erro do cliente, nao do servidor
    @Test
    void recusaDeValidacaoDeOutroServico_viraBadRequest() {
        ResponseEntity<ApiError> response = handler.handleRequisicaoInvalida(
                new InvalidRequestException("Valor invalido para o parametro type"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(400);
    }

    @Test
    void violacaoDeIntegridade_viraConflict() {
        ResponseEntity<ApiError> response =
                handler.handleDataIntegrity(new DataIntegrityViolationException("unique index violado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().status()).isEqualTo(409);
    }
}
