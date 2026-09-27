package com.blog.shared.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

// uma mensagem que precisa sair para o broker, gravada no mesmo banco e na mesma
// transacao da mudanca que a originou (padrao transactional outbox).
//
// o problema que isto resolve e o da escrita dupla. apagar o post e publicar
// "post.deleted" sao duas escritas em dois sistemas diferentes, e nao existe transacao
// que abranja o banco e o broker ao mesmo tempo. qualquer ordem falha em algum cenario:
// publicar antes do commit anuncia uma exclusao que pode voltar atras; publicar depois
// perde o evento se o processo cair entre um e outro, ou se o broker estiver fora. o
// outbox troca as duas escritas por uma so -- a linha desta tabela entra ou sai junto com
// a exclusao do post -- e deixa a publicacao para um relay que tenta ate conseguir.
//
// o id e o message id da mensagem no broker, o que permite rastrear uma entrega de ponta
// a ponta e, se preciso, reconhecer uma repeticao do outro lado.
@Entity
@Table(name = "outbox_messages", indexes = {
        @Index(name = "idx_outbox_pending", columnList = "published_at, created_at")
})
@Getter
@NoArgsConstructor(access = PROTECTED)
public class OutboxMessage {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String exchange;

    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    // o nome logico da mensagem (post.deleted), que vai no campo type do amqp
    @Column(nullable = false)
    private String type;

    // o corpo ja serializado: o relay publica exatamente estes bytes, sem reconverter
    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // nulo enquanto pendente. e o que o relay procura
    @Column(name = "published_at")
    private Instant publishedAt;

    private int attempts;

    @Column(name = "last_error", length = 500)
    private String lastError;

    public OutboxMessage(String exchange, String routingKey, String type, String payload) {
        this.id = UUID.randomUUID().toString();
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.type = type;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public boolean isPending() {
        return publishedAt == null;
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
        this.attempts++;
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.attempts++;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 500));
    }
}
