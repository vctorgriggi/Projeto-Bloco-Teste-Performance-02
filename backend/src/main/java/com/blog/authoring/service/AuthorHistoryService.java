package com.blog.authoring.service;

import com.blog.authoring.repository.AuthorRepository;
import com.blog.authoring.web.dto.AuthorRevisionResponse;
import com.blog.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// consulta o historico de mudancas do perfil de um autor via RevisionRepository,
// no mesmo formato do historico de post.
@Service
@Transactional(readOnly = true)
public class AuthorHistoryService {

    private final AuthorRepository authorRepository;

    public AuthorHistoryService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<AuthorRevisionResponse> historyOf(Long authorId) {
        List<AuthorRevisionResponse> history = authorRepository.findRevisions(authorId).getContent().stream()
                .map(AuthorRevisionResponse::from)
                .toList();
        if (history.isEmpty()) {
            throw new ResourceNotFoundException("Autor " + authorId + " nao encontrado");
        }
        return history;
    }
}
