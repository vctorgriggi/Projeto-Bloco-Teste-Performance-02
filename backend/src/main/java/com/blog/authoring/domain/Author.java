package com.blog.authoring.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import static lombok.AccessLevel.PROTECTED;

// aggregate root do autor. expoe metodos de intencao (updateProfile) em vez de
// setters soltos para manter as invariantes dentro do dominio. @Audited faz o
// envers manter uma tabela de historico (authors_AUD) com o estado a cada revisao.
@Entity
@Table(name = "authors")
@Audited
@Getter
@NoArgsConstructor(access = PROTECTED) // exigido pelo jpa, mas nao para uso externo
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 500)
    private String bio;

    // travamento otimista: o hibernate incrementa a versao a cada update e recusa
    // gravacoes sobre um estado defasado, protegendo a integridade em concorrencia.
    @Version
    private Long version;

    public Author(String name, String email, String bio) {
        this.name = name;
        this.email = email;
        this.bio = bio;
    }

    public void updateProfile(String name, String bio) {
        this.name = name;
        this.bio = bio;
    }
}
