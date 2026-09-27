package com.blog.engagement.service;

import com.blog.engagement.domain.Reaction;
import com.blog.engagement.domain.ReactionType;
import com.blog.engagement.repository.ReactionCount;
import com.blog.engagement.repository.ReactionRepository;
import com.blog.engagement.shared.BusinessRuleException;
import com.blog.engagement.shared.ResourceNotFoundException;
import com.blog.engagement.web.dto.ReactionRequest;
import com.blog.engagement.web.dto.ReactionSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// casos de uso da capacidade nova desta entrega. as regras vivem aqui, como no
// resto do sistema: um leitor deixa cada tipo de reacao uma vez, e desfazer so
// vale para uma reacao que existe.
@Service
@Transactional
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final EngagementChanges engagementChanges;

    public ReactionService(ReactionRepository reactionRepository, EngagementChanges engagementChanges) {
        this.reactionRepository = reactionRepository;
        this.engagementChanges = engagementChanges;
    }

    public ReactionSummaryResponse react(Long postId, ReactionRequest request) {
        if (reactionRepository.existsByPostIdAndReaderNameAndType(postId, request.readerName(), request.type())) {
            throw new BusinessRuleException(
                    "O leitor " + request.readerName() + " ja reagiu com " + request.type() + " neste post");
        }
        reactionRepository.save(new Reaction(postId, request.readerName(), request.type()));
        engagementChanges.announce(EngagementChanged.REACTION_ADDED, postId);
        return summaryFor(postId, request.readerName());
    }

    public ReactionSummaryResponse undoReaction(Long postId, ReactionType type, String readerName) {
        Reaction reaction = reactionRepository
                .findByPostIdAndReaderNameAndType(postId, readerName, type)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "O leitor " + readerName + " nao tem reacao " + type + " no post " + postId));
        reactionRepository.delete(reaction);
        engagementChanges.announce(EngagementChanged.REACTION_REMOVED, postId);
        return summaryFor(postId, readerName);
    }

    // monta o resumo em duas consultas: uma agregacao no banco para os totais por
    // tipo e uma busca pelas reacoes daquele leitor. o leitor e opcional -- quem
    // so esta lendo a pagina ve os numeros sem se identificar.
    @Transactional(readOnly = true)
    public ReactionSummaryResponse summaryFor(Long postId, String readerName) {
        // todos os tipos aparecem no mapa, inclusive os zerados, para a interface
        // desenhar a barra completa sem precisar conhecer o enum do servidor
        Map<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);
        for (ReactionType type : ReactionType.values()) {
            counts.put(type, 0L);
        }

        long total = 0;
        for (ReactionCount linha : reactionRepository.countByTypeForPost(postId)) {
            counts.put(linha.getType(), linha.getTotal());
            total += linha.getTotal();
        }

        List<ReactionType> mine = StringUtils.hasText(readerName)
                ? reactionRepository.findByPostIdAndReaderName(postId, readerName).stream()
                        .map(Reaction::getType)
                        .toList()
                : List.of();

        return new ReactionSummaryResponse(postId, total, counts, mine);
    }
}
