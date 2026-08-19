package com.blog.engagement.client;

import com.blog.engagement.client.dto.CommentView;
import com.blog.engagement.client.dto.NewComment;
import com.blog.engagement.client.dto.NewReaction;
import com.blog.engagement.client.dto.PurgeView;
import com.blog.engagement.client.dto.ReactionSummaryView;
import com.blog.engagement.client.dto.ServiceInfoView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// a fronteira de rede do monolito com o microsservico, escrita como uma interface.
// o spring cloud openfeign gera a implementacao: cada metodo vira uma requisicao
// http, e a serializacao do corpo e da resposta usa o mesmo jackson dos controllers.
//
// o "engagement-service" do name nao e um host: e o nome logico com que o outro
// processo se registrou no eureka. na hora da chamada, o spring cloud loadbalancer
// consulta o registro, escolhe uma instancia e monta a url. e isso que permite
// subir uma segunda instancia do engajamento sem tocar em nenhuma linha daqui.
//
// o url tem um valor padrao vazio, e vazio significa "resolva pela descoberta".
// definir engagement.service.url aponta o cliente direto para um endereco fixo,
// util para rodar a stack sem o eureka ou para apontar um teste a um servidor
// http local.
@FeignClient(
        name = "engagement-service",
        url = "${engagement.service.url:}",
        fallbackFactory = EngagementFallbackFactory.class
)
public interface EngagementClient {

    @GetMapping("/api/posts/{postId}/comments")
    List<CommentView> listComments(@PathVariable("postId") Long postId);

    @PostMapping("/api/posts/{postId}/comments")
    CommentView addComment(@PathVariable("postId") Long postId, @RequestBody NewComment body);

    @DeleteMapping("/api/comments/{commentId}")
    void deleteComment(@PathVariable("commentId") Long commentId);

    @GetMapping("/api/posts/{postId}/reactions")
    ReactionSummaryView reactionSummary(@PathVariable("postId") Long postId,
                                        @RequestParam(name = "reader", required = false) String reader);

    @PostMapping("/api/posts/{postId}/reactions")
    ReactionSummaryView react(@PathVariable("postId") Long postId, @RequestBody NewReaction body);

    @DeleteMapping("/api/posts/{postId}/reactions/{type}")
    ReactionSummaryView undoReaction(@PathVariable("postId") Long postId,
                                     @PathVariable("type") String type,
                                     @RequestParam("reader") String reader);

    // limpeza do engajamento de um post que deixou de existir
    @DeleteMapping("/api/posts/{postId}/engagement")
    PurgeView purgePost(@PathVariable("postId") Long postId);

    @GetMapping("/api/engagement/ping")
    ServiceInfoView ping();
}
