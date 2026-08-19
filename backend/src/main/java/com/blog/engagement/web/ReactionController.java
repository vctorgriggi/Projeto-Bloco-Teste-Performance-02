package com.blog.engagement.web;

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

// os endpoints novos que o front usa para falar com o microsservico. o monolito
// segue sendo a unica origem que o navegador conhece: o front nao chama o
// engajamento direto, entao nao ha uma segunda url, um segundo cors nem um segundo
// tratamento de erro na interface.
//
// e um ponto unico de entrada feito na mao, no lugar onde um dia entra um api
// gateway. a troca esta documentada em docs/MICROSSERVICO.md.
@RestController
@RequestMapping("/api/posts/{postId}/reactions")
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    // o leitor e opcional: quem so esta lendo ve os totais sem se identificar
    @GetMapping
    public ReactionSummaryResponse summary(@PathVariable Long postId,
                                           @RequestParam(required = false) String reader) {
        return ReactionSummaryResponse.from(reactionService.summaryFor(postId, reader));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReactionSummaryResponse react(@PathVariable Long postId,
                                          @Valid @RequestBody ReactionRequest request) {
        return ReactionSummaryResponse.from(reactionService.react(postId, request));
    }

    // desfazer identifica a reacao pelo par (tipo, leitor), e nao por id: o front nao
    // guarda id de reacao nenhum, so sabe qual botao o leitor apertou
    @DeleteMapping("/{type}")
    public ReactionSummaryResponse undo(@PathVariable Long postId,
                                         @PathVariable String type,
                                         @RequestParam String reader) {
        return ReactionSummaryResponse.from(reactionService.undoReaction(postId, type, reader));
    }
}
