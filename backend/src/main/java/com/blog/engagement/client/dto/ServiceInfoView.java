package com.blog.engagement.client.dto;

// resposta do ping do microsservico. quando ele nao responde, e o fallback que
// monta este mesmo objeto com status DOWN -- por isso esta e a unica chamada do
// cliente que degrada em vez de falhar.
public record ServiceInfoView(
        String service,
        String status
) {
}
