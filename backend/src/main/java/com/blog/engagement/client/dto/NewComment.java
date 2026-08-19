package com.blog.engagement.client.dto;

// corpo enviado ao microsservico para registrar um comentario. o postId nao entra
// aqui porque vai na url, como no contrato do proprio servico.
public record NewComment(
        String authorName,
        String content
) {
}
