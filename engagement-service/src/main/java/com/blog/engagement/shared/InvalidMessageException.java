package com.blog.engagement.shared;

// uma mensagem chegou pela fila sem o que precisa para ser processada (um comando de
// comentario sem texto, um evento sem o id do post). e o equivalente assincrono do 400:
// o problema esta na mensagem, nao no servico, e tentar de novo daria o mesmo resultado.
//
// por isso ela e marcada como nao retentavel na configuracao de mensageria, e a mensagem
// vai direto para a dead letter da fila, onde pode ser inspecionada.
public class InvalidMessageException extends RuntimeException {

    public InvalidMessageException(String message) {
        super(message);
    }
}
