package com.blog.engagement.service;

import com.blog.engagement.domain.Comment;
import com.blog.engagement.repository.CommentRepository;
import com.blog.engagement.shared.ResourceNotFoundException;
import com.blog.engagement.web.dto.CommentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

// casos de uso de comentario dentro do microsservico.
//
// no monolito este service checava, pela porta PostCatalog, se o post existia
// antes de gravar. essa checagem nao veio junto de proposito: quem e dono do post
// e o outro servico, e a validacao passou a acontecer la, antes de a chamada
// cruzar a rede. o microsservico trata o postId como um identificador externo e
// opaco, o que mantem a dependencia em um unico sentido (monolito -> engajamento)
// e deixa este processo capaz de subir e responder sozinho.
//
// na quarta entrega o comentario passou a ter duas portas de entrada: a api http deste
// servico (addToPost) e o comando que chega pela fila (register), que e o caminho que o
// monolito usa. as duas terminam no mesmo lugar e anunciam a mesma mudanca.
@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final EngagementChanges engagementChanges;

    public CommentService(CommentRepository commentRepository, EngagementChanges engagementChanges) {
        this.commentRepository = commentRepository;
        this.engagementChanges = engagementChanges;
    }

    public Comment addToPost(Long postId, CommentRequest request) {
        Comment comment = commentRepository.save(new Comment(postId, request.authorName(), request.content()));
        engagementChanges.announce(EngagementChanged.COMMENT_ADDED, postId);
        return comment;
    }

    // registra um comentario que chegou por mensagem. e idempotente: o broker entrega
    // pelo menos uma vez, e nao exatamente uma, entao a mesma mensagem pode chegar de
    // novo (uma reconexao antes da confirmacao, um reenvio manual da dead letter). um
    // submissionId que ja existe e reconhecido e ignorado, e o resultado vazio diz isso a
    // quem chamou. a restricao de unicidade no banco cobre a corrida entre duas entregas
    // simultaneas.
    public Optional<Comment> register(String submissionId, Long postId, String authorName, String content,
                                      Instant submittedAt) {
        if (commentRepository.existsBySubmissionId(submissionId)) {
            return Optional.empty();
        }
        Comment comment = commentRepository.save(new Comment(postId, authorName, content, submissionId, submittedAt));
        engagementChanges.announce(EngagementChanged.COMMENT_ADDED, postId);
        return Optional.of(comment);
    }

    @Transactional(readOnly = true)
    public List<Comment> findByPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    public void delete(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario " + commentId + " nao encontrado"));
        commentRepository.delete(comment);
        engagementChanges.announce(EngagementChanged.COMMENT_REMOVED, comment.getPostId());
    }
}
