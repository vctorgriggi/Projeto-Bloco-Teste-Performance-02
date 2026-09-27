package com.blog.engagement.repository;

// projecao por interface para as agregacoes "total por post", no mesmo molde do
// ReactionCount: o spring data preenche os getters com as colunas do group by.
public interface PostTotal {

    Long getPostId();

    long getTotal();
}
