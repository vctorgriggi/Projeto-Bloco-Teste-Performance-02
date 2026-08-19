package com.blog.engagement.client;

import com.blog.shared.exception.BusinessRuleException;
import com.blog.shared.exception.InvalidRequestException;
import com.blog.shared.exception.ResourceNotFoundException;
import com.blog.shared.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

// traduz a resposta de erro do microsservico em uma excecao do dominio do monolito.
//
// sem isso, qualquer resposta fora do 2xx chegaria como FeignException e acabaria
// em 500: um comentario inexistente (404 la) viraria "erro interno" aqui. o decoder
// e o que preserva o significado do erro atravessando a fronteira de rede.
//
// os dois lados respondem erro no mesmo envelope (o ApiError), e e por isso que da
// para aproveitar a mensagem original em vez de inventar um texto generico: o
// leitor ve "o leitor Carla ja reagiu com CORACAO neste post", que foi escrito pelo
// servico dono da regra.
public class EngagementErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    public EngagementErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        String mensagem = mensagemDoCorpo(response);

        // so os status que carregam significado de negocio sao repassados. o resto
        // (5xx, 405, 415, qualquer coisa inesperada) e falha da integracao, e vira
        // indisponibilidade em vez de confundir o cliente com um erro de dominio.
        return switch (response.status()) {
            case 400 -> new InvalidRequestException(
                    StringUtils.hasText(mensagem) ? mensagem : "Requisicao recusada pelo servico de engajamento");
            case 404 -> new ResourceNotFoundException(
                    StringUtils.hasText(mensagem) ? mensagem : "Recurso de engajamento nao encontrado");
            case 409 -> new BusinessRuleException(
                    StringUtils.hasText(mensagem) ? mensagem : "Conflito com o estado atual do engajamento");
            default -> new ServiceUnavailableException(
                    "O servico de engajamento respondeu " + response.status()
                            + " para " + methodKey + ": " + mensagem, null);
        };
    }

    // le o campo message do envelope de erro. se o corpo nao vier, ou nao for o
    // envelope esperado, seguimos sem mensagem em vez de estourar aqui dentro.
    private String mensagemDoCorpo(Response response) {
        if (response.body() == null) {
            return "";
        }
        try (InputStream corpo = response.body().asInputStream()) {
            JsonNode json = objectMapper.readTree(corpo);
            JsonNode message = json.get("message");
            return message == null ? "" : message.asText("");
        } catch (IOException | RuntimeException e) {
            return "";
        }
    }
}
