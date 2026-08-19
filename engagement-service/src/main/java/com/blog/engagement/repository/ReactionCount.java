package com.blog.engagement.repository;

import com.blog.engagement.domain.ReactionType;

// projecao por interface: o spring data preenche esses getters com o resultado da
// agregacao, sem precisar de uma classe de dto na camada de persistencia nem de
// um cast de Object[]. o resumo de reacoes le so o que precisa (tipo e total).
public interface ReactionCount {

    ReactionType getType();

    long getTotal();
}
