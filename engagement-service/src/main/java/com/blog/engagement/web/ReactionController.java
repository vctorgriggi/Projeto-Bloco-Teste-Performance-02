package com.blog.engagement.web;

import com.blog.engagement.domain.ReactionType;
import com.blog.engagement.service.ReactionService;
import com.blog.engagement.web.dto.ReactionRequest;
import com.blog.engagement.web.dto.ReactionSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// endpoints da capacidade nova. as tres rotas trabalham sobre o mesmo recurso
// (as reacoes de um post) e todas devolvem o resumo atualizado, para o cliente
// nao precisar de uma segunda chamada depois de reagir ou desfazer.
@RestController
@RequestMapping("/api/posts/{postId}/reactions")
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    // o leitor e opcional: sem ele a resposta traz os totais e a lista "mine" vazia
    @GetMapping
    public ReactionSummaryResponse summary(@PathVariable Long postId,
                                           @RequestParam(required = false) String reader) {
        return reactionService.summaryFor(postId, reader);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReactionSummaryResponse react(@PathVariable Long postId,
                                          @Valid @RequestBody ReactionRequest request) {
        return reactionService.react(postId, request);
    }

    // desfazer e o mesmo clique na interface, por isso a rota identifica a reacao
    // pelo par (tipo, leitor) e nao pelo id -- o front nao precisa guardar id nenhum
    @DeleteMapping("/{type}")
    public ReactionSummaryResponse undo(@PathVariable Long postId,
                                         @PathVariable ReactionType type,
                                         @RequestParam String reader) {
        return reactionService.undoReaction(postId, type, reader);
    }
}
