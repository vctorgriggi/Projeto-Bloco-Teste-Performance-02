package com.blog.shared.event;

// evento de dominio publicado quando um post e apagado.
//
// mora em shared de proposito: authoring publica e engagement escuta, e nenhum dos
// dois passa a depender do pacote do outro. hoje o evento e entregue dentro do
// mesmo processo pelo ApplicationEventPublisher do spring; se um dia virar uma
// mensagem em um broker, o unico ponto a trocar e a entrega, nao quem publica nem
// quem reage.
public record PostDeletedEvent(Long postId) {
}
