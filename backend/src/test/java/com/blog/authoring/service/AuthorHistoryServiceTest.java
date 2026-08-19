package com.blog.authoring.service;

import com.blog.authoring.repository.AuthorRepository;
import com.blog.authoring.repository.PostRepository;
import com.blog.authoring.web.dto.AuthorRequest;
import com.blog.authoring.web.dto.AuthorRevisionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// mesma ideia do historico de post, aplicada as edicoes de perfil do autor.
@SpringBootTest
@ActiveProfiles("test")
class AuthorHistoryServiceTest {

    @Autowired
    private AuthorService authorService;

    @Autowired
    private AuthorHistoryService authorHistoryService;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private PostRepository postRepository;

    @AfterEach
    void limparBanco() {
        postRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Test
    void edicoesDePerfil_ficamNoHistorico() {
        Long autorId = authorService.create(new AuthorRequest("Nome v1", "autor-hist@blog.dev", "bio v1")).getId();
        authorService.update(autorId, new AuthorRequest("Nome v2", "autor-hist@blog.dev", "bio v2"));

        List<AuthorRevisionResponse> historico = authorHistoryService.historyOf(autorId);

        assertThat(historico)
                .extracting(AuthorRevisionResponse::revisionType)
                .containsExactly("INSERT", "UPDATE");

        assertThat(historico.get(0).name()).isEqualTo("Nome v1");
        assertThat(historico.get(0).bio()).isEqualTo("bio v1");

        assertThat(historico.get(1).name()).isEqualTo("Nome v2");
        assertThat(historico.get(1).bio()).isEqualTo("bio v2");
    }
}
