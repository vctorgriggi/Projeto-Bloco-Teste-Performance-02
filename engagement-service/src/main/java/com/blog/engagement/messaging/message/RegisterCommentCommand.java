package com.blog.engagement.messaging.message;

import java.time.Instant;

// comando: "registre este comentario". chega do monolito pela fila
// engagement.comment-commands.
//
// e um comando, e nao um evento, e a diferenca aparece no nome e no destino: esta no
// imperativo, tem um unico destinatario (este servico) e pode ser recusado. um evento
// e um fato consumado, no passado, que o publicador anuncia sem saber quem ouve.
//
// o submissionId e gerado pelo monolito no momento em que aceita o comentario, e e a
// identidade do comentario antes de ele existir aqui. serve para duas coisas: tornar o
// processamento idempotente (a mesma mensagem entregue duas vezes nao vira dois
// comentarios) e deixar a interface reconhecer, na listagem, o recado que ela mesma
// mandou e que ate entao mostrava como "na fila".
//
// o submittedAt e o instante em que o leitor enviou, e vira o createdAt do comentario.
// se a mensagem esperar na fila porque este servico estava fora do ar, a conversa ainda
// fica na ordem em que as pessoas escreveram, e nao na ordem em que a fila foi drenada.
public record RegisterCommentCommand(
        String submissionId,
        Long postId,
        String authorName,
        String content,
        Instant submittedAt
) {
}
