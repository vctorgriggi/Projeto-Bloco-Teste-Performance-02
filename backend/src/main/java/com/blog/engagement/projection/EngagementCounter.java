package com.blog.engagement.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

import static lombok.AccessLevel.PROTECTED;

// a copia local dos totais de engajamento de um post: quantos comentarios, quantas
// reacoes, e de quando e essa informacao.
//
// e um modelo de leitura, e nao um aggregate do dominio. o dono do dado continua sendo
// o engagement-service; esta tabela e uma projecao dos eventos que ele publica, mantida
// aqui para a estante mostrar os numeros de todos os posts sem fazer uma chamada de rede
// por post -- e sem depender de o engajamento estar de pe. o preco e a defasagem: o
// numero aqui pode estar alguns instantes atras do de la.
//
// por ser derivado, nao e auditado (o historico de verdade esta nos eventos e no banco
// de origem) e pode ser reconstruido a qualquer momento pedindo ao engajamento que
// republique o estado. a chave e o proprio postId: um contador por post.
@Entity
@Table(name = "engagement_counters")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class EngagementCounter {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(nullable = false)
    private long comments;

    @Column(nullable = false)
    private long reactions;

    // o instante em que a mudanca aconteceu LA, e nao quando chegou aqui. e o que permite
    // reconhecer uma mensagem velha que chegou depois de uma nova
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_change", length = 40)
    private String lastChange;

    @Version
    private Long version;

    public EngagementCounter(Long postId, long comments, long reactions, String change, Instant occurredAt) {
        this.postId = postId;
        apply(comments, reactions, change, occurredAt);
    }

    // aplica o estado se ele for pelo menos tao recente quanto o atual; devolve se aplicou.
    // igual tambem aplica: a mesma mensagem entregue duas vezes da o mesmo resultado.
    public boolean applyIfNotOlder(long comments, long reactions, String change, Instant occurredAt) {
        if (occurredAt.isBefore(updatedAt)) {
            return false;
        }
        apply(comments, reactions, change, occurredAt);
        return true;
    }

    private void apply(long comments, long reactions, String change, Instant occurredAt) {
        this.comments = comments;
        this.reactions = reactions;
        this.lastChange = change;
        this.updatedAt = occurredAt;
    }
}
