package com.blog.engagement.repository;

import com.blog.engagement.domain.Reaction;
import com.blog.engagement.domain.ReactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// testes do repositorio dedicado do aggregate novo: a consulta de agregacao que
// alimenta o resumo, as consultas derivadas por leitor e a restricao de unicidade
// que impede o mesmo leitor de repetir a mesma reacao.
@DataJpaTest
@ActiveProfiles("test")
class ReactionRepositoryTest {

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void countByTypeForPost_agrupaOsTotaisPorTipoNoBanco() {
        reactionRepository.saveAndFlush(new Reaction(1L, "Carla", ReactionType.CORACAO));
        reactionRepository.saveAndFlush(new Reaction(1L, "Diego", ReactionType.CORACAO));
        reactionRepository.saveAndFlush(new Reaction(1L, "Carla", ReactionType.IDEIA));
        reactionRepository.saveAndFlush(new Reaction(2L, "Ana", ReactionType.CAFE));

        Map<ReactionType, Long> totais = reactionRepository.countByTypeForPost(1L).stream()
                .collect(Collectors.toMap(ReactionCount::getType, ReactionCount::getTotal));

        // so os tipos com reacao aparecem na agregacao; o CAFE e do outro post
        assertThat(totais).containsOnly(
                Map.entry(ReactionType.CORACAO, 2L),
                Map.entry(ReactionType.IDEIA, 1L));
    }

    @Test
    void findByPostIdAndReaderName_trazSoAsReacoesDaquelePessoa() {
        reactionRepository.saveAndFlush(new Reaction(1L, "Carla", ReactionType.CORACAO));
        reactionRepository.saveAndFlush(new Reaction(1L, "Carla", ReactionType.IDEIA));
        reactionRepository.saveAndFlush(new Reaction(1L, "Diego", ReactionType.CORACAO));

        List<ReactionType> daCarla = reactionRepository.findByPostIdAndReaderName(1L, "Carla").stream()
                .map(Reaction::getType)
                .toList();

        assertThat(daCarla).containsExactlyInAnyOrder(ReactionType.CORACAO, ReactionType.IDEIA);
    }

    @Test
    void existsByPostIdAndReaderNameAndType_distingueTipoELeitor() {
        reactionRepository.saveAndFlush(new Reaction(1L, "Carla", ReactionType.CORACAO));

        assertThat(reactionRepository
                .existsByPostIdAndReaderNameAndType(1L, "Carla", ReactionType.CORACAO)).isTrue();
        assertThat(reactionRepository
                .existsByPostIdAndReaderNameAndType(1L, "Carla", ReactionType.CAFE)).isFalse();
        assertThat(reactionRepository
                .existsByPostIdAndReaderNameAndType(1L, "Diego", ReactionType.CORACAO)).isFalse();
    }

    @Test
    void mesmoLeitorNaoRepeteOMesmoTipo_restricaoDoBanco() {
        reactionRepository.saveAndFlush(new Reaction(1L, "Carla", ReactionType.CORACAO));

        Reaction repetida = new Reaction(1L, "Carla", ReactionType.CORACAO);
        assertThatThrownBy(() -> reactionRepository.saveAndFlush(repetida))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void mesmoLeitorPodeUsarTiposDiferentes() {
        reactionRepository.saveAndFlush(new Reaction(1L, "Carla", ReactionType.CORACAO));
        reactionRepository.saveAndFlush(new Reaction(1L, "Carla", ReactionType.CAFE));

        assertThat(reactionRepository.countByPostId(1L)).isEqualTo(2);
    }

    @Test
    void novaReacao_nasceComDataEVersaoZerada() {
        Reaction reacao = reactionRepository.saveAndFlush(new Reaction(1L, "Carla", ReactionType.IDEIA));

        assertThat(reacao.getCreatedAt()).isNotNull();
        assertThat(reacao.getVersion()).isZero();
        assertThat(reacao.getType()).isEqualTo(ReactionType.IDEIA);
    }

    // guarda contra uma regressao silenciosa: se o @Enumerated virasse ordinal, a
    // coluna passaria a guardar 0/1/2 e o banco ficaria ilegivel. ler pela jpa nao
    // pegaria isso (o enum volta igual dos dois jeitos), por isso a consulta e
    // nativa, direto na coluna.
    @Test
    void tipoEGravadoComoTextoNaColuna() {
        reactionRepository.saveAndFlush(new Reaction(1L, "Carla", ReactionType.IDEIA));

        Object tipoNaColuna = em.getEntityManager()
                .createNativeQuery("select type from reactions")
                .getSingleResult();

        assertThat(tipoNaColuna).hasToString("IDEIA");
    }
}
