package com.blog.engagement.messaging;

import com.blog.engagement.projection.EngagementCounter;
import com.blog.engagement.projection.EngagementCounterRepository;
import com.blog.shared.exception.InvalidMessageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// o consumidor das mudancas de engajamento, chamado como metodo. o que se testa e a regra
// de aplicacao da copia local -- "o mais recente vence" --, que e o que torna seguro
// consumir sem saber se uma mensagem chegou repetida, atrasada ou se outra se perdeu.
@SpringBootTest
@ActiveProfiles("test")
class EngagementSnapshotListenerTest {

    private static final Instant T0 = Instant.parse("2026-05-01T10:00:00Z");

    @Autowired
    private EngagementSnapshotListener listener;

    @Autowired
    private EngagementCounterRepository repository;

    @AfterEach
    void limparBanco() {
        repository.deleteAll();
    }

    private void chega(long postId, long comentarios, long reacoes, String mudanca, Instant quando) {
        listener.onEngagementChanged(new EngagementSnapshotMessage(postId, comentarios, reacoes, mudanca, quando));
    }

    private EngagementCounter contador(long postId) {
        return repository.findById(postId).orElseThrow();
    }

    @Test
    void primeiraMudancaDeUmPost_criaOContador() {
        chega(1, 2, 3, "comment.added", T0);

        assertThat(contador(1).getComments()).isEqualTo(2);
        assertThat(contador(1).getReactions()).isEqualTo(3);
        assertThat(contador(1).getUpdatedAt()).isEqualTo(T0);
    }

    // o corpo traz o total, e nao o delta: aplicar duas vezes a mesma mensagem da o mesmo
    // numero. com "+1" a mensagem repetida contaria em dobro para sempre
    @Test
    void mesmaMensagemDuasVezes_naoContaEmDobro() {
        chega(1, 1, 0, "comment.added", T0);
        chega(1, 1, 0, "comment.added", T0);

        assertThat(contador(1).getComments()).isEqualTo(1);
    }

    // a mensagem velha que chega depois de uma nova e descartada
    @Test
    void mensagemAtrasada_naoSobrescreveUmaMaisNova() {
        chega(1, 3, 0, "comment.added", T0.plusSeconds(10));
        chega(1, 2, 0, "comment.added", T0);

        assertThat(contador(1).getComments()).isEqualTo(3);
    }

    // uma mensagem perdida nao deixa o numero errado para sempre: a seguinte traz o total
    @Test
    void mensagemPerdida_eCorrigidaPelaSeguinte() {
        chega(1, 1, 0, "comment.added", T0);
        // a do segundo comentario se perdeu no caminho
        chega(1, 3, 0, "comment.added", T0.plusSeconds(20));

        assertThat(contador(1).getComments()).isEqualTo(3);
    }

    // o post apagado vira uma linha zerada, e nao some: e ela que impede uma mudanca
    // anterior a exclusao, entregue atrasada, de ressuscitar os numeros
    @Test
    void postApagado_zeraOContador_eMudancaAnteriorNaoRessuscita() {
        chega(1, 2, 1, "comment.added", T0);
        chega(1, 0, 0, "engagement.purged", T0.plusSeconds(30));
        chega(1, 3, 1, "comment.added", T0.plusSeconds(5));

        assertThat(contador(1).getComments()).isZero();
        assertThat(contador(1).getReactions()).isZero();
    }

    @Test
    void mensagemSemPost_eInvalida() {
        assertThatThrownBy(() -> listener.onEngagementChanged(
                new EngagementSnapshotMessage(null, 1, 1, "comment.added", T0)))
                .isInstanceOf(InvalidMessageException.class);
    }
}
