package com.blog.engagement.web;

import com.blog.engagement.repository.ReactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// os endpoints novos desta entrega, do http ate o banco: reagir, consultar o
// resumo e desfazer, mais os erros que o contrato promete.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReactionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReactionRepository reactionRepository;

    @AfterEach
    void limparBanco() {
        reactionRepository.deleteAll();
    }

    private void reagir(long postId, String leitor, String tipo) throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/reactions", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"readerName\":\"" + leitor + "\",\"type\":\"" + tipo + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void postReacao_devolve201ComOResumoAtualizado() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/reactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerName":"Carla","type":"CORACAO"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.counts.CORACAO").value(1))
                .andExpect(jsonPath("$.counts.CAFE").value(0))
                .andExpect(jsonPath("$.mine[0]").value("CORACAO"));
    }

    @Test
    void getResumo_somaOsLeitoresEMarcaSoAsReacoesDeQuemPergunta() throws Exception {
        reagir(1L, "Carla", "CORACAO");
        reagir(1L, "Diego", "CORACAO");
        reagir(1L, "Diego", "IDEIA");

        mockMvc.perform(get("/api/posts/{postId}/reactions", 1L).param("reader", "Carla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.counts.CORACAO").value(2))
                .andExpect(jsonPath("$.counts.IDEIA").value(1))
                .andExpect(jsonPath("$.mine.length()").value(1))
                .andExpect(jsonPath("$.mine[0]").value("CORACAO"));
    }

    @Test
    void getResumoSemLeitor_trazOsTotaisSemMarcacao() throws Exception {
        reagir(1L, "Carla", "CORACAO");

        mockMvc.perform(get("/api/posts/{postId}/reactions", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.mine.length()").value(0));
    }

    @Test
    void reacaoRepetidaDoMesmoLeitor_devolve409() throws Exception {
        reagir(1L, "Carla", "CORACAO");

        mockMvc.perform(post("/api/posts/{postId}/reactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerName":"Carla","type":"CORACAO"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Carla")));
    }

    @Test
    void deleteReacao_devolveOResumoSemEla() throws Exception {
        reagir(1L, "Carla", "CORACAO");
        reagir(1L, "Carla", "IDEIA");

        mockMvc.perform(delete("/api/posts/{postId}/reactions/{type}", 1L, "CORACAO")
                        .param("reader", "Carla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.counts.CORACAO").value(0))
                .andExpect(jsonPath("$.mine[0]").value("IDEIA"));
    }

    @Test
    void deleteDeReacaoQueNaoExiste_devolve404() throws Exception {
        mockMvc.perform(delete("/api/posts/{postId}/reactions/{type}", 1L, "CAFE")
                        .param("reader", "Carla"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tipoDeReacaoInvalidoNaUrl_devolve400() throws Exception {
        mockMvc.perform(delete("/api/posts/{postId}/reactions/{type}", 1L, "APLAUSO")
                        .param("reader", "Carla"))
                .andExpect(status().isBadRequest());
    }

    // o 400 tem que sair no mesmo envelope dos outros erros, com uma mensagem util: e
    // essa mensagem que o monolito repassa ao leitor quando o tipo nao existe
    @Test
    void tipoDeReacaoInvalidoNoCorpo_devolve400ExplicandoOsValoresAceitos() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/reactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerName":"Carla","type":"APLAUSO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "Valor invalido para o campo type. Valores aceitos: CORACAO, CAFE, IDEIA"));
    }

    @Test
    void corpoQueNaoEJson_devolve400NoEnvelopePadrao() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/reactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("nao sou json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisicao invalido"));
    }

    @Test
    void reacaoSemLeitor_devolve400() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/reactions", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerName":"","type":"CORACAO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.readerName").isNotEmpty());
    }
}
