package com.blog.engagement.web.dto;

import com.blog.engagement.domain.ReactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// dados para registrar uma reacao. o tipo chega como enum, entao um valor fora do
// conjunto e recusado na desserializacao, antes de chegar no service.
public record ReactionRequest(
        @NotBlank @Size(max = 60) String readerName,
        @NotNull ReactionType type
) {
}
