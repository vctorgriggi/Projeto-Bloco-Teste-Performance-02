package com.blog.engagement.messaging;

import com.blog.shared.exception.ServiceUnavailableException;
import com.blog.shared.messaging.ConfirmedPublisher;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Component;

import static com.blog.shared.messaging.Topology.COMMANDS_EXCHANGE;
import static com.blog.shared.messaging.Topology.COMMENT_REGISTER;

// envia o comando de comentario e so volta quando o broker confirmar que o guardou.
//
// sem outbox, e a razao e que aqui nao ha escrita local para amarrar: o monolito nao
// grava nada quando o leitor comenta. o outbox resolve a escrita dupla (banco + broker);
// com uma escrita so, basta a confirmacao do broker. se ela nao vier, o leitor recebe
// 503 e o texto continua no formulario -- nada se perde em silencio.
//
// o message id e o proprio submissionId, o que liga a mensagem no broker ao comentario
// que ela vai virar.
@Component
public class CommentCommandSender {

    private final ConfirmedPublisher publisher;

    public CommentCommandSender(ConfirmedPublisher publisher) {
        this.publisher = publisher;
    }

    public void send(RegisterCommentCommand command) {
        try {
            publisher.convertAndSend(COMMANDS_EXCHANGE, COMMENT_REGISTER, command, message -> {
                message.getMessageProperties().setMessageId(command.submissionId());
                message.getMessageProperties().setType(RegisterCommentCommand.TYPE);
                return message;
            });
        } catch (AmqpException e) {
            throw new ServiceUnavailableException(
                    "A fila de mensagens esta indisponivel e o comentario nao foi enviado. Tente novamente em instantes.",
                    e);
        }
    }
}
