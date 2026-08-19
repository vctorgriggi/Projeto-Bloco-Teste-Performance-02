package com.blog.engagement.web.dto;

import java.time.Instant;

// diagnostico da integracao, do jeito que o front consome: se da para contar com a
// conversa e as reacoes agora, quantas instancias do microsservico estao registradas
// na descoberta, e de quando e essa leitura.
public record EngagementStatusResponse(
        String service,
        boolean available,
        int registeredInstances,
        Instant checkedAt
) {
}
