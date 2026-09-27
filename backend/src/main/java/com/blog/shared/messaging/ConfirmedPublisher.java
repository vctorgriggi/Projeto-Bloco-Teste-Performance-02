package com.blog.shared.messaging;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// publica e so volta quando o broker confirmar que a mensagem foi aceita.
//
// o convertAndSend comum devolve o controle assim que os bytes saem pelo socket, o que
// nao garante nada: o broker pode cair logo depois, ou recusar a mensagem. para o que nao
// pode se perder -- o outbox, que marca a linha como publicada, e o comando de
// comentario, que responde 202 ao leitor --, "saiu do socket" nao basta. aqui cada
// publicacao espera o ack do broker (publisher confirm), e qualquer coisa diferente
// disso vira excecao para quem chamou decidir o que fazer.
//
// depende de spring.rabbitmq.publisher-confirm-type = simple, que esta no
// application.yml deste servico, e nao no config server: e uma decisao de codigo, sem a
// qual este componente nao funciona.
@Component
public class ConfirmedPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final long timeoutMillis;

    public ConfirmedPublisher(RabbitTemplate rabbitTemplate,
                              @Value("${blog.messaging.confirm-timeout-ms:5000}") long timeoutMillis) {
        this.rabbitTemplate = rabbitTemplate;
        this.timeoutMillis = timeoutMillis;
    }

    // mensagem ja montada (o outbox guarda os bytes exatos que vao sair)
    public void send(String exchange, String routingKey, Message message) {
        rabbitTemplate.invoke(operations -> {
            operations.send(exchange, routingKey, message);
            operations.waitForConfirmsOrDie(timeoutMillis);
            return null;
        });
    }

    // objeto convertido em json pelo conversor configurado
    public void convertAndSend(String exchange, String routingKey, Object payload, MessagePostProcessor postProcessor) {
        rabbitTemplate.invoke(operations -> {
            operations.convertAndSend(exchange, routingKey, payload, postProcessor);
            operations.waitForConfirmsOrDie(timeoutMillis);
            return null;
        });
    }
}
