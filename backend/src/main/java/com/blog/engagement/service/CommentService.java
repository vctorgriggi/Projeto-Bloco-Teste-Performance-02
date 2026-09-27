package com.blog.engagement.service;

import com.blog.engagement.client.EngagementClient;
import com.blog.engagement.client.dto.CommentView;
import com.blog.engagement.messaging.CommentCommandSender;
import com.blog.engagement.messaging.RegisterCommentCommand;
import com.blog.engagement.web.dto.CommentRequest;
import com.blog.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
//
// na quarta entrega a escrita e a leitura se separaram. ler a conversa continua sendo
// uma chamada http -- quem abre a pagina precisa da lista agora. escrever virou um
// comando na fila: o leitor nao precisa esperar o comentario ser gravado para seguir, so
// precisa saber que ele foi aceito. a ordem das operacoes, porem, e a mesma de antes: o
// post e validado aqui, antes de qualquer coisa sair da maquina.
@Service
public class CommentService {

    private final EngagementClient engagementClient;
    private final CommentCommandSender commandSender;
    private final PostCatalog postCatalog;

    public CommentService(EngagementClient engagementClient, CommentCommandSender commandSender,
                          PostCatalog postCatalog) {
        this.engagementClient = engagementClient;
        this.commandSender = commandSender;
        this.postCatalog = postCatalog;
    }

    // aceita o comentario e o entrega a fila do engajamento. devolve o comando enviado,
    // que e o que a api responde com 202: "recebido, vai aparecer em instantes".
    //
    // o ganho e de resiliencia: o comentario deixa de depender de o engajamento estar de
    // pe neste instante. com ele fora do ar, o comando espera na fila e e processado
    // quando ele voltar -- o leitor nao perde o recado e nao precisa tentar de novo. o
    // que passou a ser necessario no momento do envio e o broker, e nao mais o servico.
    public RegisterCommentCommand addToPost(Long postId, CommentRequest request) {
        requirePost(postId);
        RegisterCommentCommand command = new RegisterCommentCommand(
                UUID.randomUUID().toString(), postId, request.authorName(), request.content(), Instant.now());
        commandSender.send(command);
        return command;
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
