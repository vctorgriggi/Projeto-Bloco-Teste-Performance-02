package com.blog.engagement.messaging;

import java.time.Instant;

// o que o engagement-service publica a cada mudanca de engajamento: os totais do post
// depois da mudanca, o nome da mudanca (que e tambem a routing key) e quando ela
// aconteceu. este servico assina todas, pela fila blog-api.engagement-snapshots.
public record EngagementSnapshotMessage(
        Long postId,
        long comments,
        long reactions,
        String change,
        Instant occurredAt
) {
}
