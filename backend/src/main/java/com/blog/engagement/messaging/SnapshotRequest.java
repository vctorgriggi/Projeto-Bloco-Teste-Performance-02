package com.blog.engagement.messaging;

import java.time.Instant;

// comando: "republique o estado do engajamento de todos os posts". e o pedido que este
// servico faz ao subir, para reconstruir a copia local dos contadores (veja
// SnapshotRequestOnStartup). a resposta volta como eventos engagement.snapshot na fila
// de sempre.
public record SnapshotRequest(
        String requestedBy,
        Instant requestedAt
) {

    public static final String TYPE = "engagement.snapshot.request";
}
