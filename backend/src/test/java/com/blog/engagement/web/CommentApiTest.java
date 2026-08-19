package com.blog.engagement.web;

import com.blog.authoring.domain.Post;
import com.blog.authoring.repository.PostRepository;
import com.blog.engagement.client.EngagementClient;
import com.blog.engagement.client.dto.CommentView;
import com.blog.engagement.client.dto.NewComment;
import com.blog.shared.exception.ResourceNotFoundException;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// a api de comentarios do monolito depois da migracao. o cliente do microsservico
// entra dublado de proposito: o que se testa aqui e o papel que sobrou para este
// servico -- validar o post, delegar, traduzir a resposta e traduzir a falha --, e nao
// o comportamento do engajamento, que tem os testes dele no proprio projeto.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentApiTest {

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

    @Test
    void getComentarios_delegaAoMicrosservicoETraduzOFormato() throws Exception {
        Long postId = postExistente();
        given(engagementClient.listComments(postId)).willReturn(List.of(
                new CommentView(10L, postId, "Carla", "primeiro", Instant.parse("2026-01-01T10:00:00Z")),
                new CommentView(11L, postId, "Diego", "segundo", Instant.parse("2026-01-01T11:00:00Z"))));

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].authorName").value("Carla"))
                .andExpect(jsonPath("$[1].content").value("segundo"));
    }

    @Test
    void postComentario_devolve201ERepassaOCorpoTraduzido() throws Exception {
        Long postId = postExistente();
        given(engagementClient.addComment(eq(postId), any(NewComment.class))).willReturn(
                new CommentView(10L, postId, "Carla", "que texto bom", Instant.parse("2026-01-01T10:00:00Z")));

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorName":"Carla","content":"que texto bom"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.authorName").value("Carla"));

        verify(engagementClient).addComment(postId, new NewComment("Carla", "que texto bom"));
    }

    // a validacao do post acontece antes de a chamada sair da maquina: e o monolito que
    // e dono do post, e nao faz sentido gastar uma ida a rede para descobrir isso
    @Test
    void comentarEmPostInexistente_devolve404SemChamarOMicrosservico() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/comments", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorName":"Carla","content":"recado"}
                                """))
                .andExpect(status().isNotFound());

        verifyNoInteractions(engagementClient);
    }

    @Test
    void listarComentariosDePostInexistente_devolve404SemChamarOMicrosservico() throws Exception {
        mockMvc.perform(get("/api/posts/{postId}/comments", 999_999L))
                .andExpect(status().isNotFound());

        verifyNoInteractions(engagementClient);
    }

    @Test
    void deleteComentario_delegaPorIdEDevolve204() throws Exception {
        mockMvc.perform(delete("/api/comments/{id}", 10L))
                .andExpect(status().isNoContent());

        verify(engagementClient).deleteComment(10L);
    }

    // o 404 vindo do microsservico continua sendo 404 aqui, e nao 500: e o
    // EngagementErrorDecoder que preserva esse significado, e o fallback que o deixa
    // passar em vez de trata-lo como queda de servico
    @Test
    void comentarioInexistenteNoMicrosservico_continua404() throws Exception {
        willThrow(new ResourceNotFoundException("Comentario 10 nao encontrado"))
                .given(engagementClient).deleteComment(10L);

        mockMvc.perform(delete("/api/comments/{id}", 10L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Comentario 10 nao encontrado"));
    }

    // quando o engajamento cai, a api responde 503 -- nao 500 e nem uma lista vazia
    // fingindo que o post nao tem conversa
    @Test
    void engajamentoForaDoAr_devolve503ComMensagemUtil() throws Exception {
        Long postId = postExistente();
        given(engagementClient.listComments(postId))
                .willThrow(new ServiceUnavailableException(
                        "O servico de engajamento esta indisponivel. Tente novamente em instantes.", null));

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value(
                        "O servico de engajamento esta indisponivel. Tente novamente em instantes."));
    }

    // a validacao do proprio dto continua sendo feita aqui, antes de qualquer rede
    @Test
    void comentarioSemConteudo_devolve400SemChamarOMicrosservico() throws Exception {
        Long postId = postExistente();

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorName":"Carla","content":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.content").isNotEmpty());

        verifyNoInteractions(engagementClient);
    }
}
