package com.blog.authoring.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// dados para criar ou editar um post. authorId so e usado na criacao; na edicao
// o post ja pertence a um autor.
public record PostRequest(
        @NotBlank String title,
        @NotBlank String content,
        @NotNull Long authorId
) {
}
