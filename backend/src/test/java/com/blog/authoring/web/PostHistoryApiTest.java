package com.blog.authoring.web;

import com.blog.authoring.repository.AuthorRepository;
import com.blog.authoring.repository.PostRepository;
import com.blog.authoring.service.AuthorService;
import com.blog.authoring.service.PostService;
import com.blog.authoring.web.dto.AuthorRequest;
import com.blog.authoring.web.dto.PostRequest;
import com.blog.engagement.repository.CommentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// exercita o endpoint de consulta de historico de ponta a ponta, do http ate a
// tabela de auditoria, confirmando o formato da resposta e o 404 de post ausente.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostHistoryApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthorService authorService;

    @Autowired
    private PostService postService;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @AfterEach
    void limparBanco() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Test
    void getHistory_devolveAsRevisoesDoPost() throws Exception {
        Long autorId = authorService.create(new AuthorRequest("Ana", "ana-api-hist@blog.dev", "bio")).getId();
        Long postId = postService.create(new PostRequest("Titulo", "conteudo v1", autorId)).id();
        postService.update(postId, new PostRequest("Titulo editado", "conteudo v2", autorId));

        mockMvc.perform(get("/api/posts/{id}/history", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].revisionType").value("INSERT"))
                .andExpect(jsonPath("$[0].title").value("Titulo"))
                .andExpect(jsonPath("$[1].revisionType").value("UPDATE"))
                .andExpect(jsonPath("$[1].title").value("Titulo editado"));
    }

    @Test
    void getHistory_dePostInexistente_devolve404() throws Exception {
        mockMvc.perform(get("/api/posts/{id}/history", 999_999L))
                .andExpect(status().isNotFound());
    }
}
