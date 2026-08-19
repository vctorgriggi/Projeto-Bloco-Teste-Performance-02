package com.blog.engagement.service;

import com.blog.engagement.client.EngagementClient;
import com.blog.engagement.client.dto.PurgeView;
import com.blog.shared.event.PostDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// apagar um post deixa comentarios e reacoes orfaos no outro banco. enquanto tudo
// morava junto, uma chave estrangeira resolveria; entre servicos nao existe chave
// estrangeira possivel, e a coerencia passa a ser responsabilidade da aplicacao.
//
// a limpeza reage a um evento em vez de ser chamada pelo PostService, e isso mantem
// a direcao das dependencias: authoring publica que um post foi apagado e nao sabe
// quem escuta; quem se interessa por engajamento vive neste pacote.
//
// duas decisoes deliberadas:
//
// - AFTER_COMMIT: a limpeza so acontece depois de a exclusao do post ter sido
//   confirmada no banco. se a transacao voltasse atras, nao teriamos apagado o
//   engajamento de um post que continua existindo.
// - melhor esforco: se o microsservico estiver fora do ar, a falha e registrada no
//   log e o post continua apagado. o alternativo seria recusar a exclusao do post
//   por indisponibilidade de outro servico, o que e pior. a consistencia aqui e
//   eventual, e o caminho de verdade para fecha-la e uma mensagem persistente
//   (outbox + broker) que possa ser reentregue -- fora do escopo desta entrega,
//   mas e exatamente o ponto onde ela entraria.
@Component
public class EngagementCleanupListener {

    private static final Logger log = LoggerFactory.getLogger(EngagementCleanupListener.class);

    private final EngagementClient engagementClient;

    public EngagementCleanupListener(EngagementClient engagementClient) {
        this.engagementClient = engagementClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostDeleted(PostDeletedEvent event) {
        try {
            PurgeView removido = engagementClient.purgePost(event.postId());
            log.info("engajamento do post {} limpo: {} comentario(s) e {} reacao(oes)",
                    event.postId(), removido.comments(), removido.reactions());
        } catch (RuntimeException e) {
            log.warn("nao foi possivel limpar o engajamento do post {}: {}", event.postId(), e.getMessage());
        }
    }
}
