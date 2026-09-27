package com.blog.engagement.web;

import com.blog.engagement.service.CommentService;
import com.blog.engagement.web.dto.CommentRequest;
import com.blog.engagement.web.dto.CommentResponse;
import com.blog.engagement.web.dto.CommentSubmissionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// as rotas de comentario nao mudaram: mesmos caminhos, mesmos verbos. o que mudou esta
// uma camada abaixo, no service, que agora conversa com outro processo. e esse o teste
// pratico de que a fronteira estava no lugar certo antes de virar fronteira de rede.
//
// a unica mudanca visivel na quarta entrega e o status do POST: 202 Accepted, e nao
// mais 201 Created. o comentario ainda nao existe quando a resposta sai -- ele foi
// aceito e esta na fila --, e 201 seria mentir sobre isso. o corpo traz o que o leitor
// enviou e o submissionId, com que a interface reconhece o recado quando ele aparecer.
@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommentSubmissionResponse add(@PathVariable Long postId, @Valid @RequestBody CommentRequest request) {
        return CommentSubmissionResponse.from(commentService.addToPost(postId, request));
    }

    @GetMapping("/posts/{postId}/comments")
    public List<CommentResponse> listByPost(@PathVariable Long postId) {
        return commentService.findByPost(postId).stream().map(CommentResponse::from).toList();
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long commentId) {
        commentService.delete(commentId);
        return ResponseEntity.noContent().build();
    }
}
