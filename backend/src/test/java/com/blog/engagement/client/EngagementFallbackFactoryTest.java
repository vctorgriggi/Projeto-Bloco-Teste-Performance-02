package com.blog.engagement.client;

import com.blog.engagement.client.dto.NewComment;
import com.blog.engagement.client.dto.NewReaction;
import com.blog.engagement.client.dto.ServiceInfoView;
import com.blog.shared.exception.BusinessRuleException;
import com.blog.shared.exception.ResourceNotFoundException;
import com.blog.shared.exception.ServiceUnavailableException;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// o fallback e chamado pelo circuit breaker para qualquer excecao, inclusive as de
// negocio que o decoder acabou de traduzir. o comportamento correto depende disso, e e
// exatamente o que estes testes fixam: erro com significado passa reto, falha de
// infraestrutura vira indisponibilidade.
class EngagementFallbackFactoryTest {

    private final EngagementFallbackFactory factory = new EngagementFallbackFactory();

    @Test
    void falhaDeConexao_viraServicoIndisponivel() {
        EngagementClient fallback = factory.create(new ConnectException("Connection refused"));

        assertThatThrownBy(() -> fallback.listComments(1L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("indisponivel");
    }

    @Test
    void erroDeNegocioTraduzidoPeloDecoder_passaReto() {
        BusinessRuleException original = new BusinessRuleException("O leitor Carla ja reagiu");
        EngagementClient fallback = factory.create(original);

        assertThatThrownBy(() -> fallback.react(1L, new NewReaction("Carla", "CORACAO")))
                .isSameAs(original);
    }

    @Test
    void recursoInexistente_passaReto() {
        ResourceNotFoundException original = new ResourceNotFoundException("Comentario 9 nao encontrado");
        EngagementClient fallback = factory.create(original);

        assertThatThrownBy(() -> fallback.deleteComment(9L))
                .isSameAs(original);
    }

    // o circuit breaker pode rodar a chamada em outra thread e embrulhar a causa, entao
    // o fallback precisa procurar a excecao de dominio na cadeia inteira, e nao so no
    // primeiro nivel
    @Test
    void erroDeNegocioEmbrulhado_aindaEReconhecido() {
        BusinessRuleException original = new BusinessRuleException("conflito");
        EngagementClient fallback = factory.create(new ExecutionException("embrulhado", original));

        assertThatThrownBy(() -> fallback.addComment(1L, new NewComment("Carla", "oi")))
                .isSameAs(original);
    }

    // o ping e a unica chamada que degrada em vez de falhar: e ele que permite a
    // interface avisar que o engajamento caiu, antes de o leitor tentar comentar
    @Test
    void ping_degradaParaDownEmVezDeLancar() {
        EngagementClient fallback = factory.create(new ConnectException("Connection refused"));

        ServiceInfoView status = fallback.ping();

        assertThat(status.service()).isEqualTo("engagement-service");
        assertThat(status.status()).isEqualTo("DOWN");
    }
}
