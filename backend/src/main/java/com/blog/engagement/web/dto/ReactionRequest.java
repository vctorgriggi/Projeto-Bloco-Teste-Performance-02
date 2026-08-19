package com.blog.engagement.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// dados para registrar uma reacao.
//
// o tipo e validado apenas como "veio preenchido". checar se CORACAO existe seria
// duplicar aqui uma regra que pertence ao servico de engajamento -- e duplicada,
// ela sairia de sincronia no dia em que um tipo novo fosse criado la. o valor segue
// como texto e quem recusa, se for o caso, e o dono da regra, com um 400 que este
// servico repassa.
public record ReactionRequest(
        @NotBlank @Size(max = 60) String readerName,
        @NotBlank String type
) {
}
