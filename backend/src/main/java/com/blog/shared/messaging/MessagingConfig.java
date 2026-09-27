package com.blog.shared.messaging;

import com.blog.shared.exception.InvalidMessageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.blog.shared.messaging.Topology.COMMANDS_EXCHANGE;
import static com.blog.shared.messaging.Topology.COMMENT_COMMANDS_QUEUE;
import static com.blog.shared.messaging.Topology.COMMENT_REGISTER;
import static com.blog.shared.messaging.Topology.DEAD_LETTER_EXCHANGE;
import static com.blog.shared.messaging.Topology.ENGAGEMENT_CHANGES;
import static com.blog.shared.messaging.Topology.ENGAGEMENT_EXCHANGE;
import static com.blog.shared.messaging.Topology.ENGAGEMENT_SNAPSHOTS_QUEUE;
import static com.blog.shared.messaging.Topology.POSTS_EXCHANGE;
import static com.blog.shared.messaging.Topology.SNAPSHOT_REQUEST;
import static com.blog.shared.messaging.Topology.SNAPSHOT_REQUESTS_QUEUE;
import static com.blog.shared.messaging.Topology.UNROUTED_EXCHANGE;
import static com.blog.shared.messaging.Topology.UNROUTED_QUEUE;
import static com.blog.shared.messaging.Topology.deadLetterQueueOf;

// liga o monolito ao rabbitmq. quase tudo aqui e declarativo, e e isso que o spring boot
// da de graca: a conexao, o RabbitTemplate e a fabrica de consumidores vem da
// auto-configuracao e das propriedades spring.rabbitmq.*; os beans abaixo so descrevem o
// formato das mensagens e a topologia, e o RabbitAdmin cria exchanges, filas e bindings
// no broker na primeira conexao -- e de novo depois de cada reconexao.
//
// o @EnableScheduling e para o relay do outbox, que varre as mensagens pendentes em
// intervalo fixo (veja OutboxRelayScheduler).
@Configuration
@EnableScheduling
public class MessagingConfig {

    // cabecalhos que o conversor do spring escreve com o nome da classe java de quem
    // publicou. o contrato entre os servicos e o json do corpo, nao uma classe: quem
    // consome desserializa no tipo que ele mesmo declarou no @RabbitListener.
    private static final List<String> JAVA_TYPE_HEADERS = List.of("__TypeId__", "__ContentTypeId__", "__KeyTypeId__");

