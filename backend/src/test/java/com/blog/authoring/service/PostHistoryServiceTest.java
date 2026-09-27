package com.blog.authoring.service;

import com.blog.authoring.repository.AuthorRepository;
import com.blog.authoring.repository.PostRepository;
import com.blog.authoring.web.dto.AuthorRequest;
import com.blog.authoring.web.dto.PostRequest;
import com.blog.authoring.web.dto.PostRevisionResponse;
import com.blog.shared.exception.ResourceNotFoundException;
import com.blog.shared.messaging.outbox.OutboxRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// testa o historico de mudancas de um post de ponta a ponta. os metodos nao sao
// transacionais de proposito: o envers so grava a revisao no commit de cada
// operacao, e cada chamada de service abre e fecha a sua propria transacao. por
// isso a limpeza acontece no @AfterEach, e nao por rollback.
@SpringBootTest
@ActiveProfiles("test")
class PostHistoryServiceTest {

    @Autowired
    private AuthorService authorService;

    @Autowired
    private PostService postService;

    @Autowired
    private PostHistoryService postHistoryService;

    @Autowired
    private PostRepository postRepository;

    // apagar um post grava o aviso para o engajamento no outbox, na mesma transacao. a
    // terceira entrega precisava dublar o cliente http aqui, porque a exclusao chamava a
    // rede; desde a quarta, a exclusao nao sai da maquina, e o duble deixou de ser
    // necessario. a limpeza do outbox e so higiene entre testes.
    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @AfterEach
    void limparBanco() {
        outboxRepository.deleteAll();
        postRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Test
    void cicloDeVidaDoPost_ficaRegistradoNoHistorico() {
        Long autorId = authorService.create(new AuthorRequest("Ana", "ana-post-hist@blog.dev", "bio")).getId();

        Long postId = postService.create(new PostRequest("Titulo original", "Conteudo v1", autorId)).id();
        postService.update(postId, new PostRequest("Titulo editado", "Conteudo v2", autorId));
        postService.publish(postId);
        postService.delete(postId);

        List<PostRevisionResponse> historico = postHistoryService.historyOf(postId);

        // uma revisao por operacao: criar, editar, publicar e apagar
        assertThat(historico)
                .extracting(PostRevisionResponse::revisionType)
                .containsExactly("INSERT", "UPDATE", "UPDATE", "DELETE");

        assertThat(historico.get(0).title()).isEqualTo("Titulo original");
        assertThat(historico.get(0).status()).isEqualTo("DRAFT");

        assertThat(historico.get(1).title()).isEqualTo("Titulo editado");
        assertThat(historico.get(1).content()).isEqualTo("Conteudo v2");

        assertThat(historico.get(2).status()).isEqualTo("PUBLISHED");
        assertThat(historico.get(2).publishedAt()).isNotNull();

        // com store_data_at_delete o estado anterior a exclusao fica preservado
        assertThat(historico.get(3).title()).isEqualTo("Titulo editado");

        assertThat(historico).allSatisfy(revisao -> {
            assertThat(revisao.revisionInstant()).isNotNull();
            assertThat(revisao.revisionNumber()).isPositive();
        });
    }

    @Test
    void historicoDePostInexistente_lancaNotFound() {
        assertThatThrownBy(() -> postHistoryService.historyOf(999_999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
