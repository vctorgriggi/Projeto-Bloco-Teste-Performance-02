package com.blog.engagement.web.dto;

import com.blog.engagement.domain.ReactionType;

import java.util.List;
import java.util.Map;

// resumo de engajamento de um post: o total, a contagem por tipo e quais tipos o
// leitor que esta perguntando ja marcou.
//
// devolver o resumo (e nao a lista de reacoes) e uma decisao de contrato: e o que
// a interface precisa para desenhar a barra de reacoes, cabe em uma resposta
// pequena e evita que o front tenha que agregar nada.
public record ReactionSummaryResponse(
        Long postId,
        long total,
        Map<ReactionType, Long> counts,
        List<ReactionType> mine
) {
}
