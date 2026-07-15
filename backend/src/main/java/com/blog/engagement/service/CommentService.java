package com.blog.engagement.service;

import com.blog.engagement.domain.Comment;
import com.blog.engagement.repository.CommentRepository;
import com.blog.engagement.web.dto.CommentRequest;
import com.blog.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// casos de uso de comentario. antes de gravar, confirma que o post existe
// atraves da porta PostCatalog, sem alcancar o repositorio do outro contexto.
@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostCatalog postCatalog;

    public CommentService(CommentRepository commentRepository, PostCatalog postCatalog) {
        this.commentRepository = commentRepository;
        this.postCatalog = postCatalog;
    }

    public Comment addToPost(Long postId, CommentRequest request) {
        if (!postCatalog.postExists(postId)) {
            throw new ResourceNotFoundException("Post " + postId + " nao encontrado");
        }
        Comment comment = new Comment(postId, request.authorName(), request.content());
        return commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<Comment> findByPost(Long postId) {
        if (!postCatalog.postExists(postId)) {
            throw new ResourceNotFoundException("Post " + postId + " nao encontrado");
        }
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    public void delete(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario " + commentId + " nao encontrado"));
        commentRepository.delete(comment);
    }
}
