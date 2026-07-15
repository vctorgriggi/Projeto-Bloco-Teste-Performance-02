package com.blog.authoring.repository;

import com.blog.authoring.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;

import java.util.List;

// alem do crud, estende RevisionRepository para consultar o historico de cada
// post pela integracao spring data envers. o terceiro parametro (Integer) e o
// tipo do numero de revisao gerado pelo envers.
public interface PostRepository extends JpaRepository<Post, Long>, RevisionRepository<Post, Long, Integer> {

    List<Post> findAllByOrderByCreatedAtDesc();
}
