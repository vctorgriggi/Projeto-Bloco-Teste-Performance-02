package com.blog.engagement.messaging;

import com.blog.engagement.projection.EngagementCountersService;
import com.blog.shared.exception.InvalidMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.blog.shared.messaging.Topology.ENGAGEMENT_SNAPSHOTS_QUEUE;

// consome as mudancas de engajamento e mantem a copia local dos contadores.
//
// e aqui que o sentido das dependencias fica interessante. por http, a dependencia
// continua de mao unica: o monolito chama o engajamento, nunca o contrario. por eventos,
// a informacao agora tambem corre no sentido oposto -- e isso nao cria o ciclo que a
// terceira entrega evitou, porque quem publica nao depende de quem consome. o
// engajamento anuncia no exchange dele e nao sabe que este servico existe; se este
// servico cair, o engajamento continua funcionando e as mensagens esperam na fila.
@Component
public class EngagementSnapshotListener {

    private static final Logger log = LoggerFactory.getLogger(EngagementSnapshotListener.class);

    private final EngagementCountersService countersService;

    public EngagementSnapshotListener(EngagementCountersService countersService) {
        this.countersService = countersService;
    }

    @RabbitListener(queues = ENGAGEMENT_SNAPSHOTS_QUEUE, id = "engagement-snapshots")
    public void onEngagementChanged(EngagementSnapshotMessage message) {
        if (message.postId() == null || message.occurredAt() == null) {
            throw new InvalidMessageException("mudanca de engajamento sem postId ou sem occurredAt");
        }

        boolean aplicada = countersService.apply(message.postId(), message.comments(), message.reactions(),
                message.change(), message.occurredAt());
        if (aplicada) {
            log.debug("contadores do post {} atualizados por {}: {} comentario(s), {} reacao(oes)",
                    message.postId(), message.change(), message.comments(), message.reactions());
        } else {
            log.info("mudanca {} do post {} chegou fora de ordem e foi descartada (ja havia uma mais nova)",
                    message.change(), message.postId());
        }
    }
}
