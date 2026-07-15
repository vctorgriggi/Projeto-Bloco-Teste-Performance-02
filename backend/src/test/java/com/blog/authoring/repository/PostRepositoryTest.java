package com.blog.authoring.repository;

import com.blog.authoring.domain.Post;
import com.blog.authoring.domain.PostStatus;
import com.blog.shared.config.PersistenceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// testes da persistencia do post: os valores padrao ao criar, a transicao de
// publicacao, a ordenacao da consulta derivada e o travamento otimista.
@DataJpaTest
@Import(PersistenceConfig.class)
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    void novoPost_nasceComoRascunho() {
        Post post = postRepository.saveAndFlush(new Post("Titulo", "Conteudo", 1L));

        assertThat(post.getStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(post.getCreatedAt()).isNotNull();
        assertThat(post.getPublishedAt()).isNull();
        assertThat(post.getVersion()).isZero();
    }

    @Test
    void publish_marcaStatusEData() {
        Post post = postRepository.saveAndFlush(new Post("Titulo", "Conteudo", 1L));

        post.publish();
        postRepository.saveAndFlush(post);

        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(post.getPublishedAt()).isNotNull();
        assertThat(post.getVersion()).isEqualTo(1L);
    }

    @Test
    void findAllByOrderByCreatedAtDesc_trazMaisRecentesPrimeiro() throws InterruptedException {
        postRepository.saveAndFlush(new Post("Primeiro", "c", 1L));
        Thread.sleep(10); // garante createdAt distintos para a ordenacao ser deterministica
        postRepository.saveAndFlush(new Post("Segundo", "c", 1L));
        Thread.sleep(10);
        postRepository.saveAndFlush(new Post("Terceiro", "c", 1L));

        List<String> titulos = postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(Post::getTitle)
                .toList();

        assertThat(titulos).containsExactly("Terceiro", "Segundo", "Primeiro");
    }
}
