package com.blog.authoring.messaging;

import com.blog.shared.event.PostDeletedEvent;
import com.blog.shared.messaging.outbox.OutboxWriter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.blog.shared.messaging.Topology.POSTS_EXCHANGE;
import static com.blog.shared.messaging.Topology.POST_DELETED;

// transforma o evento interno de post apagado em mensagem de integracao, gravada no
// outbox.
//
// e um @EventListener comum, e nao um @TransactionalEventListener, de proposito: ele roda
// sincrono, dentro da transacao do PostService.delete, e a linha do outbox entra no mesmo
// commit da exclusao do post. se a exclusao voltar atras, a mensagem volta junto; se o
// commit acontecer, a mensagem esta garantida no banco, e o relay a leva ao broker assim
// que ele estiver disponivel.
//
// e o oposto do que a terceira entrega fazia neste ponto (uma chamada http depois do
// commit, por melhor esforco, que se perdia se o engajamento estivesse fora). o
// PostService nao mudou: continua publicando o mesmo PostDeletedEvent. o que mudou foi
// quem escuta, e e exatamente o que o comentario do evento previa: "se um dia virar uma
// mensagem em um broker, o unico ponto a trocar e a entrega".
@Component
public class PostEventsOutbox {

    private final OutboxWriter outboxWriter;

    public PostEventsOutbox(OutboxWriter outboxWriter) {
        this.outboxWriter = outboxWriter;
    }

    @EventListener
    public void onPostDeleted(PostDeletedEvent event) {
        outboxWriter.record(POSTS_EXCHANGE, POST_DELETED, PostDeletedMessage.TYPE,
                new PostDeletedMessage(event.postId(), Instant.now()));
    }
}
