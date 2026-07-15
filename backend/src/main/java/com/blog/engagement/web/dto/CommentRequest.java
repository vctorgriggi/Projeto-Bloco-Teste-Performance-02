package com.blog.engagement.web.dto;

import jakarta.validation.constraints.NotBlank;

// dados para registrar um comentario. o postId nao entra aqui porque vem da
// url (/api/posts/{postId}/comments).
public record CommentRequest(
        @NotBlank String authorName,
        @NotBlank String content
) {
}
