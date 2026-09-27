package com.blog.shared.messaging;

import com.blog.authoring.domain.Post;
import com.blog.authoring.repository.PostRepository;
import com.blog.authoring.service.PostService;
import com.blog.engagement.client.EngagementClient;
import com.blog.engagement.messaging.EngagementSnapshotMessage;
import com.blog.engagement.projection.EngagementCounterRepository;
import com.blog.shared.messaging.outbox.OutboxMessage;
import com.blog.shared.messaging.outbox.OutboxRelay;
import com.blog.shared.messaging.outbox.OutboxRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static com.blog.shared.messaging.Topology.COMMENT_COMMANDS_QUEUE;
import static com.blog.shared.messaging.Topology.ENGAGEMENT_EXCHANGE;
import static com.blog.shared.messaging.Topology.ENGAGEMENT_SNAPSHOTS_QUEUE;
import static com.blog.shared.messaging.Topology.POSTS_EXCHANGE;
import static com.blog.shared.messaging.Topology.POST_DELETED;
import static com.blog.shared.messaging.Topology.deadLetterQueueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// a mensageria do monolito de ponta a ponta, com um rabbitmq de verdade em container.
//
// os outros testes dublam o publicador e chamam os listeners como metodos; este cobre o
// que so aparece com o broker no meio: a confirmacao de publicacao do relay, o roteamento
// pela topologia declarada, o comando esperando na fila de um destinatario que nao esta
// no ar, e a mensagem invalida desviada para a dead letter.
//
// so roda com docker disponivel; sem ele, a classe e pulada. o banco tem nome proprio
// porque este contexto e descartado ao fim da classe, e o create-drop apagaria as
// tabelas do banco em memoria que os outros testes ainda usam.
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=true",
        "spring.datasource.url=jdbc:h2:mem:blogdb-mensageria;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext
class MessagingIntegrationTest {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

    private static final Duration ESPERA = Duration.ofSeconds(10);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private EngagementCounterRepository counterRepository;

    // nenhuma chamada http ao engajamento acontece nestes cenarios; o duble so garante que
    // nada tente sair por esse caminho
    @MockBean
    private EngagementClient engagementClient;

    @AfterEach
    void limpar() {
        outboxRepository.deleteAll();
        postRepository.deleteAll();
        counterRepository.deleteAll();
    }

    // o caminho inteiro do outbox: a exclusao grava a linha, o relay publica com
    // confirmacao do broker, e a mensagem chega a quem assinou post.deleted com o id da
    // linha como message id
    @Test
    void postApagado_saiPeloOutboxEChegaAQuemAssinou() {
        Queue assinante = amqpAdmin.declareQueue();
        amqpAdmin.declareBinding(new Binding(assinante.getName(), Binding.DestinationType.QUEUE,
                POSTS_EXCHANGE, POST_DELETED, null));
        Long postId = postRepository.save(new Post("Titulo", "conteudo", 1L)).getId();

        postService.delete(postId);
        OutboxMessage gravada = outboxRepository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc().get(0);
        int publicadas = outboxRelay.relayPending();

        assertThat(publicadas).isEqualTo(1);
        assertThat(outboxRepository.countByPublishedAtIsNull()).isZero();
        Message recebida = rabbitTemplate.receive(assinante.getName(), ESPERA.toMillis());
        assertThat(recebida).isNotNull();
        assertThat(recebida.getMessageProperties().getMessageId()).isEqualTo(gravada.getId());
        assertThat(recebida.getMessageProperties().getType()).isEqualTo("post.deleted");
        assertThat(recebida.getMessageProperties().getAppId()).isEqualTo("blog-api");
        assertThat(new String(recebida.getBody(), StandardCharsets.UTF_8)).contains("\"postId\":" + postId);
    }

    // o engajamento nao esta no ar neste teste, e isso e o ponto: o comentario e aceito
    // mesmo assim, e o comando fica esperando na caixa de entrada dele -- que existe
    // porque o monolito a declara tambem. sem nenhum cabecalho com nome de classe java
    @Test
    void comentario_viraComandoQueEsperaNaFilaDoEngajamento() throws Exception {
        Long postId = postRepository.save(new Post("Titulo", "conteudo", 1L)).getId();

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"authorName":"Carla","content":"espero na fila"}
                                """))
                .andExpect(status().isAccepted());

        Message comando = rabbitTemplate.receive(COMMENT_COMMANDS_QUEUE, ESPERA.toMillis());
        assertThat(comando).isNotNull();
        assertThat(comando.getMessageProperties().getType()).isEqualTo("comment.register");
        assertThat(comando.getMessageProperties().getHeaders()).doesNotContainKey("__TypeId__");
        assertThat(new String(comando.getBody(), StandardCharsets.UTF_8))
                .contains("\"content\":\"espero na fila\"")
                .contains("\"submissionId\":\"" + comando.getMessageProperties().getMessageId() + "\"");
    }

    // o engajamento publica, a fila deste servico recebe, e os contadores se atualizam
    @Test
    void mudancaPublicadaPeloEngajamento_atualizaOsContadores() {
        rabbitTemplate.convertAndSend(ENGAGEMENT_EXCHANGE, "reaction.added",
                new EngagementSnapshotMessage(42L, 2, 5, "reaction.added", Instant.now()));

        await().atMost(ESPERA).until(() -> counterRepository.findById(42L).isPresent());
        assertThat(counterRepository.findById(42L).orElseThrow().getReactions()).isEqualTo(5);
    }

    // uma mensagem sem postId e recusada sem retentativa e desviada para a dead letter
    @Test
    void mudancaInvalida_vaiParaADeadLetter() {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        rabbitTemplate.send(ENGAGEMENT_EXCHANGE, "comment.added", MessageBuilder
                .withBody("{\"comments\":1,\"change\":\"comment.added\"}".getBytes(StandardCharsets.UTF_8))
                .andProperties(props).build());

        assertThat(rabbitTemplate.receive(deadLetterQueueOf(ENGAGEMENT_SNAPSHOTS_QUEUE), ESPERA.toMillis()))
                .isNotNull();
        assertThat(counterRepository.count()).isZero();
    }
}
