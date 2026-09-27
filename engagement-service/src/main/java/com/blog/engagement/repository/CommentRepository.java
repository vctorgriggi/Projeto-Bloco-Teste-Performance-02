package com.blog.engagement.repository;

import com.blog.engagement.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// repositorio dedicado do microsservico. e o mesmo de antes, so que agora ele
// nao divide o banco com posts e autores: aponta para o schema proprio do
// servico de engajamento.
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // apoiada no indice de post_id, e a consulta da listagem de um post
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    // contagem usada no resumo de engajamento, sem carregar os comentarios
    long countByPostId(Long postId);

    // usada na limpeza de um post apagado no outro servico. o spring data implementa
    // um deleteBy derivado carregando as entidades e removendo uma a uma, e nao com
    // um delete em massa -- e isso mantem a auditoria do envers funcionando, com uma
    // revisao de exclusao por comentario.
    long deleteByPostId(Long postId);

    // sustenta o consumo idempotente do comando de comentario: a mesma mensagem entregue
    // de novo e reconhecida antes do insert
    boolean existsBySubmissionId(String submissionId);

    // total de comentarios de cada post que tem conversa, numa consulta so. e o que o
    // servico usa para republicar o estado de todos os posts quando alguem pede.
    @Query("select c.postId as postId, count(c) as total from Comment c group by c.postId")
    List<PostTotal> countGroupedByPost();
}
