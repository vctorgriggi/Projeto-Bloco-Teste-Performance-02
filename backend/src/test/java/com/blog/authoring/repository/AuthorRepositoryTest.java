package com.blog.authoring.repository;

import com.blog.authoring.domain.Author;
import com.blog.shared.config.PersistenceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// testes da persistencia do autor: a consulta derivada existsByEmail, a restricao
// de unicidade do email e o travamento otimista via @Version. o @DataJpaTest sobe
// so a fatia de jpa sobre o h2 e reverte a transacao ao fim de cada teste; o
// @Import traz a config que habilita a fabrica de repositorios do envers.
@DataJpaTest
@Import(PersistenceConfig.class)
class AuthorRepositoryTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void existsByEmail_refleteOsAutoresGravados() {
        authorRepository.save(new Author("Ana Souza", "ana@blog.dev", "escreve sobre arquitetura"));
        em.flush();

        assertThat(authorRepository.existsByEmail("ana@blog.dev")).isTrue();
        assertThat(authorRepository.existsByEmail("desconhecido@blog.dev")).isFalse();
    }

    @Test
    void email_ehUnicoNoBanco() {
        authorRepository.saveAndFlush(new Author("Ana", "ana@blog.dev", "bio"));

        Author duplicado = new Author("Ana Clone", "ana@blog.dev", "outra bio");
        assertThatThrownBy(() -> authorRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updateProfile_incrementaAVersao() {
        Author autor = authorRepository.saveAndFlush(new Author("Ana", "ana@blog.dev", "bio inicial"));
        assertThat(autor.getVersion()).isZero();

        autor.updateProfile("Ana Souza", "bio revisada");
        authorRepository.saveAndFlush(autor);

        assertThat(autor.getVersion()).isEqualTo(1L);
        assertThat(autor.getName()).isEqualTo("Ana Souza");
        assertThat(autor.getBio()).isEqualTo("bio revisada");
    }
}
