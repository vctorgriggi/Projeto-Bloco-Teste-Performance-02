package com.blog.authoring.web;

import com.blog.authoring.service.AuthorHistoryService;
import com.blog.authoring.service.AuthorService;
import com.blog.authoring.web.dto.AuthorRequest;
import com.blog.authoring.web.dto.AuthorResponse;
import com.blog.authoring.web.dto.AuthorRevisionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// adapta http para os casos de uso de autor. nao contem regra de negocio: so
// recebe a requisicao, chama o service e devolve o dto de resposta.
@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    private final AuthorService authorService;
    private final AuthorHistoryService authorHistoryService;

    public AuthorController(AuthorService authorService, AuthorHistoryService authorHistoryService) {
        this.authorService = authorService;
        this.authorHistoryService = authorHistoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorResponse create(@Valid @RequestBody AuthorRequest request) {
        return AuthorResponse.from(authorService.create(request));
    }

    @GetMapping
    public List<AuthorResponse> list() {
        return authorService.findAll().stream().map(AuthorResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AuthorResponse get(@PathVariable Long id) {
        return AuthorResponse.from(authorService.findById(id));
    }

    // historico de mudancas do perfil do autor, da mais antiga para a mais recente
    @GetMapping("/{id}/history")
    public List<AuthorRevisionResponse> history(@PathVariable Long id) {
        return authorHistoryService.historyOf(id);
    }

    @PutMapping("/{id}")
    public AuthorResponse update(@PathVariable Long id, @Valid @RequestBody AuthorRequest request) {
        return AuthorResponse.from(authorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
