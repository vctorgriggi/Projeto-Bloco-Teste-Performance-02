package com.blog.engagement.messaging;

import com.blog.engagement.domain.Comment;
import com.blog.engagement.messaging.message.RegisterCommentCommand;
import com.blog.engagement.repository.CommentRepository;
import com.blog.engagement.shared.InvalidMessageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// o consumidor dos comandos de comentario, chamado como metodo. o que se testa aqui e o
// que o listener decide -- o que vira comentario, o que e repetido, o que e invalido --
// sem depender de um broker. a entrega de verdade, pela fila, esta coberta no
// MessagingIntegrationTest.
@SpringBootTest
@ActiveProfiles("test")
class CommentCommandListenerTest {

    @Autowired
    private CommentCommandListener listener;

    @Autowired
    private CommentRepository commentRepository;

    @AfterEach
    void limparBanco() {
        commentRepository.deleteAll();
    }

    private RegisterCommentCommand comando(String submissionId, String autor, String texto, Instant enviadoEm) {
        return new RegisterCommentCommand(submissionId, 1L, autor, texto, enviadoEm);
    }

    // o comentario nasce com o instante em que o leitor enviou, e nao com o do
    // processamento: se a mensagem esperou na fila, a conversa fica na ordem certa
    @Test
    void comandoValido_viraComentarioComOInstanteDoEnvio() {
        Instant enviadoEm = Instant.parse("2026-03-01T10:00:00Z");

        listener.onRegisterComment(comando("envio-1", "Carla", "chegou pela fila", enviadoEm));

        List<Comment> conversa = commentRepository.findByPostIdOrderByCreatedAtAsc(1L);
        assertThat(conversa).hasSize(1);
        assertThat(conversa.get(0).getSubmissionId()).isEqualTo("envio-1");
        assertThat(conversa.get(0).getContent()).isEqualTo("chegou pela fila");
        assertThat(conversa.get(0).getCreatedAt()).isEqualTo(enviadoEm);
    }

    // o broker entrega pelo menos uma vez. a mesma mensagem processada duas vezes nao
    // pode virar dois comentarios -- e o que faz o "pelo menos uma vez" ser seguro
    @Test
    void mesmaMensagemEntregueDuasVezes_viraUmComentarioSo() {
        RegisterCommentCommand repetido = comando("envio-2", "Diego", "so uma vez", Instant.now());

        listener.onRegisterComment(repetido);
        listener.onRegisterComment(repetido);

        assertThat(commentRepository.countByPostId(1L)).isEqualTo(1);
    }

    // mensagens que esperaram na fila entram na conversa pela ordem do envio, mesmo que
    // sejam processadas fora dela (dois consumidores concorrentes, por exemplo)
    @Test
    void comandosProcessadosForaDeOrdem_ficamNaOrdemEmQueForamEnviados() {
        listener.onRegisterComment(comando("envio-b", "Diego", "segundo", Instant.parse("2026-03-01T10:05:00Z")));
        listener.onRegisterComment(comando("envio-a", "Carla", "primeiro", Instant.parse("2026-03-01T10:00:00Z")));

        assertThat(commentRepository.findByPostIdOrderByCreatedAtAsc(1L))
                .extracting(Comment::getContent)
                .containsExactly("primeiro", "segundo");
    }

    // uma mensagem sem texto nao vai dar certo em tentativa nenhuma. a excecao e a nao
    // retentavel, que manda a mensagem direto para a dead letter
    @Test
    void comandoSemTexto_eRecusadoComoMensagemInvalida() {
        assertThatThrownBy(() -> listener.onRegisterComment(comando("envio-3", "Carla", "  ", Instant.now())))
                .isInstanceOf(InvalidMessageException.class);

        assertThat(commentRepository.count()).isZero();
    }

    @Test
    void comandoSemIdentificadorDeEnvio_eRecusado() {
        assertThatThrownBy(() -> listener.onRegisterComment(comando(null, "Carla", "texto", Instant.now())))
                .isInstanceOf(InvalidMessageException.class)
                .hasMessageContaining("submissionId");
    }
}
