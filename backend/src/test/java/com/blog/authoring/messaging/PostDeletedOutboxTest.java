package com.blog.authoring.messaging;

import com.blog.authoring.repository.AuthorRepository;
import com.blog.authoring.repository.PostRepository;
import com.blog.authoring.service.AuthorService;
import com.blog.authoring.service.PostService;
import com.blog.authoring.web.dto.AuthorRequest;
import com.blog.authoring.web.dto.PostRequest;
import com.blog.shared.exception.ResourceNotFoundException;
import com.blog.shared.messaging.outbox.OutboxMessage;
import com.blog.shared.messaging.outbox.OutboxRepository;
import com.blog.shared.messaging.outbox.OutboxWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// o lado de dentro do outbox: a exclusao de um post grava a mensagem de integracao no
// mesmo commit. nenhum broker e necessario aqui -- e esse o ponto do padrao: o fato fica
// garantido no banco, e sair para o broker e problema do relay, testado a parte.
//
// substitui o teste da terceira entrega que cobria a chamada http de limpeza. o cenario
// que ele protegia ("o microsservico fora do ar nao impede a exclusao do post") deixou
// de ser um caso especial: a exclusao nao fala com rede nenhuma.
@SpringBootTest
@ActiveProfiles("test")
class PostDeletedOutboxTest {

    @Autowired
    private AuthorService authorService;

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxWriter outboxWriter;

    @BeforeEach
    @AfterEach
    void limparBanco() {
        outboxRepository.deleteAll();
        postRepository.deleteAll();
        authorRepository.deleteAll();
    }

    private Long criarPost() {
        Long autorId = authorService.create(new AuthorRequest("Ana", "ana-outbox@blog.dev", "bio")).getId();
        return postService.create(new PostRequest("Titulo", "conteudo", autorId)).id();
    }

    @Test
    void apagarUmPost_gravaOEventoNoOutboxComoPendente() {
        Long postId = criarPost();

        postService.delete(postId);

        List<OutboxMessage> pendentes = outboxRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
        assertThat(pendentes).hasSize(1);
        OutboxMessage mensagem = pendentes.get(0);
        assertThat(mensagem.getExchange()).isEqualTo("blog.posts");
        assertThat(mensagem.getRoutingKey()).isEqualTo("post.deleted");
        assertThat(mensagem.getType()).isEqualTo("post.deleted");
        assertThat(mensagem.getPayload()).contains("\"postId\":" + postId).contains("deletedAt");
        assertThat(mensagem.isPending()).isTrue();
    }

    // editar ou publicar nao sao fatos que alguem assinou; so a exclusao vira mensagem
    @Test
    void publicarUmPost_naoGravaNadaNoOutbox() {
        Long postId = criarPost();

        postService.publish(postId);

        assertThat(outboxRepository.count()).isZero();
    }

    // a exclusao que falha nao deixa mensagem para tras: a linha do outbox e a exclusao
    // entram (ou nao entram) juntas
    @Test
    void exclusaoQueFalha_naoDeixaMensagemNoOutbox() {
        assertThatThrownBy(() -> postService.delete(999_999L)).isInstanceOf(ResourceNotFoundException.class);

        assertThat(outboxRepository.count()).isZero();
    }

    // gravar no outbox fora de uma transacao seria uma escrita independente -- o problema
    // que o padrao existe para evitar. o MANDATORY transforma esse erro de uso em excecao
    @Test
    void gravarNoOutboxForaDeTransacao_eRecusado() {
        assertThatThrownBy(() -> outboxWriter.record("blog.posts", "post.deleted", "post.deleted",
                new PostDeletedMessage(1L, null)))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
