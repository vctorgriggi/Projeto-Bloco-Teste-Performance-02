package com.blog.engagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

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
//
// na quarta entrega o comentario ganhou o submissionId: quando ele chega pela fila, e
// nao por http, a identidade dele nasce no monolito, antes de existir linha aqui. a
// restricao de unicidade e o que garante que a mesma mensagem, entregue duas vezes, nao
// vira dois comentarios. e nulo nos comentarios criados pela api http deste servico.
@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comments_post_id", columnList = "post_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_comments_submission_id", columnNames = "submission_id")
        }
)
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

    // texto longo. ate a quarta entrega era @Lob, e o @Lob tinha dois defeitos que o h2
    // escondia: o envers nao o levava para a tabela de auditoria (que nascia varchar(255)
    // e quebrava a gravacao de qualquer texto maior), e no postgres ele vira um large
    // object (oid), guardado fora da linha, que fica orfao quando a linha e apagada.
    // LONG32VARCHAR vira text no postgres, nas duas tabelas.
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "submission_id", length = 36)
    private String submissionId;

    @Version
    private Long version;

    public Comment(Long postId, String authorName, String content) {
        this(postId, authorName, content, null, Instant.now());
    }

    // comentario que chegou por comando. o instante e o do envio pelo leitor, e nao o
    // do processamento: se a mensagem esperou na fila, a conversa continua na ordem em
    // que foi escrita.
    public Comment(Long postId, String authorName, String content, String submissionId, Instant createdAt) {
        this.postId = postId;
        this.authorName = authorName;
        this.content = content;
        this.submissionId = submissionId;
        this.createdAt = createdAt;
    }
}
