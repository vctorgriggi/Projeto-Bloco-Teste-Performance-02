package com.blog.shared.exception;

// lancada quando um servico chamado recusa a requisicao por validacao (400). o
// monolito nao repete as validacoes do outro servico -- quem e dono da regra e
// dele -- entao repassa a recusa como 400 em vez de escondê-la atras de um 500.
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
