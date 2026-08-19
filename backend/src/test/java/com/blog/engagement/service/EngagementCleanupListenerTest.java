package com.blog.engagement.service;

import com.blog.authoring.repository.AuthorRepository;
import com.blog.authoring.repository.PostRepository;
import com.blog.authoring.service.AuthorService;
import com.blog.authoring.service.PostService;
import com.blog.authoring.web.dto.AuthorRequest;
import com.blog.authoring.web.dto.PostRequest;
import com.blog.engagement.client.EngagementClient;
import com.blog.engagement.client.dto.PurgeView;
import com.blog.shared.exception.ServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

// sem chave estrangeira entre os bancos, apagar um post deixaria conversa orfa no outro
// servico. a limpeza reage a um evento de dominio, e este teste cobre as duas coisas que
// importam nesse desenho: ela acontece de fato, e ela nao pode derrubar a exclusao do
// post se o outro servico estiver fora do ar.
//
// nao e transacional de proposito: o listener escuta em AFTER_COMMIT, entao um teste que
// rodasse dentro de uma transacao revertida nunca veria o evento chegar.
@SpringBootTest
@ActiveProfiles("test")
class EngagementCleanupListenerTest {

    @Autowired
    private AuthorService authorService;

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @MockBean
    private EngagementClient engagementClient;

    @AfterEach
    void limparBanco() {
        postRepository.deleteAll();
        authorRepository.deleteAll();
    }

    private Long criarPost() {
        Long autorId = authorService.create(new AuthorRequest("Ana", "ana-cleanup@blog.dev", "bio")).getId();
        return postService.create(new PostRequest("Titulo", "conteudo", autorId)).id();
    }

    @Test
    void apagarUmPost_pedeAoMicrosservicoQueLimpeOEngajamentoDaquelePost() {
        Long postId = criarPost();
        given(engagementClient.purgePost(postId)).willReturn(new PurgeView(postId, 2, 3));

        postService.delete(postId);

        verify(engagementClient).purgePost(postId);
    }

    // editar ou publicar nao dispara limpeza nenhuma: so a exclusao publica o evento
    @Test
    void publicarUmPost_naoPedeLimpezaDeEngajamento() {
        Long postId = criarPost();

        postService.publish(postId);

        verifyNoInteractions(engagementClient);
    }

    // a exclusao do post nao pode ficar refem da disponibilidade do outro servico. a
    // consistencia aqui e eventual, e a falha vai para o log em vez de virar erro na api.
    @Test
    void microsservicoForaDoAr_naoImpedeAExclusaoDoPost() {
        Long postId = criarPost();
        given(engagementClient.purgePost(postId))
                .willThrow(new ServiceUnavailableException("engajamento indisponivel", null));

        assertThatCode(() -> postService.delete(postId)).doesNotThrowAnyException();

        assertThat(postRepository.findById(postId)).isEmpty();
    }
}
