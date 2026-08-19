package com.blog.engagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

import static lombok.AccessLevel.PROTECTED;

// aggregate novo desta entrega: a reacao de um leitor a um post. cada linha e um
// voto de um leitor em um tipo, e nao um contador agregado -- guardar o voto
// individual e o que permite saber se aquele leitor ja reagiu (para alternar o
// botao na interface) e ainda somar por tipo com uma consulta de agregacao.
//
// a restricao de unicidade (post, leitor, tipo) sustenta no banco a regra de que
// um leitor deixa cada tipo de reacao uma vez so, e nao apenas no service.
@Entity
@Table(
        name = "reactions",
        indexes = {
                @Index(name = "idx_reactions_post_id", columnList = "post_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reactions_post_reader_type",
                        columnNames = {"post_id", "reader_name", "type"}
                )
        }
)
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    // nao ha login no blog: o leitor se identifica por um nome, que e o que
    // amarra a reacao a uma pessoa para efeito de "uma vez cada".
    @Column(name = "reader_name", nullable = false)
    private String readerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReactionType type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    public Reaction(Long postId, String readerName, ReactionType type) {
        this.postId = postId;
        this.readerName = readerName;
        this.type = type;
        this.createdAt = Instant.now();
    }
}
