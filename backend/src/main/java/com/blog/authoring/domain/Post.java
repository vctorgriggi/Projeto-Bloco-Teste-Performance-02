package com.blog.authoring.domain;

import com.blog.shared.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

import static lombok.AccessLevel.PROTECTED;

// aggregate root do post. referencia o autor por id (e nao por objeto) para
// manter os aggregates desacoplados. as transicoes de estado (publish) ficam
// dentro da entidade para proteger a regra de negocio. os indices cobrem os
// caminhos de consulta reais (por autor, por status e a ordenacao por data).
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_author_id", columnList = "author_id"),
        @Index(name = "idx_posts_status", columnList = "status"),
        @Index(name = "idx_posts_created_at", columnList = "created_at")
})
@Audited
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    // texto longo. ate a quarta entrega era @Lob, e o @Lob tinha dois defeitos que o h2
    // escondia: o envers nao o levava para a tabela de auditoria (que nascia varchar(255)
    // e quebrava a gravacao de qualquer texto maior), e no postgres ele vira um large
    // object (oid), guardado fora da linha, que fica orfao quando a linha e apagada.
    // LONG32VARCHAR vira text no postgres, nas duas tabelas.
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(nullable = false)
    private String content;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    private Long version;

    public Post(String title, String content, Long authorId) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.status = PostStatus.DRAFT;
        this.createdAt = Instant.now();
    }

    public void edit(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void publish() {
        if (status == PostStatus.PUBLISHED) {
            throw new BusinessRuleException("Post ja esta publicado");
        }
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }
}
