package com.blog.shared.exception;

// lancada quando o microsservico de engajamento nao responde: processo fora do ar,
// timeout, ou o circuito aberto cortando a chamada antes mesmo de tentar.
//
// e uma excecao que so passou a existir na terceira entrega, e a razao e a propria
// mudanca de arquitetura: enquanto comentario era uma tabela local, uma falha ali
// era falha do banco; agora e uma dependencia de rede que pode estar
// indisponivel enquanto o resto do sistema continua funcionando. o handler global
// traduz para 503, que e o status que diz "tente de novo mais tarde".
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
