package com.blog.engagement.repository;

import com.blog.engagement.domain.Reaction;
import com.blog.engagement.domain.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// repositorio do aggregate novo. o crud vem herdado; abaixo ficam as consultas
// que o servico de fato faz, todas filtrando por post_id (o indice da entidade).
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    // soma as reacoes de um post agrupadas por tipo. e o coracao do resumo: em
    // vez de trazer todas as linhas e contar em memoria, o group by resolve no
    // banco e devolve uma linha por tipo. como e uma agregacao, nao da para
    // escrever com nome de metodo derivado, entao aqui a consulta e explicita.
    @Query("""
            select r.type as type, count(r) as total
            from Reaction r
            where r.postId = :postId
            group by r.type
            """)
    List<ReactionCount> countByTypeForPost(@Param("postId") Long postId);

    // as reacoes daquele leitor especifico no post, para a interface marcar quais
    // botoes ele ja apertou
    List<Reaction> findByPostIdAndReaderName(Long postId, String readerName);

    // sustenta a regra de "uma reacao de cada tipo por leitor" antes do insert,
    // sem depender de deixar a restricao de unicidade estourar
    boolean existsByPostIdAndReaderNameAndType(Long postId, String readerName, ReactionType type);

    // usada para desfazer uma reacao (o mesmo clique que alterna o botao)
    Optional<Reaction> findByPostIdAndReaderNameAndType(Long postId, String readerName, ReactionType type);

    long countByPostId(Long postId);

    // limpeza do engajamento de um post que deixou de existir
    long deleteByPostId(Long postId);
}
