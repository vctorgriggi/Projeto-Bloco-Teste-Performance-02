package com.blog.engagement.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.blog.shared.messaging.Topology.COMMANDS_EXCHANGE;
import static com.blog.shared.messaging.Topology.SNAPSHOT_REQUEST;

// pede ao engajamento, ao subir, que republique o estado de todos os posts.
//
// a copia local dos contadores vive no banco em memoria deste servico e nasce vazia a
// cada restart. o jeito "sincrono" de preenche-la seria buscar tudo por http no startup,
// o que faria este servico depender de o engajamento estar de pe naquele instante. pela
// fila, o pedido espera: se o engajamento estiver fora, ele atende quando voltar, e os
// contadores aparecem sem ninguem precisar reiniciar nada.
//
// e por melhor esforco: sem broker no startup, os contadores comecam vazios e se
// preenchem com as mudancas seguintes. desligado no perfil de teste.
@Component
@ConditionalOnProperty(name = "blog.messaging.request-snapshot-on-startup", havingValue = "true", matchIfMissing = true)
public class SnapshotRequestOnStartup {

    private static final Logger log = LoggerFactory.getLogger(SnapshotRequestOnStartup.class);

    private final RabbitTemplate rabbitTemplate;
    private final String applicationName;

    public SnapshotRequestOnStartup(RabbitTemplate rabbitTemplate,
                                    @Value("${spring.application.name}") String applicationName) {
        this.rabbitTemplate = rabbitTemplate;
        this.applicationName = applicationName;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void pedirEstadoAtual() {
        try {
            rabbitTemplate.convertAndSend(COMMANDS_EXCHANGE, SNAPSHOT_REQUEST,
                    new SnapshotRequest(applicationName, Instant.now()), message -> {
                        message.getMessageProperties().setType(SnapshotRequest.TYPE);
                        return message;
                    });
            log.info("pedido o estado atual do engajamento para montar os contadores");
        } catch (AmqpException e) {
            log.warn("broker indisponivel no startup; os contadores se preenchem com as proximas mudancas: {}",
                    e.getMessage());
        }
    }
}
