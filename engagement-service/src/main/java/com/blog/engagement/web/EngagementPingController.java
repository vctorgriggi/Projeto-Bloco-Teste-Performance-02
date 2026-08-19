package com.blog.engagement.web;

import com.blog.engagement.web.dto.ServiceInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ponto de contato leve para o monolito perguntar "voce esta ai?". existe separado
// do /actuator/health porque e um contrato da integracao, e nao de monitoramento:
// e uma resposta minima, sem detalhe de banco nem de disco, que o monolito usa
// para dizer na interface se o engajamento esta disponivel.
@RestController
@RequestMapping("/api/engagement")
public class EngagementPingController {

    private final String applicationName;

    public EngagementPingController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/ping")
    public ServiceInfoResponse ping() {
        return new ServiceInfoResponse(applicationName, "UP");
    }
}
