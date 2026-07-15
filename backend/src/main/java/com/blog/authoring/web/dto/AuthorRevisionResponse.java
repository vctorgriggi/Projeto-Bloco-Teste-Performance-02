package com.blog.authoring.web.dto;

import com.blog.authoring.domain.Author;
import org.springframework.data.history.Revision;

import java.time.Instant;

// uma entrada do historico de um autor, com o estado do perfil naquela revisao e
// os metadados que o envers registra a cada mudanca.
public record AuthorRevisionResponse(
        long revisionNumber,
        String revisionType,
        Instant revisionInstant,
        Long authorId,
        String name,
        String email,
        String bio
) {
    public static AuthorRevisionResponse from(Revision<Integer, Author> revision) {
        Author author = revision.getEntity();
        return new AuthorRevisionResponse(
                revision.getRequiredRevisionNumber(),
                revision.getMetadata().getRevisionType().name(),
                revision.getRevisionInstant().orElse(null),
                author.getId(),
                author.getName(),
                author.getEmail(),
                author.getBio()
        );
    }
}
