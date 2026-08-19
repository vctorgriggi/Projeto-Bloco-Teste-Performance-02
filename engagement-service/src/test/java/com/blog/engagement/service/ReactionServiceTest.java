package com.blog.engagement.service;

import com.blog.engagement.domain.ReactionType;
import com.blog.engagement.repository.ReactionRepository;
import com.blog.engagement.shared.BusinessRuleException;
import com.blog.engagement.shared.ResourceNotFoundException;
import com.blog.engagement.web.dto.ReactionRequest;
import com.blog.engagement.web.dto.ReactionSummaryResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// as regras da capacidade nova, exercitadas pelo service real sobre o banco do
// microsservico. como no resto do projeto, a limpeza fica no @AfterEach em vez de
// rollback, para cada chamada de service abrir e fechar a sua propria transacao.
@SpringBootTest
@ActiveProfiles("test")
class ReactionServiceTest {

    @Autowired
    private ReactionService reactionService;

    @Autowired
    private ReactionRepository reactionRepository;

    @AfterEach
    void limparBanco() {
        reactionRepository.deleteAll();
    }

    @Test
    void resumoDePostSemReacao_vemZeradoComTodosOsTipos() {
        ReactionSummaryResponse resumo = reactionService.summaryFor(42L, "Carla");

        assertThat(resumo.postId()).isEqualTo(42L);
        assertThat(resumo.total()).isZero();
        assertThat(resumo.mine()).isEmpty();
        // todos os tipos aparecem, para a interface montar a barra completa
        assertThat(resumo.counts()).containsOnlyKeys(ReactionType.values());
        assertThat(resumo.counts().values()).allMatch(total -> total == 0L);
    }

    @Test
    void react_somaNoTotalEMarcaComoReacaoDoLeitor() {
        ReactionSummaryResponse resumo =
                reactionService.react(1L, new ReactionRequest("Carla", ReactionType.CORACAO));

        assertThat(resumo.total()).isEqualTo(1);
        assertThat(resumo.counts().get(ReactionType.CORACAO)).isEqualTo(1);
        assertThat(resumo.mine()).containsExactly(ReactionType.CORACAO);
    }

    @Test
    void reacoesDeLeitoresDiferentes_somamNoMesmoTipo() {
        reactionService.react(1L, new ReactionRequest("Carla", ReactionType.CORACAO));
        ReactionSummaryResponse resumo =
                reactionService.react(1L, new ReactionRequest("Diego", ReactionType.CORACAO));

        assertThat(resumo.counts().get(ReactionType.CORACAO)).isEqualTo(2);
        // o resumo e sempre do ponto de vista de quem chamou
        assertThat(resumo.mine()).containsExactly(ReactionType.CORACAO);
    }

    @Test
    void mesmoLeitorRepetindoOMesmoTipo_violaRegraDeNegocio() {
        reactionService.react(1L, new ReactionRequest("Carla", ReactionType.CORACAO));

        assertThatThrownBy(() -> reactionService.react(1L, new ReactionRequest("Carla", ReactionType.CORACAO)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Carla");
    }

    @Test
    void undoReaction_removeSoAReacaoDaquelePar() {
        reactionService.react(1L, new ReactionRequest("Carla", ReactionType.CORACAO));
        reactionService.react(1L, new ReactionRequest("Carla", ReactionType.IDEIA));
        reactionService.react(1L, new ReactionRequest("Diego", ReactionType.CORACAO));

        ReactionSummaryResponse resumo = reactionService.undoReaction(1L, ReactionType.CORACAO, "Carla");

        assertThat(resumo.total()).isEqualTo(2);
        assertThat(resumo.counts().get(ReactionType.CORACAO)).isEqualTo(1); // sobrou a do Diego
        assertThat(resumo.mine()).containsExactly(ReactionType.IDEIA);
    }

    @Test
    void undoDeReacaoQueNaoExiste_lancaNotFound() {
        assertThatThrownBy(() -> reactionService.undoReaction(1L, ReactionType.CAFE, "Carla"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reacoesDeOutroPost_naoEntramNoResumo() {
        reactionService.react(1L, new ReactionRequest("Carla", ReactionType.CORACAO));
        reactionService.react(2L, new ReactionRequest("Carla", ReactionType.CORACAO));

        assertThat(reactionService.summaryFor(1L, "Carla").total()).isEqualTo(1);
    }

    @Test
    void resumoSemLeitorInformado_trazTotaisSemMarcarNada() {
        reactionService.react(1L, new ReactionRequest("Carla", ReactionType.CORACAO));

        ReactionSummaryResponse resumo = reactionService.summaryFor(1L, null);

        assertThat(resumo.total()).isEqualTo(1);
        assertThat(resumo.mine()).isEmpty();
    }
}
