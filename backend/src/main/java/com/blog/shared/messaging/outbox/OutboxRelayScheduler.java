package com.blog.shared.messaging.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// dispara o relay em intervalo fixo. o intervalo vem do config server: e o quanto um
// evento pode demorar para sair depois do commit, um ajuste de operacao.
//
// separado do OutboxRelay para que os testes exercitem a logica do relay chamando-a
// direto, com o agendamento desligado (blog.messaging.outbox.relay-enabled = false no
// perfil de teste) -- um relay rodando sozinho em segundo plano tornaria os testes
// dependentes de tempo.
//
// fixedDelay, e nao fixedRate: a proxima varredura so comeca depois de a anterior
// terminar, entao um broker lento nao empilha varreduras concorrentes publicando a
// mesma linha.
@Component
@ConditionalOnProperty(name = "blog.messaging.outbox.relay-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {

    private final OutboxRelay relay;

    public OutboxRelayScheduler(OutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${blog.messaging.outbox.relay-interval-ms:2000}",
            initialDelayString = "${blog.messaging.outbox.relay-interval-ms:2000}")
    public void relay() {
        relay.relayPending();
    }
}
