package com.blog.engagement.messaging;

import com.blog.engagement.domain.Comment;
import com.blog.engagement.messaging.message.EngagementSnapshotMessage;
import com.blog.engagement.messaging.message.PostDeletedMessage;
import com.blog.engagement.messaging.message.RegisterCommentCommand;
import com.blog.engagement.repository.CommentRepository;
import com.blog.engagement.repository.ReactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.blog.engagement.messaging.Topology.COMMANDS_EXCHANGE;
import static com.blog.engagement.messaging.Topology.COMMENT_COMMANDS_QUEUE;
import static com.blog.engagement.messaging.Topology.COMMENT_REGISTER;
import static com.blog.engagement.messaging.Topology.ENGAGEMENT_EXCHANGE;
import static com.blog.engagement.messaging.Topology.POSTS_EXCHANGE;
import static com.blog.engagement.messaging.Topology.POST_DELETED;
import static com.blog.engagement.messaging.Topology.UNROUTED_QUEUE;
import static com.blog.engagement.messaging.Topology.deadLetterQueueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// a mensageria de ponta a ponta, com um rabbitmq de verdade em container.
//
// os outros testes chamam os listeners como metodos e dublam o RabbitTemplate, e isso
// deixa de fora justamente o que so o broker faz: rotear pela routing key, desviar para
// a dead letter o que o consumidor recusou, mandar para a fila de nao roteados o evento
// que ninguem assinou. e a lacuna que a terceira entrega deixou registrada ("a conversa
// real foi verificada a mao"); aqui ela passa a ser verificada pela suite.
//
// so roda com docker disponivel. sem ele a classe e pulada, e o resto da suite continua
// sem depender de infraestrutura nenhuma.
//
// o banco tem nome proprio porque este contexto e descartado ao fim da classe
// (@DirtiesContext, para os consumidores nao ficarem ligados a um broker que ja parou),
// e o create-drop apagaria as tabelas do banco em memoria que os outros testes, em
// outro contexto, ainda estao usando.
@SpringBootTest(properties = {
        "engagement.messaging.start-consumers-on-ready=true",
        "spring.datasource.url=jdbc:h2:mem:engagement-mensageria;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext
class MessagingIntegrationTest {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

    private static final Duration ESPERA = Duration.ofSeconds(10);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    // uma fila temporaria assinando tudo o que este servico publica, no papel de um
    // assinante qualquer -- e o que o monolito faz com a fila dele
    private Queue observador;

    @BeforeEach
    void assinarEventosDoEngajamento() {
        observador = amqpAdmin.declareQueue();
        amqpAdmin.declareBinding(new Binding(observador.getName(), Binding.DestinationType.QUEUE,
                ENGAGEMENT_EXCHANGE, "#", null));
    }

    @AfterEach
    void limpar() {
        amqpAdmin.deleteQueue(observador.getName());
        commentRepository.deleteAll();
        reactionRepository.deleteAll();
    }

    private EngagementSnapshotMessage proximoEvento() {
        return rabbitTemplate.receiveAndConvert(observador.getName(), ESPERA.toMillis(),
                new ParameterizedTypeReference<EngagementSnapshotMessage>() {
                });
    }

    @Test
    void comandoPelaFila_viraComentario_eOEventoSaiComOTotalDoPost() {
        String envio = UUID.randomUUID().toString();

        rabbitTemplate.convertAndSend(COMMANDS_EXCHANGE, COMMENT_REGISTER,
                new RegisterCommentCommand(envio, 3L, "Carla", "chegou pelo broker", Instant.now()));

        await().atMost(ESPERA).until(() -> commentRepository.existsBySubmissionId(envio));

        EngagementSnapshotMessage evento = proximoEvento();
        assertThat(evento).isNotNull();
        assertThat(evento.change()).isEqualTo("comment.added");
        assertThat(evento.postId()).isEqualTo(3L);
        assertThat(evento.comments()).isEqualTo(1);
    }

    // a mensagem invalida e recusada de vez, sem retentativa, e o broker a desvia para a
    // dead letter da fila. ela nao se perde, e nao volta para a fila em loop
    @Test
    void comandoInvalido_vaiParaADeadLetter() {
        rabbitTemplate.convertAndSend(COMMANDS_EXCHANGE, COMMENT_REGISTER,
                new RegisterCommentCommand("envio-sem-texto", 3L, "Carla", "", Instant.now()));

        Message recusada = rabbitTemplate.receive(deadLetterQueueOf(COMMENT_COMMANDS_QUEUE), ESPERA.toMillis());

        assertThat(recusada).isNotNull();
        assertThat(new String(recusada.getBody(), StandardCharsets.UTF_8)).contains("envio-sem-texto");
        assertThat(commentRepository.count()).isZero();
    }

    // um corpo que nem e json nao chega ao listener: falha na conversao, que o spring
    // trata como fatal, e segue o mesmo caminho da dead letter
    @Test
    void mensagemIlegivel_vaiParaADeadLetter() {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
        rabbitTemplate.send(COMMANDS_EXCHANGE, COMMENT_REGISTER,
                MessageBuilder.withBody("isto nao e um comando".getBytes(StandardCharsets.UTF_8))
                        .andProperties(props).build());

        assertThat(rabbitTemplate.receive(deadLetterQueueOf(COMMENT_COMMANDS_QUEUE), ESPERA.toMillis()))
                .isNotNull();
    }

    @Test
    void postApagadoNoMonolito_limpaAConversa_eAvisaQueOPostSaiuDeCena() {
        commentRepository.save(new Comment(5L, "Carla", "vai sumir"));

        rabbitTemplate.convertAndSend(POSTS_EXCHANGE, POST_DELETED, new PostDeletedMessage(5L, Instant.now()));

        await().atMost(ESPERA).until(() -> commentRepository.countByPostId(5L) == 0);
        EngagementSnapshotMessage evento = proximoEvento();
        assertThat(evento.change()).isEqualTo("engagement.purged");
        assertThat(evento.postId()).isEqualTo(5L);
    }

    // um evento que ninguem assinou nao some: o alternate exchange de blog.posts o desvia
    // para blog.unrouted. sem isso, o broker descartaria em silencio
    @Test
    void eventoSemAssinante_naoSomeEVaiParaAFilaDeNaoRoteados() {
        rabbitTemplate.convertAndSend(POSTS_EXCHANGE, "post.published", new PostDeletedMessage(6L, Instant.now()));

        assertThat(rabbitTemplate.receive(UNROUTED_QUEUE, ESPERA.toMillis())).isNotNull();
    }
}
