package com.blog.engagement.messaging.message;

import java.time.Instant;

// evento: "o post foi apagado". publicado pelo monolito no exchange blog.posts, com a
// routing key post.deleted, e assinado por este servico pela fila engagement.post-deleted.
//
// e uma notificacao de evento no sentido estrito: carrega so o id do que mudou. este
// servico nao precisa de mais nada do post para limpar o que tem dele.
public record PostDeletedMessage(
        Long postId,
        Instant deletedAt
) {
}
