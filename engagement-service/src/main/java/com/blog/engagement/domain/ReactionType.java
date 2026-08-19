package com.blog.engagement.domain;

// as reacoes que um leitor pode deixar em um post. o conjunto e fechado de
// proposito: e um enum, nao texto livre, para o banco e a api nao aceitarem
// valores inventados. gravado como string para a coluna continuar legivel.
public enum ReactionType {

    CORACAO,  // gostei / me tocou
    CAFE,     // li com calma, acompanhado
    IDEIA     // me fez pensar
}
