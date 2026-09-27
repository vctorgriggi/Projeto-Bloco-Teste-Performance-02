package com.blog.engagement.client.dto;

import java.time.Instant;

// o comentario como o microsservico devolve.
//
// tem os mesmos campos do CommentResponse que a api do monolito expoe, e a
// duplicacao e intencional: sao dois contratos diferentes que hoje coincidem. um
// e o que chega do outro servico, o outro e o que o front consome. separados, o
// microsservico pode acrescentar um campo sem que isso vaze para a nossa api, e
// uma mudanca no formato dele quebra a traducao (um lugar) e nao a tela.
//
// o submissionId e nulo para comentarios criados direto na api do microsservico, e
// preenchido para os que chegaram pela fila: e o id que este servico gerou ao aceitar o
// envio (veja CommentService).
public record CommentView(
        Long id,
        Long postId,
        String authorName,
        String content,
        Instant createdAt,
        String submissionId
) {
}
