package com.blog.engagement.shared;

// regra de negocio do engajamento violada (ex: o leitor tentou repetir a mesma
// reacao). o handler global traduz para http 409.
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
