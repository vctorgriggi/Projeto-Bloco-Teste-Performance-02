package com.blog.authoring.domain;

// ciclo de vida de um post. um rascunho ainda nao aparece publicamente; ao ser
// publicado passa a valer para leitura.
public enum PostStatus {
    DRAFT,
    PUBLISHED
}
