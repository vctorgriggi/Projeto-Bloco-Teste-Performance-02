package com.blog.engagement.service;

import com.blog.engagement.repository.CommentRepository;
import com.blog.engagement.repository.PostTotal;
import com.blog.engagement.repository.ReactionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// monta e anuncia o estado do engajamento de um post depois de uma mudanca.
//
// e chamado de dentro da transacao do service que mudou o dado, e isso importa: as
// contagens enxergam a propria escrita (o hibernate descarrega as alteracoes pendentes
// antes da consulta), entao o total anunciado ja inclui o comentario que acabou de
// entrar. o anuncio so sai do processo depois do commit.
@Component
public class EngagementChanges {

    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EngagementChanges(CommentRepository commentRepository, ReactionRepository reactionRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
        this.eventPublisher = eventPublisher;
    }

    public void announce(String change, Long postId) {
        eventPublisher.publishEvent(new EngagementChanged(
                postId,
                commentRepository.countByPostId(postId),
                reactionRepository.countByPostId(postId),
                change,
                Instant.now()));
    }

    // o estado atual de todos os posts que tem algum engajamento, em duas consultas
    // agregadas em vez de duas por post
    public List<EngagementChanged> currentState(String change) {
        Map<Long, long[]> totais = new TreeMap<>();
        for (PostTotal linha : commentRepository.countGroupedByPost()) {
            totais.computeIfAbsent(linha.getPostId(), id -> new long[2])[0] = linha.getTotal();
        }
        for (PostTotal linha : reactionRepository.countGroupedByPost()) {
            totais.computeIfAbsent(linha.getPostId(), id -> new long[2])[1] = linha.getTotal();
        }

        Instant agora = Instant.now();
        return totais.entrySet().stream()
                .map(e -> new EngagementChanged(e.getKey(), e.getValue()[0], e.getValue()[1], change, agora))
                .toList();
    }
}
