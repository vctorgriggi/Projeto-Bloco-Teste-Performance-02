package com.blog.authoring.service;

import com.blog.authoring.domain.Author;
import com.blog.authoring.repository.AuthorRepository;
import com.blog.authoring.web.dto.AuthorRequest;
import com.blog.shared.exception.BusinessRuleException;
import com.blog.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// orquestra os casos de uso de autor. concentra as regras (ex: email unico) e
// delega persistencia ao repositorio. nao conhece http nem dto de outras camadas.
@Service
@Transactional
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public Author create(AuthorRequest request) {
        if (authorRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Ja existe um autor com o email " + request.email());
        }
        Author author = new Author(request.name(), request.email(), request.bio());
        return authorRepository.save(author);
    }

    @Transactional(readOnly = true)
    public List<Author> findAll() {
        return authorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Author findById(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autor " + id + " nao encontrado"));
    }

    public Author update(Long id, AuthorRequest request) {
        Author author = findById(id);
        author.updateProfile(request.name(), request.bio());
        return author; // dentro da transacao o jpa persiste a mudanca no flush
    }

    public void delete(Long id) {
        Author author = findById(id);
        authorRepository.delete(author);
    }
}
