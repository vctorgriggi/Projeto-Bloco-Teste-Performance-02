package com.blog.engagement.client.dto;

// o que o microsservico responde ao limpar o engajamento de um post apagado.
// serve so para o log da limpeza dizer quanta coisa foi removida.
public record PurgeView(
        Long postId,
        long comments,
        long reactions
) {
}
