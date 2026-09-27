package com.blog.engagement.web;

import com.blog.engagement.client.EngagementClient;
import com.blog.engagement.projection.EngagementCounterRepository;
import com.blog.engagement.projection.EngagementCountersService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// os contadores da estante saem da copia local, e nao do microsservico. o
// verifyNoInteractions e o que transforma essa decisao em teste: se alguem trocasse a
// projecao por uma chamada de rede, a estante voltaria a depender do engajamento estar
// de pe, e este teste falharia.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EngagementCountersApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EngagementCountersService countersService;

    @Autowired
    private EngagementCounterRepository repository;

    @MockBean
    private EngagementClient engagementClient;

    @AfterEach
    void limparBanco() {
        repository.deleteAll();
    }

    @Test
    void contadores_vemDaCopiaLocalSemChamarOMicrosservico() throws Exception {
        countersService.apply(2L, 1, 4, "reaction.added", Instant.parse("2026-05-01T10:00:00Z"));
        countersService.apply(1L, 3, 0, "comment.added", Instant.parse("2026-05-01T10:01:00Z"));

        mockMvc.perform(get("/api/engagement/counters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].postId").value(1))
                .andExpect(jsonPath("$[0].comments").value(3))
                .andExpect(jsonPath("$[1].postId").value(2))
                .andExpect(jsonPath("$[1].reactions").value(4))
                .andExpect(jsonPath("$[1].updatedAt").value("2026-05-01T10:00:00Z"));

        verifyNoInteractions(engagementClient);
    }

    @Test
    void semNenhumEventoAinda_respondeListaVazia() throws Exception {
        mockMvc.perform(get("/api/engagement/counters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
