package com.blog.authoring.repository;

import com.blog.authoring.domain.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.history.RevisionRepository;

// camada de repositorio: spring data gera a implementacao em tempo de execucao.
// o RevisionRepository adiciona a consulta ao historico de perfil do autor.
public interface AuthorRepository extends JpaRepository<Author, Long>, RevisionRepository<Author, Long, Integer> {

    boolean existsByEmail(String email);
}
