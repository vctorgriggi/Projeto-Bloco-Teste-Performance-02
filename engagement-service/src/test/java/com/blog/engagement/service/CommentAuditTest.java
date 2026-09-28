package com.blog.engagement.service;

import com.blog.engagement.domain.Comment;
import com.blog.engagement.repository.CommentRepository;
import com.blog.engagement.web.dto.CommentRequest;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// a trilha de auditoria do comentario existia na segunda entrega e nao podia se
// perder na mudanca de processo. este teste confirma que o envers continua gravando
// em comments_AUD dentro do microsservico, inclusive a revisao de exclusao com o
// estado preservado (store_data_at_delete).
//
// nao e transacional de proposito: o envers so grava a revisao no commit, e cada
// chamada de service abre e fecha a sua propria transacao.
@SpringBootTest
@ActiveProfiles("test")
class CommentAuditTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @AfterEach
    void limparBanco() {
        commentRepository.deleteAll();
    }

    @Test
    void criarEApagarUmComentario_deixaTrilhaDeAuditoria() {
        Comment comentario = commentService.addToPost(1L, new CommentRequest("Carla", "primeiro recado"));
        Long comentarioId = comentario.getId();

        commentService.delete(comentarioId);

        AuditReader auditReader = AuditReaderFactory.get(entityManagerFactory.createEntityManager());

        List<Number> revisoes = auditReader.getRevisions(Comment.class, comentarioId);
        assertThat(revisoes).hasSize(2); // insert e delete

        @SuppressWarnings("unchecked")
        List<Object[]> entradas = auditReader.createQuery()
                .forRevisionsOfEntity(Comment.class, false, true)
                .add(AuditEntity.id().eq(comentarioId))
                .getResultList();

        assertThat(entradas).hasSize(2);
        assertThat(entradas.get(0)[2]).isEqualTo(RevisionType.ADD);
        assertThat(entradas.get(1)[2]).isEqualTo(RevisionType.DEL);

        // com store_data_at_delete ligado, a revisao de exclusao guarda o conteudo
        // que o comentario tinha antes de sumir
        Comment estadoNaExclusao = (Comment) entradas.get(1)[0];
        assertThat(estadoNaExclusao.getAuthorName()).isEqualTo("Carla");
        assertThat(estadoNaExclusao.getContent()).isEqualTo("primeiro recado");
    }

    // o recado longo que quebrava a auditoria: com @Lob, a comments_AUD nascia com
    // varchar(255), e o comentario inteiro falhava ao gravar. o defeito veio junto com a
    // entidade desde a segunda entrega e so apareceu na quinta, ao gerar o schema do
    // postgres
    @Test
    void comentarioLongo_eGravadoEAuditadoInteiro() {
        String recadoLongo = "concordo com cada ponto do texto. ".repeat(40);

        Comment comentario = commentService.addToPost(1L, new CommentRequest("Diego", recadoLongo));

        AuditReader auditReader = AuditReaderFactory.get(entityManagerFactory.createEntityManager());
        Comment auditado = auditReader.find(Comment.class, comentario.getId(),
                auditReader.getRevisions(Comment.class, comentario.getId()).get(0));
        assertThat(auditado.getContent()).isEqualTo(recadoLongo);
    }
}
