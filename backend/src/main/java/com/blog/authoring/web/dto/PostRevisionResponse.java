package com.blog.authoring.web.dto;

import com.blog.authoring.domain.Post;
import org.springframework.data.history.Revision;

import java.time.Instant;

// uma entrada do historico de um post: alem do estado do post naquele momento,
// carrega os metadados da revisao (numero, tipo e quando aconteceu) que o envers
// registra a cada mudanca.
public record PostRevisionResponse(
        long revisionNumber,
        String revisionType,
        Instant revisionInstant,
        Long postId,
        String title,
        String content,
        Long authorId,
        String status,
        Instant createdAt,
        Instant publishedAt
) {
    public static PostRevisionResponse from(Revision<Integer, Post> revision) {
        Post post = revision.getEntity();
        return new PostRevisionResponse(
                revision.getRequiredRevisionNumber(),
                revision.getMetadata().getRevisionType().name(), // INSERT, UPDATE ou DELETE
                revision.getRevisionInstant().orElse(null),
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorId(),
                post.getStatus() != null ? post.getStatus().name() : null,
                post.getCreatedAt(),
                post.getPublishedAt()
        );
    }
}
