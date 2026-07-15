package com.blog.authoring.web.dto;

import com.blog.authoring.domain.Post;

import java.time.Instant;

// resposta de um post enriquecida com o nome do autor. o nome vem de uma busca
// no aggregate de autor feita pelo service, ja que o post so guarda o authorId.
public record PostResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        String authorName,
        String status,
        Instant createdAt,
        Instant publishedAt
) {
    public static PostResponse from(Post post, String authorName) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorId(),
                authorName,
                post.getStatus().name(),
                post.getCreatedAt(),
                post.getPublishedAt()
        );
    }
}
