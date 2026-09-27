package com.blog.engagement.service;

import com.blog.engagement.repository.CommentRepository;
import com.blog.engagement.repository.ReactionRepository;
import com.blog.engagement.web.dto.PurgeResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// remove todo o engajamento de um post. e chamado quando o post deixa de existir no
// outro servico, o que aqui e apenas uma informacao que chega de fora -- este
// processo nao tem como saber sozinho que um post foi apagado.
//
// desde a quarta entrega essa informacao chega como evento (post.deleted), pela fila
// engagement.post-deleted. a rota http que fazia o mesmo continua existindo, como
// ferramenta de operacao, mas o monolito deixou de chama-la.
//
// a operacao e idempotente de proposito: limpar duas vezes o mesmo post devolve zero
// na segunda e nao e erro. e o que torna seguro o "pelo menos uma vez" do broker: a
// reentrega da mesma mensagem e normal, e aqui ela e inofensiva.
@Service
@Transactional
public class EngagementCleanupService {

    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EngagementCleanupService(CommentRepository commentRepository,
                                    ReactionRepository reactionRepository,
                                    ApplicationEventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
        this.eventPublisher = eventPublisher;
    }

    public PurgeResponse purgePost(Long postId) {
        long comentarios = commentRepository.deleteByPostId(postId);
        long reacoes = reactionRepository.deleteByPostId(postId);

        // anuncia mesmo quando nao havia nada para limpar: quem mantem uma copia dos
        // contadores precisa saber que este post saiu de cena, tenha ele conversa ou nao
        eventPublisher.publishEvent(new EngagementChanged(
                postId, 0, 0, EngagementChanged.ENGAGEMENT_PURGED, Instant.now()));
        return new PurgeResponse(postId, comentarios, reacoes);
    }
}
