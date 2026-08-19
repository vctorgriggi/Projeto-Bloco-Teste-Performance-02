package com.blog.engagement.shared;

import java.time.Instant;
import java.util.Map;

// o microsservico responde erro no mesmo formato do monolito, de proposito: quem
// consome (o monolito, hoje; um gateway, amanha) le sempre o mesmo envelope, e o
// front nao precisa saber de qual processo o erro veio.
//
// o formato e repetido em vez de compartilhado por uma biblioteca comum. isso e
// duplicacao consciente: uma lib compartilhada acoplaria os dois servicos em
// tempo de build e faria um deploy do formato de erro virar deploy dos dois.
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError ofValidation(int status, String error, String message, String path,
                                        Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }
}
