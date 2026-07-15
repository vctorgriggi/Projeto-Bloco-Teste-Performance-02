package com.blog.engagement.web.dto;

import com.blog.engagement.domain.Comment;

import java.time.Instant;

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