    // json com o mesmo ObjectMapper dos controllers (instantes em iso-8601, campos
    // desconhecidos ignorados). a precedencia INFERRED faz o consumidor usar o tipo do
    // parametro do listener, e nao um nome de classe vindo no cabecalho -- e isso que
    // permite a cada servico ter o proprio record para a mesma mensagem.
    @Bean
    MessageConverter messageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setCreateMessageIds(true);
        return converter;
    }

    // toda mensagem que sai daqui leva quem publicou e quando, e nenhuma leva nome de
    // classe java. o message id vem do conversor, ou de quem publicou, se ja definiu um
    // (o outbox usa o id da propria linha; o comando de comentario, o id do envio).
    @Bean
    RabbitTemplateCustomizer carimboDeOrigem(@Value("${spring.application.name}") String applicationName) {
        return template -> template.setBeforePublishPostProcessors(message -> {
            MessageProperties props = message.getMessageProperties();
            props.setAppId(applicationName);
            if (props.getTimestamp() == null) {
                props.setTimestamp(new Date());
            }
            JAVA_TYPE_HEADERS.forEach(props.getHeaders()::remove);
            return message;
        });
    }

    // a retentativa dos consumidores vem ligada por propriedade, com tentativas e
    // intervalo definidos no config server. o que as propriedades nao expressam e que uma
    // mensagem invalida ou ilegivel vai falhar igual em todas as tentativas: retentar so
    // atrasa a ida dela para a dead letter. por isso InvalidMessageException e as falhas de
    // conversao sao marcadas como nao retentaveis. a busca percorre a cadeia de causas
    // porque o container embrulha o erro.
    @Bean
    RabbitRetryTemplateCustomizer naoRetentarMensagemInvalida(RabbitProperties rabbitProperties) {
        return (target, retryTemplate) -> {
            if (target != RabbitRetryTemplateCustomizer.Target.LISTENER) {
                return;
            }
            int tentativas = rabbitProperties.getListener().getSimple().getRetry().getMaxAttempts();
            retryTemplate.setRetryPolicy(new SimpleRetryPolicy(tentativas, Map.of(
                    InvalidMessageException.class, false,
                    org.springframework.amqp.support.converter.MessageConversionException.class, false,
                    org.springframework.messaging.converter.MessageConversionException.class, false,
                    MethodArgumentResolutionException.class, false), true, true));
        };
    }

    // ---- topologia -------------------------------------------------------------------

    // os exchanges sao declarados pelos dois servicos, com argumentos identicos. declarar
    // e idempotente, e fazer dos dois lados tira a ordem de subida da equacao: quem subir
    // primeiro cria, o outro confirma.
    @Bean
    Declarables exchanges() {
        Exchange posts = ExchangeBuilder.topicExchange(POSTS_EXCHANGE).durable(true)
                .alternate(UNROUTED_EXCHANGE).build();
        Exchange engagement = ExchangeBuilder.topicExchange(ENGAGEMENT_EXCHANGE).durable(true)
                .alternate(UNROUTED_EXCHANGE).build();
        DirectExchange commands = ExchangeBuilder.directExchange(COMMANDS_EXCHANGE).durable(true).build();
        DirectExchange deadLetters = ExchangeBuilder.directExchange(DEAD_LETTER_EXCHANGE).durable(true).build();

        FanoutExchange unrouted = ExchangeBuilder.fanoutExchange(UNROUTED_EXCHANGE).durable(true).build();
        Queue unroutedQueue = QueueBuilder.durable(UNROUTED_QUEUE).build();

        return new Declarables(posts, engagement, commands, deadLetters, unrouted, unroutedQueue,
                BindingBuilder.bind(unroutedQueue).to(unrouted));
    }

    // a fila deste servico: assina as mudancas de engajamento para manter os contadores.
    @Bean
    Declarables filaDosContadores() {
        return new Declarables(filaComDeadLetter(ENGAGEMENT_SNAPSHOTS_QUEUE, ENGAGEMENT_EXCHANGE, ENGAGEMENT_CHANGES));
    }

    // as caixas de entrada dos comandos que este servico envia. elas pertencem ao
    // engagement-service, que as declara igual; declarar tambem daqui e a diferenca entre
    // evento e comando posta em pratica:
    //
    // - um evento e publicado para quem quiser ouvir. se ninguem assinou ainda, nao e
    //   problema de quem publicou (e o alternate exchange guarda a mensagem).
    // - um comando tem destinatario. se o engajamento nunca subiu contra este broker, a
    //   fila dele ainda nao existe, e o comentario do leitor seria descartado. declarando
    //   daqui, o comando espera na fila ate o destinatario aparecer.
    @Bean
    Declarables caixasDeEntradaDosComandos() {
        List<Declarable> declaraveis = new ArrayList<>();
        declaraveis.addAll(filaComDeadLetter(COMMENT_COMMANDS_QUEUE, COMMANDS_EXCHANGE, COMMENT_REGISTER));
        declaraveis.addAll(filaComDeadLetter(SNAPSHOT_REQUESTS_QUEUE, COMMANDS_EXCHANGE, SNAPSHOT_REQUEST));
        return new Declarables(declaraveis);
    }

    // uma fila duravel, a dead letter dela, e os bindings. a mensagem recusada pelo
    // consumidor (depois das retentativas, ou de cara, se for invalida) e roteada pelo
    // broker para blog.dlx com a routing key = nome da fila de origem, e dali para
    // <fila>.dlq, onde fica parada para inspecao e, se fizer sentido, reenvio.
    static List<Declarable> filaComDeadLetter(String nome, String exchange, String... routingKeys) {
        Queue fila = QueueBuilder.durable(nome)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(nome)
                .build();
        Queue deadLetter = QueueBuilder.durable(deadLetterQueueOf(nome)).build();

        List<Declarable> declaraveis = new ArrayList<>(List.of(fila, deadLetter));
        for (String routingKey : routingKeys) {
            declaraveis.add(new Binding(nome, Binding.DestinationType.QUEUE, exchange, routingKey, null));
        }
        declaraveis.add(new Binding(deadLetterQueueOf(nome), Binding.DestinationType.QUEUE,
                DEAD_LETTER_EXCHANGE, nome, null));
        return declaraveis;
    }
}
