package com.blog.engagement.messaging;

import com.blog.engagement.messaging.message.SnapshotRequest;
import com.blog.engagement.service.EngagementChanged;
import com.blog.engagement.service.EngagementChanges;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.blog.engagement.messaging.Topology.SNAPSHOT_REQUESTS_QUEUE;

// atende o pedido de republicacao do estado: publica um engagement.snapshot para cada
// post que tem algum engajamento.
//
// a resposta nao volta para quem pediu, e sim para o exchange de eventos, como qualquer
// outra mudanca. e deliberado: quem pediu (o monolito reconstruindo a copia dele) ja
// assina esse exchange, e qualquer outro assinante que precise se reconstruir aproveita
// a mesma republicacao. um request/reply dedicado criaria um segundo caminho para o
// mesmo dado.
@Component
public class SnapshotRequestListener {

    private static final Logger log = LoggerFactory.getLogger(SnapshotRequestListener.class);

    private final EngagementChanges engagementChanges;
    private final EngagementEventPublisher publisher;

    public SnapshotRequestListener(EngagementChanges engagementChanges, EngagementEventPublisher publisher) {
        this.engagementChanges = engagementChanges;
        this.publisher = publisher;
    }

    @RabbitListener(queues = SNAPSHOT_REQUESTS_QUEUE, id = "snapshot-requests")
    public void onSnapshotRequest(SnapshotRequest request) {
        List<EngagementChanged> estado = engagementChanges.currentState(EngagementChanged.ENGAGEMENT_SNAPSHOT);
        estado.forEach(publisher::publish);
        log.info("estado de {} post(s) republicado a pedido de {}", estado.size(), request.requestedBy());
    }
}
