package com.blog.engagement.shared;

// lancada quando um recurso pedido pelo cliente nao existe. o handler global do
// microsservico traduz para http 404, e o monolito reconhece esse 404 na resposta
// e o repassa ao front sem transformar em erro de infraestrutura.
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
