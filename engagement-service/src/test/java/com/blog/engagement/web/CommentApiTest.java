package com.blog.engagement.web;

import com.blog.engagement.domain.Comment;
import com.blog.engagement.repository.CommentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// a api de comentarios do microsservico, exercitada de ponta a ponta. o valor
// deste teste e garantir que o contrato que o monolito repassa continua igual ao
// que existia antes da migracao -- e por isso que o front nao precisou mudar.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentRepository commentRepository;

    @AfterEach
    void limparBanco() {
        commentRepository.deleteAll();
    }

    @Test
    void postComentario_devolve201ComOFormatoEsperado() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/comments", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorName":"Carla","content":"otimo texto"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.postId").value(7))
                .andExpect(jsonPath("$.authorName").value("Carla"))
                .andExpect(jsonPath("$.content").value("otimo texto"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void getComentarios_listaSoOsDoPostEmOrdemCronologica() throws Exception {
        commentRepository.save(new Comment(7L, "Carla", "primeiro"));
        commentRepository.save(new Comment(7L, "Diego", "segundo"));
        commentRepository.save(new Comment(8L, "Ana", "de outro post"));

        mockMvc.perform(get("/api/posts/{postId}/comments", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].authorName").value("Carla"))
                .andExpect(jsonPath("$[1].authorName").value("Diego"));
    }

    @Test
    void postComentarioSemConteudo_devolve400ComOsCamposComProblema() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/comments", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorName":"Carla","content":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.content").isNotEmpty());
    }

    @Test
    void deleteComentario_devolve204ERemoveDoBanco() throws Exception {
        Comment comentario = commentRepository.save(new Comment(7L, "Carla", "para apagar"));

        mockMvc.perform(delete("/api/comments/{id}", comentario.getId()))
                .andExpect(status().isNoContent());

        assertThat(commentRepository.findById(comentario.getId())).isEmpty();
    }

    @Test
    void deleteDeComentarioInexistente_devolve404() throws Exception {
        mockMvc.perform(delete("/api/comments/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // um postId que nao existe no monolito e aceito aqui: quem valida a existencia
    // do post e o servico dono do post, antes de a chamada cruzar a rede. o teste
    // registra essa decisao para que ela nao pareca um esquecimento.
    @Test
    void comentarioEmPostDesconhecido_eAceito_poisAValidacaoEDoOutroServico() throws Exception {
        mockMvc.perform(post("/api/posts/{postId}/comments", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorName":"Carla","content":"recado solto"}
                                """))
                .andExpect(status().isCreated());
    }
}
