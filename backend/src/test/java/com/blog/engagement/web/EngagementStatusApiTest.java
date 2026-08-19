package com.blog.engagement.web;

import com.blog.engagement.client.EngagementClient;
import com.blog.engagement.client.dto.ServiceInfoView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
}
