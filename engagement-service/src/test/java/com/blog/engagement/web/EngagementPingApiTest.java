package com.blog.engagement.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// o ping e o contrato que sustenta o selo de disponibilidade na interface, entao
// vale um teste: se o nome do servico ou o formato mudarem, o monolito passa a
// reportar o engajamento como fora do ar sem que ele esteja.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EngagementPingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ping_respondeComONomeDoServicoEStatusUp() throws Exception {
        mockMvc.perform(get("/api/engagement/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("engagement-service"))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
