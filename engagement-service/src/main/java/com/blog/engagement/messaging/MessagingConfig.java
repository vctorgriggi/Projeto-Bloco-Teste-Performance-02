package com.blog.engagement.messaging;

import com.blog.engagement.shared.InvalidMessageException;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.blog.engagement.messaging.Topology.COMMANDS_EXCHANGE;
import static com.blog.engagement.messaging.Topology.COMMENT_COMMANDS_QUEUE;
import static com.blog.engagement.messaging.Topology.COMMENT_REGISTER;
import static com.blog.engagement.messaging.Topology.DEAD_LETTER_EXCHANGE;
import static com.blog.engagement.messaging.Topology.ENGAGEMENT_EXCHANGE;
import static com.blog.engagement.messaging.Topology.POSTS_EXCHANGE;
import static com.blog.engagement.messaging.Topology.POST_DELETED;
import static com.blog.engagement.messaging.Topology.POST_DELETED_QUEUE;
import static com.blog.engagement.messaging.Topology.SNAPSHOT_REQUEST;
import static com.blog.engagement.messaging.Topology.SNAPSHOT_REQUESTS_QUEUE;
import static com.blog.engagement.messaging.Topology.UNROUTED_EXCHANGE;
import static com.blog.engagement.messaging.Topology.UNROUTED_QUEUE;
import static com.blog.engagement.messaging.Topology.deadLetterQueueOf;

// liga este servico ao rabbitmq. quase tudo aqui e declarativo, e e isso que o spring
// boot da de graca: a conexao, o RabbitTemplate e a fabrica de consumidores vem da
// auto-configuracao e das propriedades spring.rabbitmq.*; os beans abaixo so descrevem
// o formato das mensagens e a topologia, e o RabbitAdmin cria exchanges, filas e
// bindings no broker na primeira conexao -- e de novo depois de cada reconexao.
@Configuration
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
    // classe java. o message id vem do conversor (ou de quem publicou, se ja definiu um).
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

    // a retentativa dos consumidores vem ligada por propriedade (spring.rabbitmq.listener
    // .simple.retry), com tentativas e intervalo definidos no config server. o que as
    // propriedades nao expressam e a diferenca entre dois tipos de falha:
    //
    // - uma falha passageira (banco ocupado, conflito de concorrencia) pode dar certo na
    //   proxima tentativa, e merece retentativa;
    // - uma mensagem invalida vai falhar igual todas as vezes. retentar so atrasa a ida
    //   dela para a dead letter e segura o consumidor enquanto isso.
    //
    // por isso InvalidMessageException e marcada como nao retentavel, e junto dela as
    // falhas de conversao (um corpo que nem e json, um campo com tipo errado). essas
    // entraram depois de medir: sem elas, uma mensagem ilegivel levava tres tentativas e
    // alguns segundos para chegar a dead letter, em vez de ir na hora. a busca percorre a
    // cadeia de causas porque o container embrulha o erro do listener.
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

    // as filas que este servico consome. o consumidor e o dono da fila dele: e ele quem
    // decide o nome, a durabilidade e o que assina.
    @Bean
    Declarables filasDoEngajamento() {
        List<Declarable> declaraveis = new ArrayList<>();

        // evento: o post foi apagado no monolito -> limpar a conversa dele aqui
        declaraveis.addAll(filaComDeadLetter(POST_DELETED_QUEUE, POSTS_EXCHANGE, POST_DELETED));

        // comando: registrar um comentario enviado pelo leitor
        declaraveis.addAll(filaComDeadLetter(COMMENT_COMMANDS_QUEUE, COMMANDS_EXCHANGE, COMMENT_REGISTER));

        // comando: republicar o estado do engajamento de todos os posts
        declaraveis.addAll(filaComDeadLetter(SNAPSHOT_REQUESTS_QUEUE, COMMANDS_EXCHANGE, SNAPSHOT_REQUEST));

        return new Declarables(declaraveis);
    }

    // uma fila duravel, a dead letter dela, e o binding de cada uma ao seu exchange.
    //
    // a mensagem recusada pelo consumidor (depois das retentativas, ou de cara, se for
    // invalida) e roteada pelo broker para blog.dlx com a routing key = nome da fila de
    // origem, e dali para <fila>.dlq. ela nao se perde nem volta para a fila em loop: fica
    // parada onde alguem pode inspecionar e, se fizer sentido, reenviar.
    static List<Declarable> filaComDeadLetter(String nome, String exchange, String routingKey) {
        Queue fila = QueueBuilder.durable(nome)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(nome)
                .build();
        Queue deadLetter = QueueBuilder.durable(deadLetterQueueOf(nome)).build();

        Binding assinatura = new Binding(nome, Binding.DestinationType.QUEUE, exchange, routingKey, null);
        Binding paraDeadLetter = new Binding(deadLetterQueueOf(nome), Binding.DestinationType.QUEUE,
                DEAD_LETTER_EXCHANGE, nome, null);

        return List.of(fila, deadLetter, assinatura, paraDeadLetter);
    }
}
