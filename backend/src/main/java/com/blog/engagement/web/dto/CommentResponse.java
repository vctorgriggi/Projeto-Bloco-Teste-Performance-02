package com.blog.engagement.web.dto;

import com.blog.engagement.client.dto.CommentView;

import java.time.Instant;

// o comentario como a api do monolito devolve. o formato e byte a byte o mesmo de
// antes da migracao -- o front nao mudou uma linha por causa dela.
//
// o from() agora parte do que veio do microsservico, e nao mais de uma entidade
// jpa. e a unica diferenca visivel deste arquivo, e ela resume bem a mudanca: o
// monolito deixou de ser dono do dado e passou a ser dono do contrato.
public record CommentResponse(
        Long id,
        Long postId,
        String authorName,
        String content,
        Instant createdAt
) {
    public static CommentResponse from(CommentView comment) {
        return new CommentResponse(
                comment.id(),
                comment.postId(),
                comment.authorName(),
                comment.content(),
                comment.createdAt()
        );
    }
}
