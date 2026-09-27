package com.blog.authoring.messaging;

import java.time.Instant;

// o evento "post apagado" como ele sai para o broker (exchange blog.posts, routing key
// post.deleted).
//
// e o contrato publico do fato, e por isso e um record separado do PostDeletedEvent, que
// e o evento interno do processo. hoje os dois tem quase os mesmos campos; separados, o
// evento interno pode mudar a vontade, e o que sai para outros servicos so muda de
// proposito.
//
// carrega so o id: e uma notificacao de evento. quem precisar de mais sobre o post teria
// que perguntar a quem e dono dele -- e o unico assinante hoje, o engajamento, nao
// precisa de mais nada para limpar o que tem.
public record PostDeletedMessage(
        Long postId,
        Instant deletedAt
) {

    public static final String TYPE = "post.deleted";
}
