package com.blog.engagement.web;

import com.blog.engagement.client.EngagementClient;
import com.blog.engagement.client.dto.ServiceInfoView;
import com.blog.shared.messaging.BrokerHealth;
import com.blog.shared.messaging.outbox.OutboxRepository;
import com.blog.shared.messaging.outbox.OutboxWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// o endpoint que sustenta o aviso na interface. o detalhe que este teste protege e o
// status http: mesmo quando o engajamento esta fora do ar, a resposta e 200, porque a
// pergunta "esta disponivel?" foi respondida com sucesso. se aqui saisse 503, o front
// trataria o proprio diagnostico como falha e nao teria o que mostrar.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EngagementStatusApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EngagementClient engagementClient;

    @MockBean
    private BrokerHealth brokerHealth;

    @Autowired
    private OutboxWriter outboxWriter;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    @AfterEach
    void limparOutbox() {
        outboxRepository.deleteAll();
    }

    @Test
    void microsservicoDePe_reportaDisponivel() throws Exception {
        given(engagementClient.ping()).willReturn(new ServiceInfoView("engagement-service", "UP"));

        mockMvc.perform(get("/api/engagement/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("engagement-service"))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.checkedAt").isNotEmpty());
    }

    // e o fallback do cliente que devolve DOWN quando a chamada nao completa
    @Test
    void microsservicoForaDoAr_reporta200ComDisponivelFalso() throws Exception {
        given(engagementClient.ping()).willReturn(new ServiceInfoView("engagement-service", "DOWN"));

        mockMvc.perform(get("/api/engagement/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    // com o eureka desligado no perfil de teste, a contagem de instancias registradas e
    // zero e a resposta continua valida: as duas informacoes sao independentes
    @Test
    void semDescobertaAtiva_aContagemDeInstanciasEZero() throws Exception {
        given(engagementClient.ping()).willReturn(new ServiceInfoView("engagement-service", "UP"));

        mockMvc.perform(get("/api/engagement/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registeredInstances").value(0));
    }

    // com a mensageria, o engajamento e o broker podem cair um sem o outro, e a
    // combinacao muda o que o leitor consegue fazer: engajamento fora e broker de pe
    // significa que ainda da para comentar, e o recado espera na fila
    @Test
    void engajamentoForaEBrokerDePe_saoReportadosSeparados() throws Exception {
        given(engagementClient.ping()).willReturn(new ServiceInfoView("engagement-service", "DOWN"));
        given(brokerHealth.available()).willReturn(true);

        mockMvc.perform(get("/api/engagement/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.brokerAvailable").value(true));
    }

    // o numero de eventos esperando no outbox e o termometro da mensageria: com o broker
    // fora, ele cresce, e o diagnostico mostra que os eventos estao guardados
    @Test
    void eventosEsperandoNoOutbox_aparecemNoDiagnostico() throws Exception {
        given(engagementClient.ping()).willReturn(new ServiceInfoView("engagement-service", "UP"));
        transactionTemplate.executeWithoutResult(status ->
                outboxWriter.record("blog.posts", "post.deleted", "post.deleted", Map.of("postId", 1)));

        mockMvc.perform(get("/api/engagement/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingEvents").value(1));
    }
}
