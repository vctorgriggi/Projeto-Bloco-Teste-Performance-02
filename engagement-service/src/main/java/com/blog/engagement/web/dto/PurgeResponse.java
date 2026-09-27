package com.blog.engagement.web.dto;

// o que a limpeza de um post removeu. devolver os numeros em vez de um 204 vazio e
// deliberado: eles vao para o log (hoje o do PostDeletedListener, que consome o evento
// de post apagado; na terceira entrega, o do monolito, que chamava a rota http), e e
// assim que se enxerga um problema de consistencia entre os dois bancos -- limpar zero
// comentario de um post que tinha conversa e sinal de que os ids sairam de sincronia.
public record PurgeResponse(
        Long postId,
        long comments,
        long reactions
) {
}
