package com.blog.engagement.web;

import com.blog.engagement.domain.Comment;
import com.blog.engagement.domain.Reaction;
import com.blog.engagement.domain.ReactionType;
import com.blog.engagement.repository.CommentRepository;
import com.blog.engagement.repository.ReactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// a rota de limpeza de um post apagado. desde a quarta entrega o caminho normal e o
// evento post.deleted (coberto no EngagementEventPublisherTest e no teste de
// integracao), e esta rota ficou como ferramenta de operacao -- mas a regra e a mesma,
// entao vale testar tanto o caso normal quanto a idempotencia (a segunda chamada nao
// pode falhar, porque a notificacao pode ser reentregue).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostEngagementApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @AfterEach
    void limparBanco() {
        commentRepository.deleteAll();
        reactionRepository.deleteAll();
    }

    @Test
    void deleteEngajamento_removeComentariosEReacoesSoDaquelePost() throws Exception {
        commentRepository.save(new Comment(1L, "Carla", "vai sumir"));
        commentRepository.save(new Comment(1L, "Diego", "tambem vai"));
        commentRepository.save(new Comment(2L, "Ana", "essa fica"));
        reactionRepository.save(new Reaction(1L, "Carla", ReactionType.CORACAO));
        reactionRepository.save(new Reaction(2L, "Ana", ReactionType.CAFE));

        mockMvc.perform(delete("/api/posts/{postId}/engagement", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(1))
                .andExpect(jsonPath("$.comments").value(2))
                .andExpect(jsonPath("$.reactions").value(1));

        assertThat(commentRepository.countByPostId(1L)).isZero();
        assertThat(commentRepository.countByPostId(2L)).isEqualTo(1);
        assertThat(reactionRepository.countByPostId(2L)).isEqualTo(1);
    }

    @Test
    void deleteEngajamentoRepetido_eIdempotenteEDevolveZero() throws Exception {
        commentRepository.save(new Comment(1L, "Carla", "vai sumir"));

        mockMvc.perform(delete("/api/posts/{postId}/engagement", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").value(1));

        mockMvc.perform(delete("/api/posts/{postId}/engagement", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments").value(0))
                .andExpect(jsonPath("$.reactions").value(0));
    }
}
