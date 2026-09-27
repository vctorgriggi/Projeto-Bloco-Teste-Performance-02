package com.blog.engagement.messaging;

import java.time.Instant;

// comando: "registre este comentario". vai para a fila engagement.comment-commands, pelo
// exchange blog.commands.
//
// o engagement-service tem um record com os mesmos campos, e nao uma copia deste arquivo
// por dependencia: o contrato e o json, e cada lado desserializa no tipo que declara.
//
// o submissionId e gerado aqui, no momento em que o leitor envia, e e a identidade do
// comentario antes de ele existir do outro lado. e o que torna o consumo idempotente la
// e o que deixa a interface reconhecer o proprio recado quando ele aparece na listagem.
public record RegisterCommentCommand(
        String submissionId,
        Long postId,
        String authorName,
        String content,
        Instant submittedAt
) {

    public static final String TYPE = "comment.register";
}
