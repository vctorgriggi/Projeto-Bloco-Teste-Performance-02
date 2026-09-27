package com.blog.engagement.web.dto;

import com.blog.engagement.messaging.RegisterCommentCommand;

import java.time.Instant;

// a resposta do POST de comentario: o que foi aceito, e nao o que foi gravado.
//
// o status PENDING diz ao front que o recado esta na fila. o submissionId e a ponte: o
// comentario, quando for gravado pelo engajamento, vai aparecer na listagem com este
// mesmo id, e e assim que a interface troca o "na fila" pelo comentario de verdade.
public record CommentSubmissionResponse(
        String submissionId,
        Long postId,
        String authorName,
        String content,
        Instant submittedAt,
        String status
) {
    public static CommentSubmissionResponse from(RegisterCommentCommand command) {
        return new CommentSubmissionResponse(
                command.submissionId(),
                command.postId(),
                command.authorName(),
                command.content(),
                command.submittedAt(),
                "PENDING"
        );
    }
}
