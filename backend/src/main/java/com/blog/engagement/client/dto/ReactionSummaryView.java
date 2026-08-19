package com.blog.engagement.client.dto;

import java.util.List;
import java.util.Map;

// o resumo de reacoes como o microsservico devolve: totais por tipo e os tipos que
// aquele leitor ja marcou. as chaves do mapa sao os nomes dos tipos, nao um enum,
// pelo mesmo motivo do NewReaction.
public record ReactionSummaryView(
        Long postId,
        long total,
        Map<String, Long> counts,
        List<String> mine
) {
}
