package com.blog.engagement.client.dto;

// corpo enviado ao microsservico para registrar uma reacao. o tipo viaja como
// texto: o vocabulario de reacoes pertence ao servico de engajamento, e o monolito
// so repassa o que o cliente pediu. um valor invalido e recusado por quem e dono
// da regra, com 400, e esse 400 volta ao front sem virar erro de infraestrutura.
public record NewReaction(
        String readerName,
        String type
) {
}
