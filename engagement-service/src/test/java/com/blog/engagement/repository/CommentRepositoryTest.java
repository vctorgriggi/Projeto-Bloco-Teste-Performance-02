package com.blog.engagement.repository;

import com.blog.engagement.domain.Comment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// o teste de persistencia do comentario acompanhou a entidade na mudanca de
// processo. ele nao precisa mais importar a configuracao de repositorios do envers
// (o microsservico nao expoe consulta de revisao), mas continua verificando o que
// importa: a consulta derivada filtra pelo post pedido e ordena por data.
@DataJpaTest
@ActiveProfiles("test")
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

    @Test
    void countByPostId_contaSoOsComentariosDaquelePost() {
        commentRepository.saveAndFlush(new Comment(1L, "Carla", "um"));
        commentRepository.saveAndFlush(new Comment(1L, "Diego", "dois"));
        commentRepository.saveAndFlush(new Comment(2L, "Ana", "de outro post"));

        assertThat(commentRepository.countByPostId(1L)).isEqualTo(2);
        assertThat(commentRepository.countByPostId(99L)).isZero();
    }
}
