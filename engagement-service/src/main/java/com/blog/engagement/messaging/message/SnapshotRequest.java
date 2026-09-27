package com.blog.engagement.messaging.message;

import java.time.Instant;

// comando: "republique o estado do engajamento de todos os posts".
//
// quem manda e o monolito, ao subir: a copia local que ele mantem dos contadores vive
// em um banco em memoria e nasce vazia. em vez de um endpoint para ele buscar tudo por
// http (o que o faria depender de este servico estar de pe naquele instante), ele pede
// pela fila, e a resposta volta pelo mesmo caminho de sempre -- eventos
// engagement.snapshot no exchange blog.engagement. se este servico estiver fora do ar,
// o pedido espera na fila.
public record SnapshotRequest(
        String requestedBy,
        Instant requestedAt
) {
}
