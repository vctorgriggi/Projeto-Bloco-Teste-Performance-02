package com.blog.engagement.service;

import com.blog.engagement.client.EngagementClient;
import com.blog.engagement.client.dto.NewReaction;
import com.blog.engagement.client.dto.ReactionSummaryView;
import com.blog.engagement.web.dto.ReactionRequest;
import com.blog.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

// a capacidade nova vista pelo monolito. o desenho e o mesmo do comentario: valida
// o post, que e o dado desta casa, e delega a reacao a quem e dono dela.
//
// note o que este service nao faz: nao conhece os tipos de reacao, nao sabe que um
// leitor so pode reagir uma vez de cada jeito, nao conta nada. essas regras vivem
// inteiras no microsservico, e e isso que separacao de responsabilidades significa
// aqui -- se amanha entrar um quarto tipo de reacao, nenhum arquivo do monolito muda.
@Service
public class ReactionService {

    private final EngagementClient engagementClient;
    private final PostCatalog postCatalog;

    public ReactionService(EngagementClient engagementClient, PostCatalog postCatalog) {
        this.engagementClient = engagementClient;
        this.postCatalog = postCatalog;
    }

    public ReactionSummaryView summaryFor(Long postId, String reader) {
        requirePost(postId);
        return engagementClient.reactionSummary(postId, reader);
    }

    public ReactionSummaryView react(Long postId, ReactionRequest request) {
        requirePost(postId);
        return engagementClient.react(postId, new NewReaction(request.readerName(), request.type()));
    }

    public ReactionSummaryView undoReaction(Long postId, String type, String reader) {
        requirePost(postId);
        return engagementClient.undoReaction(postId, type, reader);
    }

    private void requirePost(Long postId) {
        if (!postCatalog.postExists(postId)) {
            throw new ResourceNotFoundException("Post " + postId + " nao encontrado");
        }
    }
}
