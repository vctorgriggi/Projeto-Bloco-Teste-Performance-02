package com.blog.shared.messaging.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxMessage, String> {

    // as pendentes, na ordem em que foram gravadas, em lotes. a ordem importa: dois
    // eventos do mesmo post devem sair na sequencia em que aconteceram
    List<OutboxMessage> findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();

    // quantas ainda nao sairam. vai para o diagnostico da integracao: um numero que so
    // cresce e o sinal de que o broker esta fora ou recusando
    long countByPublishedAtIsNull();
}
