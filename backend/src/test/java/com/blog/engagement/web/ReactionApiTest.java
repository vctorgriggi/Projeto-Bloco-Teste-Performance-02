package com.blog.engagement.web;

import com.blog.authoring.domain.Post;
import com.blog.authoring.repository.PostRepository;
import com.blog.engagement.client.EngagementClient;
import com.blog.engagement.client.dto.NewReaction;
import com.blog.engagement.client.dto.ReactionSummaryView;
import com.blog.shared.exception.BusinessRuleException;
import com.blog.shared.exception.InvalidRequestException;
import com.blog.shared.exception.ServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// os endpoints novos vistos pelo front. o foco e o mesmo do teste de comentario: o
// monolito valida o post, delega e traduz -- inclusive os erros, que precisam chegar ao
// navegador com o status que tinham do outro lado.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReactionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @MockBean
    private EngagementClient engagementClient;

    @AfterEach
    void limparBanco() {
        postRepository.deleteAll();
    }

    private Long postExistente() {
        return postRepository.save(new Post("Titulo", "conteudo", 1L)).getId();
    }

    private ReactionSummaryView resumo(Long postId, long total, Map<String, Long> counts, List<String> mine) {
        return new ReactionSummaryView(postId, total, counts, mine);
    }

    @Test
    void getResumo_repassaOsTotaisEAsReacoesDoLeitor() throws Exception {
        Long postId = postExistente();
        given(engagementClient.reactionSummary(postId, "Carla")).willReturn(
                resumo(postId, 3, Map.of("CORACAO", 2L, "CAFE", 0L, "IDEIA", 1L), List.of("CORACAO")));

        mockMvc.perform(get("/api/posts/{postId}/reactions", postId).param("reader", "Carla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.counts.CORACAO").value(2))
                .andExpect(jsonPath("$.counts.CAFE").value(0))
                .andExpect(jsonPath("$.mine[0]").value("CORACAO"));
    }

    @Test
    void getResumoSemLeitor_funcionaEChegaSemLeitorAoMicrosservico() throws Exception {
        Long postId = postExistente();
        given(engagementClient.reactionSummary(postId, null)).willReturn(
                resumo(postId, 1, Map.of("CORACAO", 1L, "CAFE", 0L, "IDEIA", 0L), List.of()));

        mockMvc.perform(get("/api/posts/{postId}/reactions", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.mine.length()").value(0));

        verify(engagementClient).reactionSummary(postId, null);
    }

    @Test
    void postReacao_devolve201ComOResumoAtualizado() throws Exception {
        Long postId = postExistente();
        given(engagementClient.react(eq(postId), any(NewReaction.class))).willReturn(
                resumo(postId, 1, Map.of("CORACAO", 1L, "CAFE", 0L, "IDEIA", 0L), List.of("CORACAO")));

        mockMvc.perform(post("/api/posts/{postId}/reactions", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerName":"Carla","type":"CORACAO"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.counts.CORACAO").value(1))
                .andExpect(jsonPath("$.mine[0]").value("CORACAO"));

        verify(engagementClient).react(postId, new NewReaction("Carla", "CORACAO"));
    }

    @Test
    void deleteReacao_desfazPeloParTipoELeitor() throws Exception {
        Long postId = postExistente();
        given(engagementClient.undoReaction(postId, "CORACAO", "Carla")).willReturn(
                resumo(postId, 0, Map.of("CORACAO", 0L, "CAFE", 0L, "IDEIA", 0L), List.of()));

        mockMvc.perform(delete("/api/posts/{postId}/reactions/{type}", postId, "CORACAO")
                        .param("reader", "Carla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.mine.length()").value(0));
    }

    @Test
    void reagirEmPostInexistente_devolve404SemChamarOMicrosservico() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/reactions", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerName":"Carla","type":"CORACAO"}
                                """))
                .andExpect(status().isNotFound());

        verifyNoInteractions(engagementClient);
    }

    // a regra de "uma reacao de cada tipo por leitor" vive no microsservico, e o 409
    // dele chega ao front como 409, com a mensagem que ele escreveu
    @Test
    void reacaoRepetida_chegaComo409ComAMensagemDoOutroServico() throws Exception {
        Long postId = postExistente();
        given(engagementClient.react(eq(postId), any(NewReaction.class)))
                .willThrow(new BusinessRuleException("O leitor Carla ja reagiu com CORACAO neste post"));

        mockMvc.perform(post("/api/posts/{postId}/reactions", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerName":"Carla","type":"CORACAO"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("O leitor Carla ja reagiu com CORACAO neste post"));
    }

    // o monolito nao conhece o vocabulario de reacoes, entao um tipo inventado passa por
    // ele e e recusado por quem e dono da regra. o 400 de la vira 400 aqui.
    @Test
    void tipoDesconhecido_chegaComo400VindoDoMicrosservico() throws Exception {
        Long postId = postExistente();
        given(engagementClient.react(eq(postId), any(NewReaction.class)))
                .willThrow(new InvalidRequestException("Valor invalido para o parametro type"));

        mockMvc.perform(post("/api/posts/{postId}/reactions", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerName":"Carla","type":"APLAUSO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Valor invalido para o parametro type"));
    }

    @Test
    void engajamentoForaDoAr_devolve503() throws Exception {
        Long postId = postExistente();
        given(engagementClient.reactionSummary(postId, null))
                .willThrow(new ServiceUnavailableException("O servico de engajamento esta indisponivel.", null));

        mockMvc.perform(get("/api/posts/{postId}/reactions", postId))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void reacaoSemLeitor_devolve400SemChamarOMicrosservico() throws Exception {
        Long postId = postExistente();

        mockMvc.perform(post("/api/posts/{postId}/reactions", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"readerName":"","type":"CORACAO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.readerName").isNotEmpty());

        verifyNoInteractions(engagementClient);
    }
}
