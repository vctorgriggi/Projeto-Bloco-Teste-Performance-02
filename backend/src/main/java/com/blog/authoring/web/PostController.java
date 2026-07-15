package com.blog.authoring.web;

import com.blog.authoring.service.PostHistoryService;
import com.blog.authoring.service.PostService;
import com.blog.authoring.web.dto.PostRequest;
import com.blog.authoring.web.dto.PostResponse;
import com.blog.authoring.web.dto.PostRevisionResponse;
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

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final PostHistoryService postHistoryService;

    public PostController(PostService postService, PostHistoryService postHistoryService) {
        this.postService = postService;
        this.postHistoryService = postHistoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody PostRequest request) {
        return postService.create(request);
    }

    @GetMapping
    public List<PostResponse> list() {
        return postService.findAll();
    }

    @GetMapping("/{id}")
    public PostResponse get(@PathVariable Long id) {
        return postService.findById(id);
    }

    // linha do tempo das mudancas do post, da mais antiga para a mais recente
    @GetMapping("/{id}/history")
    public List<PostRevisionResponse> history(@PathVariable Long id) {
        return postHistoryService.historyOf(id);
    }

    @PutMapping("/{id}")
    public PostResponse update(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return postService.update(id, request);
    }

    // transicao de estado exposta como sub-recurso, em vez de um put generico
    @PostMapping("/{id}/publish")
    public PostResponse publish(@PathVariable Long id) {
        return postService.publish(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
