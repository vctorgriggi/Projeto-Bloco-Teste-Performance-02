package com.blog.shared.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// grava uma mensagem no outbox.
//
// o MANDATORY e o ponto do componente inteiro: exige uma transacao ja aberta por quem
// chamou, e falha se nao houver. gravar no outbox fora da transacao da mudanca seria so
// uma segunda escrita independente -- exatamente o problema que o outbox existe para
// evitar. com MANDATORY, esse erro aparece no primeiro teste, e nao em producao.
@Component
public class OutboxWriter {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxMessage record(String exchange, String routingKey, String type, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return outboxRepository.save(new OutboxMessage(exchange, routingKey, type, json));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("nao foi possivel serializar a mensagem " + type, e);
        }
    }
}
