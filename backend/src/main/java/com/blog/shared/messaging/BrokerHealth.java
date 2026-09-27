package com.blog.shared.messaging;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.stereotype.Component;

// responde se o broker esta alcancavel agora. alimenta o diagnostico da integracao,
// porque com a mensageria o sistema passou a ter duas pecas externas que podem cair
// independentes uma da outra: o engajamento e o broker. com o engajamento fora e o broker
// de pe, comentar ainda funciona (o recado espera na fila); com o broker fora, nao.
//
// a conexao do CachingConnectionFactory e compartilhada, e fechar o proxy devolvido nao
// fecha a conexao de verdade -- o try-with-resources aqui so devolve o que pegou.
@Component
public class BrokerHealth {

    private final ConnectionFactory connectionFactory;

    public BrokerHealth(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public boolean available() {
        try (Connection connection = connectionFactory.createConnection()) {
            return connection.isOpen();
        } catch (AmqpException e) {
            return false;
        }
    }
}
