package com.blog.engagement.service;

import com.blog.engagement.client.EngagementClient;
import com.blog.engagement.client.dto.CommentView;
import com.blog.engagement.client.dto.NewComment;
import com.blog.engagement.web.dto.CommentRequest;
import com.blog.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

// casos de uso de comentario do ponto de vista do monolito. o que este service faz
// mudou de natureza na terceira entrega: antes ele gravava em um repositorio local,
// agora ele compoe -- valida o que e da sua alcada e delega o resto ao servico dono
// do dado.
//
// duas coisas continuam exatamente como eram, e isso e o ponto:
//
// 1. a porta PostCatalog. a checagem "esse post existe?" nunca foi feita pelo
//    repositorio de posts direto, e sim por essa interface. o que era uma chamada
//    local hoje protege uma chamada de rede, e nenhuma linha dela precisou mudar.
// 2. a ordem das operacoes. o post e validado aqui, antes de a requisicao sair da
//    maquina: quem e dono do post e este servico, e mandar o microsservico
//    perguntar de volta criaria uma dependencia circular entre os dois processos.
//
// o que desapareceu foi o @Transactional. nao ha mais transacao local para abrir, e
// abrir uma seria pior do que inutil: manteria uma conexao do pool presa enquanto a
// chamada http espera resposta.
@Service
public class CommentService {

    private final EngagementClient engagementClient;
    private final PostCatalog postCatalog;

    public CommentService(EngagementClient engagementClient, PostCatalog postCatalog) {
        this.engagementClient = engagementClient;
        this.postCatalog = postCatalog;
    }

    public CommentView addToPost(Long postId, CommentRequest request) {
        requirePost(postId);
        return engagementClient.addComment(postId, new NewComment(request.authorName(), request.content()));
    }

    public List<CommentView> findByPost(Long postId) {
        requirePost(postId);
        return engagementClient.listComments(postId);
    }

    // o comentario e identificado por id proprio, que so o microsservico conhece.
    // aqui nao ha nada para validar antes: se o id nao existir, o 404 vem de la e o
    // decoder do cliente o repassa como 404 nosso.
    public void delete(Long commentId) {
        engagementClient.deleteComment(commentId);
    }

    private void requirePost(Long postId) {
        if (!postCatalog.postExists(postId)) {
            throw new ResourceNotFoundException("Post " + postId + " nao encontrado");
        }
    }
}
