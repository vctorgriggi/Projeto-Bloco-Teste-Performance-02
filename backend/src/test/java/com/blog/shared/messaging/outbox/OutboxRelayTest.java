package com.blog.shared.messaging.outbox;

import com.blog.shared.messaging.ConfirmedPublisher;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// o relay do outbox, com o publicador dublado. o que se testa e a decisao do relay em
// cada situacao: o que sai, em que ordem, o que acontece quando o broker nao confirma, e
// o que nunca sai duas vezes. o agendamento fica desligado no perfil de teste, entao o
// relay so roda quando o teste manda.
@SpringBootTest
@ActiveProfiles("test")
class OutboxRelayTest {

    @MockBean
    private ConfirmedPublisher publisher;

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private OutboxWriter writer;

    @Autowired
    private OutboxRepository repository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    @AfterEach
    void limparOutbox() {
        repository.deleteAll();
    }

    private OutboxMessage gravar(long postId) {
        return transactionTemplate.execute(status -> writer.record("blog.posts", "post.deleted", "post.deleted",
                Map.of("postId", postId)));
    }

    @Test
    void pendentes_saemNaOrdemEmQueForamGravadas_eFicamMarcadas() {
        OutboxMessage primeira = gravar(1);
        OutboxMessage segunda = gravar(2);

        int publicadas = relay.relayPending();

        assertThat(publicadas).isEqualTo(2);
        ArgumentCaptor<Message> enviadas = ArgumentCaptor.forClass(Message.class);
        verify(publisher, times(2)).send(eq("blog.posts"), eq("post.deleted"), enviadas.capture());
        assertThat(enviadas.getAllValues())
                .extracting(m -> m.getMessageProperties().getMessageId())
                .containsExactly(primeira.getId(), segunda.getId());
        assertThat(repository.countByPublishedAtIsNull()).isZero();
    }

    // o message id e o id da linha, e o corpo sao os bytes gravados: se a mesma linha
    // sair duas vezes, o consumidor ve a mesma mensagem
    @Test
    void mensagemPublicada_levaOIdDaLinhaEOCorpoGravado() {
        OutboxMessage gravada = gravar(7);

        relay.relayPending();

        ArgumentCaptor<Message> enviada = ArgumentCaptor.forClass(Message.class);
        verify(publisher).send(anyString(), anyString(), enviada.capture());
        Message mensagem = enviada.getValue();
        assertThat(mensagem.getMessageProperties().getMessageId()).isEqualTo(gravada.getId());
        assertThat(mensagem.getMessageProperties().getType()).isEqualTo("post.deleted");
        assertThat(mensagem.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(new String(mensagem.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"postId\":7}");
    }

    // broker fora: a mensagem continua pendente, com a tentativa e o erro anotados, e o
    // lote para ali -- as seguintes nao passam na frente dela
    @Test
    void brokerForaDoAr_mensagemContinuaPendenteEOLoteParaNaPrimeiraFalha() {
        gravar(1);
        gravar(2);
        willThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .given(publisher).send(anyString(), anyString(), any(Message.class));

        int publicadas = relay.relayPending();

        assertThat(publicadas).isZero();
        verify(publisher, times(1)).send(anyString(), anyString(), any(Message.class));
        assertThat(repository.countByPublishedAtIsNull()).isEqualTo(2);
        OutboxMessage primeira = repository.findTop50ByPublishedAtIsNullOrderByCreatedAtAsc().get(0);
        assertThat(primeira.getAttempts()).isEqualTo(1);
        assertThat(primeira.getLastError()).contains("Connection refused");
    }

    // quando o broker volta, a proxima varredura entrega o que ficou -- sem ninguem
    // precisar reenviar nada
    @Test
    void brokerDeVolta_aProximaVarreduraEntregaOQueFicou() {
        gravar(1);
        willThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .given(publisher).send(anyString(), anyString(), any(Message.class));
        relay.relayPending();

        reset(publisher);
        int publicadas = relay.relayPending();

        assertThat(publicadas).isEqualTo(1);
        assertThat(repository.countByPublishedAtIsNull()).isZero();
    }

    @Test
    void mensagemJaPublicada_naoSaiDeNovo() {
        gravar(1);
        relay.relayPending();
        clearInvocations(publisher);

        relay.relayPending();

        verify(publisher, never()).send(anyString(), anyString(), any(Message.class));
    }

    // o relay publica de uma thread agendada, sem trace. a mensagem guarda o contexto de
    // quem a gravou, e o relay publica dentro dele: com o agente do opentelemetry, a
    // exclusao do post, a publicacao e a limpeza no outro servico viram um trace so
    @Test
    void mensagemSaiNoContextoDoTraceDeQuemAGravou() {
        SpanContext daRequisicao = SpanContext.createFromRemoteParent(
                "4bf92f3577b34da6a3ce929d0e0e4736", "00f067aa0ba902b7", TraceFlags.getSampled(), TraceState.getDefault());
        OutboxMessage gravada;
        try (Scope ignorado = Context.root().with(Span.wrap(daRequisicao)).makeCurrent()) {
            gravada = gravar(1);
        }
        assertThat(gravada.getTraceParent()).isEqualTo("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        AtomicReference<String> traceNaPublicacao = new AtomicReference<>();
        willAnswer(invocacao -> {
            traceNaPublicacao.set(Span.current().getSpanContext().getTraceId());
            return null;
        }).given(publisher).send(anyString(), anyString(), any(Message.class));

        relay.relayPending();

        assertThat(traceNaPublicacao.get()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    // sem trace em andamento (desenvolvimento, sem o agente), nada e gravado nem restaurado
    @Test
    void semTraceEmAndamento_naoGuardaContexto() {
        assertThat(gravar(1).getTraceParent()).isNull();

        relay.relayPending();

        assertThat(repository.countByPublishedAtIsNull()).isZero();
    }
}
