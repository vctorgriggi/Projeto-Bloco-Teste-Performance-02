package com.blog.engagement.messaging;

import com.blog.engagement.service.EngagementChanged;
import com.blog.engagement.service.EngagementChanges;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

// o que acontece quando este servico termina de subir, nesta ordem:
//
// 1. anuncia o proprio estado (um engagement.snapshot por post), para quem mantem copia
//    dos contadores se acertar com o que este processo tem agora;
// 2. so entao liga os consumidores das filas.
//
// a ordem veio de um defeito real, encontrado subindo a stack e derrubando este servico
// no meio da demonstracao. os consumidores do spring ligam durante a inicializacao do
// contexto, ANTES dos CommandLineRunner -- e o seeder e um CommandLineRunner. com
// mensagens esperando na fila, este servico processou um post.deleted num banco ainda
// vazio (limpou zero), e o seeder recriou em seguida a conversa do post apagado; e o
// comentario que esperava na fila saiu anunciado com os totais de antes do seed, deixando
// o contador da estante errado.
//
// a regra que sobra e geral, e nao so do seeder: um servico so deve consumir quando esta
// pronto para processar. por isso o auto-startup dos consumidores e false no
// application.yml, e quem os liga e este componente, no ApplicationReadyEvent -- que o
// spring publica depois de todos os runners.
@Component
public class MessagingStartup {

    private static final Logger log = LoggerFactory.getLogger(MessagingStartup.class);

    private final EngagementChanges engagementChanges;
    private final EngagementEventPublisher publisher;
    private final RabbitListenerEndpointRegistry listeners;
    private final boolean publishSnapshot;
    private final boolean startConsumers;

    public MessagingStartup(EngagementChanges engagementChanges, EngagementEventPublisher publisher,
                            RabbitListenerEndpointRegistry listeners,
                            @Value("${engagement.messaging.publish-snapshot-on-startup:true}") boolean publishSnapshot,
                            @Value("${engagement.messaging.start-consumers-on-ready:true}") boolean startConsumers) {
        this.engagementChanges = engagementChanges;
        this.publisher = publisher;
        this.listeners = listeners;
        this.publishSnapshot = publishSnapshot;
        this.startConsumers = startConsumers;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (publishSnapshot) {
            List<EngagementChanged> estado = engagementChanges.currentState(EngagementChanged.ENGAGEMENT_SNAPSHOT);
            estado.forEach(publisher::publish);
            log.info("estado de {} post(s) anunciado na subida", estado.size());
        }
        if (startConsumers) {
            listeners.start();
            log.info("consumidores ligados: {}", listeners.getListenerContainerIds());
        }
    }
}
