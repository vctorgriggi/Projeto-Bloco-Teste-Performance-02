package com.blog.engagement.messaging;

import com.blog.engagement.messaging.message.PostDeletedMessage;
import com.blog.engagement.service.EngagementCleanupService;
import com.blog.engagement.shared.InvalidMessageException;
import com.blog.engagement.web.dto.PurgeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.blog.engagement.messaging.Topology.POST_DELETED_QUEUE;

// assina o evento post.deleted e limpa a conversa e as reacoes do post apagado.
//
// na terceira entrega era o monolito que chamava este servico por http depois de apagar
// o post, por melhor esforco: se este servico estivesse fora do ar naquele instante, o
// engajamento ficava orfao para sempre. agora o monolito so anuncia o fato, e a fila
// guarda a mensagem ate este servico voltar e processar. a janela de inconsistencia
// continua existindo, mas deixou de ser permanente -- dura o tempo de este servico
// estar fora.
//
// a direcao da dependencia tambem inverteu: o monolito nao sabe mais que este servico
// existe para fins de limpeza. quem decidiu que "post apagado" importa aqui foi este
// servico, ao assinar a routing key.
@Component
public class PostDeletedListener {

    private static final Logger log = LoggerFactory.getLogger(PostDeletedListener.class);

    private final EngagementCleanupService cleanupService;

    public PostDeletedListener(EngagementCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @RabbitListener(queues = POST_DELETED_QUEUE, id = "post-deleted")
    public void onPostDeleted(PostDeletedMessage event) {
        if (event.postId() == null) {
            throw new InvalidMessageException("evento post.deleted sem postId");
        }
        PurgeResponse removido = cleanupService.purgePost(event.postId());
        log.info("post {} apagado no monolito: {} comentario(s) e {} reacao(oes) removidos aqui",
                event.postId(), removido.comments(), removido.reactions());
    }
}
