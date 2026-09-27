package com.blog.engagement.projection;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

// mantem e serve a copia local dos contadores de engajamento.
//
// a regra de aplicacao e "o mais recente vence", pelo instante da mudanca na origem, e e
// o que torna o consumo seguro sem nenhuma tabela de mensagens ja processadas:
//
// - a mesma mensagem entregue duas vezes grava o mesmo estado (idempotente);
// - uma mensagem velha que chegue depois de uma nova e descartada (ordem);
// - uma mensagem perdida e corrigida pela proxima, que traz o total, e nao o delta.
//
// o post apagado nao precisa de caso especial: o engagement.purged chega com zero
// comentarios e zero reacoes, e vira uma linha zerada. manter a linha, em vez de apagar,
// e o que impede uma mudanca atrasada, anterior a exclusao, de ressuscitar os numeros.
@Service
@Transactional
public class EngagementCountersService {

    private final EngagementCounterRepository repository;

    public EngagementCountersService(EngagementCounterRepository repository) {
        this.repository = repository;
    }

    public boolean apply(Long postId, long comments, long reactions, String change, Instant occurredAt) {
        return repository.findById(postId)
                .map(atual -> atual.applyIfNotOlder(comments, reactions, change, occurredAt))
                .orElseGet(() -> {
                    repository.save(new EngagementCounter(postId, comments, reactions, change, occurredAt));
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public List<EngagementCounter> all() {
        return repository.findAllByOrderByPostIdAsc();
    }
}
