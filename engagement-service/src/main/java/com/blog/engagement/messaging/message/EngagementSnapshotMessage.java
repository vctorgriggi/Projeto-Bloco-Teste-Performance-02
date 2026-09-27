package com.blog.engagement.messaging.message;

import java.time.Instant;

// evento publicado por este servico sempre que o engajamento de um post muda.
//
// o padrao aqui e a transferencia de estado pelo evento (event-carried state transfer):
// a routing key diz o que aconteceu (comment.added, reaction.removed, ...), e o corpo
// carrega o estado resultante -- os totais do post DEPOIS da mudanca, e nao o delta.
//
// mandar o total, e nao "+1", e o que torna o consumo idempotente e tolerante a perda:
// aplicar duas vezes a mesma mensagem da o mesmo resultado, e se uma mensagem se perder
// a seguinte ja traz o numero certo. um "+1" duplicado contaria errado para sempre.
//
// o occurredAt permite ao consumidor descartar uma mensagem mais velha que chegue depois
// de uma mais nova.
public record EngagementSnapshotMessage(
        Long postId,
        long comments,
        long reactions,
        String change,
        Instant occurredAt
) {
}
