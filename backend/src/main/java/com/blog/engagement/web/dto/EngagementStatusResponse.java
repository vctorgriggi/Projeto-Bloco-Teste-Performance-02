package com.blog.engagement.web.dto;

import java.time.Instant;

// diagnostico da integracao, do jeito que o front consome: se da para contar com a
// conversa e as reacoes agora, quantas instancias do microsservico estao registradas
// na descoberta, e de quando e essa leitura.
//
// a quarta entrega acrescentou a mensageria: se o broker esta alcancavel e quantos
// eventos ainda esperam no outbox para sair. sao informacoes independentes do
// "available", e a combinacao e o que interessa -- engajamento fora e broker de pe
// significa que da para comentar, e o recado espera na fila.
public record EngagementStatusResponse(
        String service,
        boolean available,
        int registeredInstances,
        Instant checkedAt,
        boolean brokerAvailable,
        long pendingEvents
) {
}
