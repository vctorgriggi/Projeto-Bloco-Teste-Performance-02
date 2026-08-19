package com.blog.engagement.client;

import com.blog.shared.exception.BusinessRuleException;
import com.blog.shared.exception.InvalidRequestException;
import com.blog.shared.exception.ResourceNotFoundException;
import com.blog.shared.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// o decoder e o que faz um erro do outro servico continuar significando a mesma coisa
// depois de atravessar a rede. sem ele, todo erro do engajamento chegaria como
// FeignException e sairia como 500 para o front, sem distincao entre "esse comentario
// nao existe" e "o servico caiu".
class EngagementErrorDecoderTest {

    private final EngagementErrorDecoder decoder = new EngagementErrorDecoder(new ObjectMapper());

    private Response respostaComStatus(int status, String corpo) {
        Request request = Request.create(Request.HttpMethod.GET, "/api/posts/1/comments",
                Map.of(), null, new RequestTemplate());
        return Response.builder()
                .status(status)
                .reason("erro simulado")
                .request(request)
                .body(corpo, StandardCharsets.UTF_8)
                .build();
    }

    private String envelopeDeErro(int status, String mensagem) {
        return """
                {"timestamp":"2026-01-01T00:00:00Z","status":%d,"error":"erro",
                 "message":"%s","path":"/api/posts/1/comments","fieldErrors":null}
                """.formatted(status, mensagem);
    }

    @Test
    void status404_viraRecursoNaoEncontradoComAMensagemOriginal() {
        Exception traduzida = decoder.decode("EngagementClient#listComments(Long)",
                respostaComStatus(404, envelopeDeErro(404, "Comentario 9 nao encontrado")));

        assertThat(traduzida)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Comentario 9 nao encontrado");
    }

    @Test
    void status409_viraRegraDeNegocio() {
        Exception traduzida = decoder.decode("EngagementClient#react(Long,NewReaction)",
                respostaComStatus(409, envelopeDeErro(409, "O leitor Carla ja reagiu com CORACAO neste post")));

        assertThat(traduzida)
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Carla");
    }

    @Test
    void status400_viraRequisicaoInvalida() {
        Exception traduzida = decoder.decode("EngagementClient#undoReaction(Long,String,String)",
                respostaComStatus(400, envelopeDeErro(400, "Valor invalido para o parametro type")));

        assertThat(traduzida)
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("type");
    }

    // qualquer outro status e falha da integracao, nao do pedido do cliente
    @Test
    void status500_viraServicoIndisponivel() {
        Exception traduzida = decoder.decode("EngagementClient#listComments(Long)",
                respostaComStatus(500, envelopeDeErro(500, "boom")));

        assertThat(traduzida).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void status503_viraServicoIndisponivel() {
        Exception traduzida = decoder.decode("EngagementClient#listComments(Long)",
                respostaComStatus(503, envelopeDeErro(503, "indisponivel")));

        assertThat(traduzida).isInstanceOf(ServiceUnavailableException.class);
    }

    // corpo que nao segue o envelope de erro nao pode derrubar o decoder: ele cai numa
    // mensagem padrao e mantem o status traduzido
    @Test
    void corpoQueNaoEOEnvelopeEsperado_naoQuebraATraducao() {
        Exception traduzida = decoder.decode("EngagementClient#listComments(Long)",
                respostaComStatus(404, "<html>pagina de erro do servidor</html>"));

        assertThat(traduzida)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Recurso de engajamento nao encontrado");
    }
}
