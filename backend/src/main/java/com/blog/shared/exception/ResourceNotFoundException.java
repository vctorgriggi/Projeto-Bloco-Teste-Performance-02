package com.blog.shared.exception;

// lancada quando um recurso pedido pelo cliente nao existe. o handler global
// traduz para http 404.
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
