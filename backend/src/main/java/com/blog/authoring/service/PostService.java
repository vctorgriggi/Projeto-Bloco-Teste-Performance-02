package com.blog.authoring.service;

import com.blog.authoring.domain.Author;
import com.blog.authoring.domain.Post;
import com.blog.authoring.repository.AuthorRepository;
import com.blog.authoring.repository.PostRepository;
import com.blog.authoring.web.dto.PostRequest;
import com.blog.authoring.web.dto.PostResponse;
import com.blog.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// casos de uso de post. monta o PostResponse enriquecido com o nome do autor,
// resolvendo a referencia por id entre os aggregates Post e Author.
@Service
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final AuthorRepository authorRepository;

    public PostService(PostRepository postRepository, AuthorRepository authorRepository) {
        this.postRepository = postRepository;
        this.authorRepository = authorRepository;
    }

    public PostResponse create(PostRequest request) {
        requireAuthor(request.authorId());
        Post post = new Post(request.title(), request.content(), request.authorId());
        return toResponse(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public List<PostResponse> findAll() {
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PostResponse findById(Long id) {
        return toResponse(getPost(id));
    }

    public PostResponse update(Long id, PostRequest request) {
        Post post = getPost(id);
        post.edit(request.title(), request.content());
        return toResponse(post);
    }

    public PostResponse publish(Long id) {
        Post post = getPost(id);
        post.publish(); // a regra de "ja publicado" vive na entidade
        return toResponse(post);
    }

    public void delete(Long id) {
        Post post = getPost(id);
        postRepository.delete(post);
    }

    private Post getPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post " + id + " nao encontrado"));
    }

    private void requireAuthor(Long authorId) {
        if (!authorRepository.existsById(authorId)) {
            throw new ResourceNotFoundException("Autor " + authorId + " nao encontrado");
        }
    }

    // busca o nome do autor para compor a resposta. usa um rotulo neutro caso o
    // autor tenha sido removido depois que o post foi criado.
    private PostResponse toResponse(Post post) {
        String authorName = authorRepository.findById(post.getAuthorId())
                .map(Author::getName)
                .orElse("Autor removido");
        return PostResponse.from(post, authorName);
    }
}
