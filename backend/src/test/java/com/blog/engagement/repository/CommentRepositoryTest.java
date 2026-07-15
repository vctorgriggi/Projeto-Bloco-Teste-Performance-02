package com.blog.engagement.repository;

import com.blog.engagement.domain.Comment;
import com.blog.shared.config.PersistenceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// testes da persistencia do comentario: a consulta derivada deve trazer apenas os
// comentarios do post pedido e em ordem cronologica.
@DataJpaTest
@Import(PersistenceConfig.class)
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void findByPostId_filtraPeloPostEOrdenaPorData() throws InterruptedException {
        commentRepository.saveAndFlush(new Comment(1L, "Carla", "primeiro recado"));
        Thread.sleep(10);
        commentRepository.saveAndFlush(new Comment(1L, "Diego", "segundo recado"));
        Thread.sleep(10);
        commentRepository.saveAndFlush(new Comment(2L, "Ana", "recado de outro post"));

        List<Comment> doPrimeiroPost = commentRepository.findByPostIdOrderByCreatedAtAsc(1L);

        assertThat(doPrimeiroPost)
                .extracting(Comment::getAuthorName)
                .containsExactly("Carla", "Diego");
    }
}
