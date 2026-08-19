package com.blog.engagement.web.dto;

import com.blog.engagement.client.dto.ReactionSummaryView;

import java.util.List;
import java.util.Map;

// o resumo de reacoes como a api do monolito devolve. e a traducao do que veio do
// microsservico para o contrato desta api, pelo mesmo motivo do CommentResponse:
// se o formato de la mudar, quebra a conversao (um lugar) e nao a tela.
//
// os tipos continuam como texto tambem aqui. o monolito repassa o vocabulario do
// servico de engajamento sem interpreta-lo, e quem desenha a barra de reacoes com
// esses nomes e o front.
public record ReactionSummaryResponse(
        Long postId,
        long total,
        Map<String, Long> counts,
        List<String> mine
) {
    public static ReactionSummaryResponse from(ReactionSummaryView summary) {
        return new ReactionSummaryResponse(
                summary.postId(),
                summary.total(),
                summary.counts(),
                summary.mine()
        );
    }
}
