package com.blog.engagement.web.dto;

import com.blog.engagement.domain.Comment;

import java.time.Instant;

// o formato de saida foi mantido campo a campo igual ao do monolito antes da
// migracao. era o que o front ja consumia, e manter o contrato foi o que permitiu
// mover a entidade de processo sem mexer na tela de comentarios.
public record CommentResponse(
        Long id,
        Long postId,
        String authorName,
        String content,
        Instant createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorName(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
