package com.blog.authoring.web.dto;

import com.blog.authoring.domain.Author;

// representacao do autor devolvida pela api. converte a entidade de dominio em
// um contrato estavel, sem expor a entidade direto.
public record AuthorResponse(
        Long id,
        String name,
        String email,
        String bio
) {
    public static AuthorResponse from(Author author) {
        return new AuthorResponse(author.getId(), author.getName(), author.getEmail(), author.getBio());
    }
}
