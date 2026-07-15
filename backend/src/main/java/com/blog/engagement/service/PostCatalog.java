package com.blog.engagement.service;

// porta (context mapping) que o engajamento usa para saber se um post existe,
// sem depender da entidade Post nem do repositorio do contexto de authoring.
// quem implementa essa interface e um adaptador do lado de authoring.
public interface PostCatalog {

    boolean postExists(Long postId);
}
