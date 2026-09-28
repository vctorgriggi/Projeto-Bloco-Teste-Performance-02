package com.blog.engagement.messaging;

import com.blog.engagement.domain.Comment;
import com.blog.engagement.domain.ReactionType;
import com.blog.engagement.messaging.message.EngagementSnapshotMessage;
import com.blog.engagement.messaging.message.PostDeletedMessage;
import com.blog.engagement.messaging.message.SnapshotRequest;
import com.blog.engagement.repository.CommentRepository;
import com.blog.engagement.repository.ReactionRepository;
import com.blog.engagement.service.CommentService;
import com.blog.engagement.service.EngagementChanges;
import com.blog.engagement.service.ReactionService;
import com.blog.engagement.web.dto.CommentRequest;
import com.blog.engagement.web.dto.ReactionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.ConnectException;
import java.time.Instant;

import static com.blog.engagement.messaging.Topology.ENGAGEMENT_EXCHANGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// o lado publicador do engajamento: toda mudanca vira um evento com os totais do post
// depois dela, e so depois de a transacao confirmar. o RabbitTemplate entra dublado,
// porque o que importa aqui e o que sai e quando sai -- a travessia pelo broker esta no
// MessagingIntegrationTest.
//
// nao e transacional, pelo mesmo motivo dos testes de auditoria: a publicacao escuta o
// commit, e um teste dentro de uma transacao revertida nunca a veria acontecer.
@SpringBootTest
@ActiveProfiles("test")
class EngagementEventPublisherTest {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ReactionService reactionService;

    @Autowired
    private PostDeletedListener postDeletedListener;

    @Autowired
    private SnapshotRequestListener snapshotRequestListener;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EngagementChanges engagementChanges;

    @Autowired
    private EngagementEventPublisher eventPublisher;

    @Autowired
    private RabbitListenerEndpointRegistry listenerRegistry;

    @AfterEach
    void limparBanco() {
        commentRepository.deleteAll();
        reactionRepository.deleteAll();
    }

    private EngagementSnapshotMessage publicado(String routingKey) {
        ArgumentCaptor<Object> corpo = ArgumentCaptor.forClass(Object.class);
        // a publicacao e assincrona (quinta entrega): espera ela sair, com limite
        verify(rabbitTemplate, timeout(3000)).convertAndSend(eq(ENGAGEMENT_EXCHANGE), eq(routingKey),
                corpo.capture(), any(MessagePostProcessor.class));
        return (EngagementSnapshotMessage) corpo.getValue();
    }

    // o corpo carrega o estado depois da mudanca (o total), e nao o delta. e isso que
    // torna seguro para o consumidor aplicar a mesma mensagem duas vezes
    @Test
    void reagir_publicaReactionAddedComOsTotaisDoPostDepoisDaMudanca() {
        commentService.addToPost(7L, new CommentRequest("Carla", "oi"));
        reactionService.react(7L, new ReactionRequest("Carla", ReactionType.CAFE));

        EngagementSnapshotMessage mensagem = publicado("reaction.added");
        assertThat(mensagem.postId()).isEqualTo(7L);
        assertThat(mensagem.comments()).isEqualTo(1);
        assertThat(mensagem.reactions()).isEqualTo(1);
        assertThat(mensagem.change()).isEqualTo("reaction.added");
        assertThat(mensagem.occurredAt()).isNotNull();
    }

    @Test
    void apagarComentario_publicaCommentRemovedJaSemEle() {
        Comment comentario = commentService.addToPost(7L, new CommentRequest("Carla", "vai sumir"));

        commentService.delete(comentario.getId());

        assertThat(publicado("comment.removed").comments()).isZero();
    }

