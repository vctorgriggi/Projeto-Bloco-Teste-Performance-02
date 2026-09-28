package com.blog.engagement.messaging;

import com.blog.engagement.messaging.message.EngagementSnapshotMessage;
import com.blog.engagement.service.EngagementChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static com.blog.engagement.messaging.Topology.ENGAGEMENT_EXCHANGE;

// transforma o anuncio interno de mudanca em mensagem no exchange blog.engagement.
//
// AFTER_COMMIT: so publica depois de o dado estar gravado. publicar antes arriscaria
// anunciar um comentario cuja transacao voltou atras. o fallbackExecution cobre o caso
// de o anuncio ser feito fora de transacao (a republicacao do estado, por exemplo).
//
// a publicacao e por melhor esforco, sem outbox, e a escolha e consciente. o que esta
// mensagem carrega e um estado derivado (contagens) que se corrige sozinho: a proxima
// mudanca no mesmo post traz o total certo, e o monolito pode pedir a republicacao de
// tudo a qualquer momento. um fato unico e irrecuperavel, como "o post foi apagado",
// mereceu outbox no monolito; um contador que se refaz, nao. se o broker estiver fora,
// o comentario continua gravado e a falha vai para o log -- a conversa nao fica refem
// da mensageria.
//
// e a publicacao sai de outra thread (@Async, quinta entrega). na thread da requisicao,
// um broker fora do ar custava o timeout de conexao a quem reagiu: medido no
// kubernetes, onde o Service do rabbitmq existe sem pods e a conexao espera em vez de ser
// recusada, a reacao era gravada e o leitor recebia 503 em 3,9s -- tentava de novo e
// levava 409. assincrona, a resposta nao espera o broker. a ordem entre duas publicacoes
// pode trocar, e isso ja era tolerado: o consumidor aplica "o mais recente vence".
@Component
public class EngagementEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EngagementEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public EngagementEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Async(PublicacaoAssincronaConfig.EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEngagementChanged(EngagementChanged change) {
        publish(change);
    }

    public void publish(EngagementChanged change) {
        EngagementSnapshotMessage message = new EngagementSnapshotMessage(
                change.postId(), change.comments(), change.reactions(), change.change(), change.occurredAt());
        try {
            rabbitTemplate.convertAndSend(ENGAGEMENT_EXCHANGE, change.change(), message, amqp -> {
                amqp.getMessageProperties().setType(change.change());
                return amqp;
            });
            log.debug("publicado {} do post {}: {} comentario(s), {} reacao(oes)",
                    change.change(), change.postId(), change.comments(), change.reactions());
        } catch (AmqpException e) {
            log.warn("nao foi possivel publicar {} do post {}: {}", change.change(), change.postId(), e.getMessage());
        }
    }
}
