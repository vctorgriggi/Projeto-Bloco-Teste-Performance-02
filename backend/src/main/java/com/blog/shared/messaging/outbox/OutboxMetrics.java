package com.blog.shared.messaging.outbox;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

// publica o numero de eventos esperando no outbox como metrica (blog.outbox.pending).
//
// o diagnostico /api/engagement/status ja mostra esse numero para quem pergunta; a
// metrica e para quem opera: no grafana ela vira uma serie no tempo, e um alerta pode
// disparar quando ela so cresce -- o sinal de que o broker caiu e os eventos estao
// guardados esperando por ele.
//
// usa a api do opentelemetry, e nao o micrometer: quem coleta e exporta e o agente das
// imagens, o mesmo que ja manda traces e logs. sem o agente (desenvolvimento, testes) a
// api e um no-op, e o callback nunca e chamado.
@Component
public class OutboxMetrics {

    private final ObservableLongGauge pendentes;

    public OutboxMetrics(OutboxRepository outboxRepository) {
        this.pendentes = GlobalOpenTelemetry.getMeter("com.blog.outbox")
                .gaugeBuilder("blog.outbox.pending")
                .setDescription("eventos gravados no outbox que ainda nao sairam para o broker")
                .setUnit("{evento}")
                .ofLongs()
                .buildWithCallback(medida -> medida.record(outboxRepository.countByPublishedAtIsNull()));
    }

    @PreDestroy
    void encerrar() {
        pendentes.close();
    }
}
