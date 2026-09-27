package com.blog.engagement.service;

import java.time.Instant;

// evento interno: o engajamento de um post mudou, e estes sao os totais depois da
// mudanca. e entregue dentro do processo pelo ApplicationEventPublisher do spring, e so
// vira mensagem no broker depois de a transacao que o gerou ter sido confirmada (veja
// EngagementEventPublisher).
//
// os services nao falam com o rabbitmq direto. eles anunciam o que aconteceu; quem
// transforma isso em mensagem e a camada de mensageria. e a mesma separacao que ja
// existia entre service e repositorio: a regra nao sabe por onde o dado sai.
public record EngagementChanged(
        Long postId,
        long comments,
        long reactions,
        String change,
        Instant occurredAt
) {

    // o vocabulario das mudancas. cada nome vira a routing key da mensagem publicada, o
    // que deixa um assinante escolher o que ouvir: o monolito assina todas para manter
    // os contadores; um servico de notificacao, por exemplo, assinaria so comment.added.
    public static final String COMMENT_ADDED = "comment.added";
    public static final String COMMENT_REMOVED = "comment.removed";
    public static final String REACTION_ADDED = "reaction.added";
    public static final String REACTION_REMOVED = "reaction.removed";
    public static final String ENGAGEMENT_PURGED = "engagement.purged";
    public static final String ENGAGEMENT_SNAPSHOT = "engagement.snapshot";
}
