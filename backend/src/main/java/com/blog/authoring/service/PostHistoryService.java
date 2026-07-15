package com.blog.authoring.service;

import com.blog.authoring.repository.PostRepository;
import com.blog.authoring.web.dto.PostRevisionResponse;
import com.blog.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// consulta o historico de um post. delega ao RevisionRepository, que devolve as
// revisoes em ordem crescente (da mais antiga para a mais recente), e converte
// cada uma no dto de resposta. um post que ja foi apagado continua tendo
// historico, o que e justamente o objetivo da auditoria.
@Service
@Transactional(readOnly = true)
public class PostHistoryService {

    private final PostRepository postRepository;

    public PostHistoryService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<PostRevisionResponse> historyOf(Long postId) {
        List<PostRevisionResponse> history = postRepository.findRevisions(postId).getContent().stream()
                .map(PostRevisionResponse::from)
                .toList();
        if (history.isEmpty()) {
            throw new ResourceNotFoundException("Post " + postId + " nao encontrado");
        }
        return history;
    }
}