    // se a transacao voltar atras, nada sai: anunciar um comentario que nao foi gravado
    // faria o monolito contar uma conversa que nao existe
    @Test
    void transacaoRevertida_naoPublicaNada() {
        transactionTemplate.executeWithoutResult(status -> {
            commentService.addToPost(7L, new CommentRequest("Carla", "nunca existiu"));
            status.setRollbackOnly();
        });

        verify(rabbitTemplate, after(500).never()).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class));
        assertThat(commentRepository.count()).isZero();
    }

    // o evento de post apagado chega, a conversa sai, e o aviso de que o post saiu de
    // cena vai para quem mantem copia dos contadores
    @Test
    void postApagadoNoMonolito_limpaEPublicaEngagementPurged() {
        commentService.addToPost(9L, new CommentRequest("Carla", "vai sumir"));
        reactionService.react(9L, new ReactionRequest("Diego", ReactionType.IDEIA));

        postDeletedListener.onPostDeleted(new PostDeletedMessage(9L, Instant.now()));

        assertThat(commentRepository.countByPostId(9L)).isZero();
        assertThat(reactionRepository.countByPostId(9L)).isZero();
        EngagementSnapshotMessage mensagem = publicado("engagement.purged");
        assertThat(mensagem.comments()).isZero();
        assertThat(mensagem.reactions()).isZero();
    }

    // o pedido de republicacao gera um snapshot por post com engajamento
    @Test
    void pedidoDeRepublicacao_publicaUmSnapshotPorPost() {
        commentService.addToPost(1L, new CommentRequest("Carla", "a"));
        commentService.addToPost(1L, new CommentRequest("Diego", "b"));
        reactionService.react(2L, new ReactionRequest("Ana", ReactionType.CAFE));

        snapshotRequestListener.onSnapshotRequest(new SnapshotRequest("teste", Instant.now()));

        ArgumentCaptor<Object> corpos = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate, times(2)).convertAndSend(eq(ENGAGEMENT_EXCHANGE), eq("engagement.snapshot"),
                corpos.capture(), any(MessagePostProcessor.class));
        assertThat(corpos.getAllValues())
                .extracting(m -> (EngagementSnapshotMessage) m)
                .extracting(EngagementSnapshotMessage::postId, EngagementSnapshotMessage::comments,
                        EngagementSnapshotMessage::reactions)
                .containsExactly(tuple(1L, 2L, 0L), tuple(2L, 0L, 1L));
    }

    // com o broker fora do ar, o comentario continua gravado: o contador e um dado
    // derivado, que se corrige na proxima mudanca, e nao pode derrubar a conversa
    @Test
    void brokerForaDoAr_naoImpedeOComentario() {
        willThrow(new AmqpConnectException(new ConnectException("recusada")))
                .given(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class),
                        any(MessagePostProcessor.class));

        assertThatCode(() -> commentService.addToPost(7L, new CommentRequest("Carla", "fica gravado")))
                .doesNotThrowAnyException();

        assertThat(commentRepository.countByPostId(7L)).isEqualTo(1);
    }

    // ao terminar de subir, o servico anuncia o estado de cada post antes de ligar os
    // consumidores -- e o que acerta a copia do monolito depois de um restart
    @Test
    void aoSubir_anunciaOEstadoDeCadaPostAntesDeConsumir() {
        commentService.addToPost(3L, new CommentRequest("Carla", "ja estava aqui"));
        clearInvocations(rabbitTemplate);

        new MessagingStartup(engagementChanges, eventPublisher, listenerRegistry, true, false).onReady();

        EngagementSnapshotMessage mensagem = publicado("engagement.snapshot");
        assertThat(mensagem.postId()).isEqualTo(3L);
        assertThat(mensagem.comments()).isEqualTo(1);
    }

    // o broker lento (fora do ar, com a conexao esperando o timeout) nao pode segurar a
    // resposta de quem reagiu ou comentou. medido no kubernetes antes da correcao: a
    // reacao era gravada e o leitor recebia 503 em 3,9s
    @Test
    void brokerLento_naoAtrasaAResposta() {
        willAnswer(invocacao -> {
            Thread.sleep(2000);
            return null;
        }).given(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class),
                any(MessagePostProcessor.class));

        long inicio = System.nanoTime();
        reactionService.react(8L, new ReactionRequest("Carla", ReactionType.CAFE));
        long milissegundos = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(milissegundos).isLessThan(1000);
    }
}
