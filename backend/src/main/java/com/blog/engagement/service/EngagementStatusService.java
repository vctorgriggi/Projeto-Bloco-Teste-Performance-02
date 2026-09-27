package com.blog.engagement.service;

import com.blog.engagement.client.EngagementClient;
import com.blog.engagement.client.dto.ServiceInfoView;
import com.blog.engagement.web.dto.EngagementStatusResponse;
import com.blog.shared.messaging.BrokerHealth;
import com.blog.shared.messaging.outbox.OutboxRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.time.Instant;

// diz se o engajamento esta disponivel agora. e o que sustenta o aviso na interface:
// em vez de o leitor descobrir que o servico caiu ao tentar comentar, a tela ja
// avisa antes.
//
// a resposta junta duas fontes de informacao diferentes, que respondem perguntas
// diferentes:
//
// - o DiscoveryClient consulta o registro do eureka: "quantas instancias do
//   engajamento estao registradas?". e a visao do que deveria existir.
// - o ping pelo cliente feign vai ate o servico: "ele responde?". e a visao do que
//   de fato funciona. se nao responder, o fallback devolve DOWN em vez de estourar.
//
// as duas podem discordar, e a divergencia e informacao util: instancia registrada
// que nao responde e um processo travado ou um registro que ainda nao expirou.
//
// desde a quarta entrega a resposta diz tambem se o broker esta de pe e quantos eventos
// esperam no outbox. o numero de pendentes e o termometro da mensageria: zero e o
// normal; um numero que so cresce e o broker fora do ar, com os eventos guardados em
// seguranca no banco esperando por ele.
@Service
public class EngagementStatusService {

    private static final String SERVICE_ID = "engagement-service";

    private final EngagementClient engagementClient;
    private final ObjectProvider<DiscoveryClient> discoveryClient;
    private final BrokerHealth brokerHealth;
    private final OutboxRepository outboxRepository;

    // o DiscoveryClient chega por ObjectProvider porque ele so existe quando a
    // descoberta esta ligada. assim o monolito continua subindo (e reportando o
    // status pelo ping) mesmo quando roda apontado direto para o microsservico,
    // sem eureka nenhum -- o cenario dos testes e da execucao simplificada.
    public EngagementStatusService(EngagementClient engagementClient,
                                   ObjectProvider<DiscoveryClient> discoveryClient,
                                   BrokerHealth brokerHealth,
                                   OutboxRepository outboxRepository) {
        this.engagementClient = engagementClient;
        this.discoveryClient = discoveryClient;
        this.brokerHealth = brokerHealth;
        this.outboxRepository = outboxRepository;
    }

    public EngagementStatusResponse current() {
        ServiceInfoView ping = engagementClient.ping();
        boolean disponivel = "UP".equalsIgnoreCase(ping.status());

        return new EngagementStatusResponse(SERVICE_ID, disponivel, instanciasRegistradas(), Instant.now(),
                brokerHealth.available(), outboxRepository.countByPublishedAtIsNull());
    }

    private int instanciasRegistradas() {
        DiscoveryClient registro = discoveryClient.getIfAvailable();
        return registro == null ? 0 : registro.getInstances(SERVICE_ID).size();
    }
}
