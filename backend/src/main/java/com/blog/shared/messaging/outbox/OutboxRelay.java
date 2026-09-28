package com.blog.shared.messaging.outbox;

import com.blog.shared.messaging.ConfirmedPublisher;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

// leva as mensagens pendentes do outbox para o broker.
//
// para cada uma: publica, espera a confirmacao do broker e so entao marca como
// publicada. se o processo cair entre a confirmacao e a marcacao, a mensagem sai de novo
// na proxima varredura -- a garantia e "pelo menos uma vez", e nao "exatamente uma". e
// por isso que os consumidores do outro lado sao idempotentes: limpar duas vezes o
// engajamento do mesmo post da o mesmo resultado.
//
// duas decisoes deliberadas:
//
// - na primeira falha, o lote para. a falha quase sempre e o broker fora do ar, e tentar
//   as mensagens seguintes so repetiria o erro; alem disso, pular uma e publicar a
//   proxima inverteria a ordem dos eventos.
// - nao ha transacao em volta do lote. cada marcacao grava sozinha, e nenhuma conexao do
//   banco fica presa enquanto o relay espera a confirmacao do broker.
//
// e uma terceira, da quinta entrega: cada mensagem e publicada dentro do contexto do
// trace de quem a gravou. o relay roda numa thread agendada, sem trace nenhum; sem
// restaurar o contexto, o agente do opentelemetry abriria um trace novo a cada
// publicacao, e "apagar o post" e "limpar a conversa no outro servico" apareceriam
// separados no grafana. com ele, sao um trace so, com o intervalo do outbox visivel.
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;
    private final ConfirmedPublisher publisher;

    public OutboxRelay(OutboxRepository outboxRepository, ConfirmedPublisher publisher) {
        this.outboxRepository = outboxRepository;
        this.publisher = publisher;
    }

    // devolve quantas mensagens sairam nesta varredura
    public int relayPending() {
        List<OutboxMessage> pendentes = outboxRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        int publicadas = 0;

        for (OutboxMessage pendente : pendentes) {
            try (Scope ignorado = contextoDeQuemGravou(pendente).makeCurrent()) {
                publisher.send(pendente.getExchange(), pendente.getRoutingKey(), toAmqp(pendente));
            } catch (AmqpException e) {
                pendente.markFailed(e.getMessage());
                outboxRepository.save(pendente);
                log.warn("outbox: {} {} nao saiu (tentativa {}): {} -- {} pendente(s) aguardam o broker",
                        pendente.getType(), pendente.getId(), pendente.getAttempts(), e.getMessage(),
                        pendentes.size() - publicadas);
                break;
            }
            pendente.markPublished();
            outboxRepository.save(pendente);
            publicadas++;
            log.info("outbox: {} publicado ({} -> {}, id {})",
                    pendente.getType(), pendente.getExchange(), pendente.getRoutingKey(), pendente.getId());
        }
        return publicadas;
    }

    private static Context contextoDeQuemGravou(OutboxMessage pendente) {
        if (pendente.getTraceParent() == null) {
            return Context.current();
        }
        return W3CTraceContextPropagator.getInstance()
                .extract(Context.root(), Map.of("traceparent", pendente.getTraceParent()), PORTADOR);
    }

    private static final TextMapGetter<Map<String, String>> PORTADOR = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> portador) {
            return portador.keySet();
        }

        @Override
        public String get(Map<String, String> portador, String chave) {
            return portador == null ? null : portador.get(chave);
        }
    };

    // a mensagem sai com os bytes gravados, sem passar de novo pelo conversor. o message
    // id e o id da linha: se a mesma linha sair duas vezes, o consumidor ve o mesmo id
    private Message toAmqp(OutboxMessage pendente) {
        return MessageBuilder.withBody(pendente.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(pendente.getId())
                .setType(pendente.getType())
                .setTimestamp(Date.from(pendente.getCreatedAt()))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
    }
}
