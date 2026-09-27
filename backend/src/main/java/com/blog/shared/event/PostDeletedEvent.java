package com.blog.shared.event;

// evento de dominio publicado quando um post e apagado.
//
// mora em shared de proposito: quem publica nao precisa conhecer quem escuta. ate a
// terceira entrega o evento era entregue dentro do processo e escutado pelo contexto
// de engajamento, que chamava o microsservico por http. o comentario original dizia
// que, se um dia virasse mensagem em um broker, o unico ponto a trocar seria a
// entrega -- e foi o que aconteceu na quarta: quem escuta agora e o PostEventsOutbox,
// que grava a mensagem de integracao na mesma transacao, e o PostService nao mudou.
public record PostDeletedEvent(Long postId) {
}
