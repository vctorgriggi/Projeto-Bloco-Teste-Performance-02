package com.blog.shared.exception;

// lancada quando uma regra de negocio e violada (ex: email duplicado, publicar
// um post ja publicado). o handler global traduz para http 409.
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
