package com.blog.engagement.messaging;

import com.blog.engagement.messaging.message.RegisterCommentCommand;
import com.blog.engagement.service.CommentService;
import com.blog.engagement.shared.InvalidMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;

import static com.blog.engagement.messaging.Topology.COMMENT_COMMANDS_QUEUE;

// consome os comandos de comentario enviados pelo monolito.
//
// e o padrao de fila de trabalho (point-to-point, competing consumers): cada mensagem e
// entregue a um consumidor so. subir uma segunda instancia deste servico divide a fila
// entre as duas sem configurar nada, e o numero de consumidores por instancia vem do
// config server (concurrency / max-concurrency). e aqui que a escrita de comentarios
// passou a escalar independente do monolito.
//
// o listener e fino de proposito, como um controller: confere o formato da mensagem,
// traduz para a chamada do service e deixa a regra la. a validacao aqui e o equivalente
// do @Valid: o monolito ja validou antes de enviar, mas uma fila aceita mensagem de
// qualquer um, e este servico nao confia na palavra de quem mandou.
@Component
public class CommentCommandListener {

    private static final Logger log = LoggerFactory.getLogger(CommentCommandListener.class);

    private final CommentService commentService;

    public CommentCommandListener(CommentService commentService) {
        this.commentService = commentService;
    }

    @RabbitListener(queues = COMMENT_COMMANDS_QUEUE, id = "comment-commands")
    public void onRegisterComment(RegisterCommentCommand command) {
        validar(command);

        Instant enviadoEm = command.submittedAt() != null ? command.submittedAt() : Instant.now();
        commentService.register(command.submissionId(), command.postId(),
                        command.authorName().strip(), command.content().strip(), enviadoEm)
                .ifPresentOrElse(
                        comment -> log.info("comentario {} registrado no post {} (envio {})",
                                comment.getId(), command.postId(), command.submissionId()),
                        () -> log.info("envio {} ja processado antes; mensagem repetida ignorada",
                                command.submissionId()));
    }

    private void validar(RegisterCommentCommand command) {
        if (!StringUtils.hasText(command.submissionId())) {
            throw new InvalidMessageException("comando de comentario sem submissionId");
        }
        if (command.postId() == null) {
            throw new InvalidMessageException("comando de comentario " + command.submissionId() + " sem postId");
        }
        if (!StringUtils.hasText(command.authorName()) || !StringUtils.hasText(command.content())) {
            throw new InvalidMessageException(
                    "comando de comentario " + command.submissionId() + " sem autor ou sem texto");
        }
    }
}
