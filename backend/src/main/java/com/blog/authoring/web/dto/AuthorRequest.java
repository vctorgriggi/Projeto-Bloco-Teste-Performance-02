package com.blog.authoring.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// dados que o cliente envia para criar ou atualizar um autor. o bean validation
// rejeita entradas invalidas antes de chegar no service.
public record AuthorRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @Size(max = 500) String bio
) {
}
