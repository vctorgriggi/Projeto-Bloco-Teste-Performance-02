package com.blog.engagement.web;

import com.blog.engagement.service.EngagementCleanupService;
import com.blog.engagement.web.dto.PurgeResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// o engajamento de um post tratado como um recurso proprio, para poder ser removido
// de uma vez. na terceira entrega era a rota que o monolito chamava quando um post era
// apagado; desde a quarta, esse aviso chega como evento (post.deleted, pela fila), e a
// rota ficou como ferramenta de operacao -- limpar a mao um post cujo evento foi parar
// na dead letter, por exemplo. a regra e a mesma, no mesmo service.
//
// nao e uma rota que o navegador use: e um endpoint de integracao entre servicos. em
// um ambiente real ele estaria atras da rede interna ou de autenticacao entre
// servicos, e nao exposto junto das rotas publicas.
@RestController
@RequestMapping("/api/posts/{postId}/engagement")
public class PostEngagementController {

    private final EngagementCleanupService engagementCleanupService;

    public PostEngagementController(EngagementCleanupService engagementCleanupService) {
        this.engagementCleanupService = engagementCleanupService;
    }

    @DeleteMapping
    public PurgeResponse purge(@PathVariable Long postId) {
        return engagementCleanupService.purgePost(postId);
    }
}
