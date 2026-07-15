package com.blog.authoring.service;

import com.blog.authoring.repository.PostRepository;
import com.blog.engagement.service.PostCatalog;
import org.springframework.stereotype.Component;

// adaptador que liga a porta PostCatalog (definida no engajamento) ao
// repositorio de posts de authoring. e o unico ponto de contato entre os dois
// contextos, o que mantem o acoplamento explicito e controlado.
@Component
public class PostCatalogAdapter implements PostCatalog {

    private final PostRepository postRepository;

    public PostCatalogAdapter(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public boolean postExists(Long postId) {
        return postRepository.existsById(postId);
    }
}
