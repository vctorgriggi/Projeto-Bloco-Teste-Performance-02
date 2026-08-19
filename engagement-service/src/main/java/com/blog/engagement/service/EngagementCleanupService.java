package com.blog.engagement.service;

import com.blog.engagement.repository.CommentRepository;
import com.blog.engagement.repository.ReactionRepository;
import com.blog.engagement.web.dto.PurgeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// remove todo o engajamento de um post. e chamado quando o post deixa de existir no
// outro servico, o que aqui e apenas uma informacao que chega de fora -- este
// processo nao tem como saber sozinho que um post foi apagado.
//
// a operacao e idempotente de proposito: limpar duas vezes o mesmo post devolve zero
// na segunda e nao e erro. isso importa porque a entrega da notificacao e por melhor
// esforco no outro lado e, no dia em que virar uma mensagem em um broker, a
// reentrega da mesma mensagem passa a ser normal.
@Service
@Transactional
public class EngagementCleanupService {

    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;

    public EngagementCleanupService(CommentRepository commentRepository,
                                    ReactionRepository reactionRepository) {
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
    }

    public PurgeResponse purgePost(Long postId) {
        long comentarios = commentRepository.deleteByPostId(postId);
        long reacoes = reactionRepository.deleteByPostId(postId);
        return new PurgeResponse(postId, comentarios, reacoes);
    }
}
