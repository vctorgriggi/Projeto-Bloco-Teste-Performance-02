package com.blog.engagement.web.dto;

// resposta do ping de integracao. serve para o monolito (e para quem estiver
// depurando) confirmar que este processo esta de pe e conseguir dizer, na
// interface, se a conversa e as reacoes estao disponiveis agora.
public record ServiceInfoResponse(
        String service,
        String status
) {
}
