package com.blog.engagement.service;

import com.blog.engagement.domain.Comment;
import com.blog.engagement.repository.CommentRepository;
import com.blog.engagement.shared.ResourceNotFoundException;
import com.blog.engagement.web.dto.CommentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// casos de uso de comentario dentro do microsservico.
//
// no monolito este service checava, pela porta PostCatalog, se o post existia
// antes de gravar. essa checagem nao veio junto de proposito: quem e dono do post
// e o outro servico, e a validacao passou a acontecer la, antes de a chamada
// cruzar a rede. o microsservico trata o postId como um identificador externo e
// opaco, o que mantem a dependencia em um unico sentido (monolito -> engajamento)
// e deixa este processo capaz de subir e responder sozinho.
@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public Comment addToPost(Long postId, CommentRequest request) {
        return commentRepository.save(new Comment(postId, request.authorName(), request.content()));
    }

    @Transactional(readOnly = true)
    public List<Comment> findByPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    public void delete(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario " + commentId + " nao encontrado"));
        commentRepository.delete(comment);
    }
}
