package com.blog.engagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.time.Instant;

import static lombok.AccessLevel.PROTECTED;

// comentario de um leitor em um post. a entidade veio do monolito na terceira
// entrega e chegou aqui inteira: mesmo mapeamento jpa, mesmo indice em post_id,
// mesmo travamento otimista e a mesma auditoria do envers (comments_AUD).
//
// o postId continua sendo apenas um numero. antes ele era uma referencia solta
// dentro do mesmo banco; agora e um identificador que atravessa a fronteira de
// rede, e nao existe (nem poderia existir) chave estrangeira para a tabela de
// posts, que vive em outro servico e em outro banco.
@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_post_id", columnList = "post_id")
})
@Audited
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_name", nullable = false)
    private String authorName; // nome livre de quem comenta, sem cadastro

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    public Comment(Long postId, String authorName, String content) {
        this.postId = postId;
        this.authorName = authorName;
        this.content = content;
        this.createdAt = Instant.now();
    }
}
