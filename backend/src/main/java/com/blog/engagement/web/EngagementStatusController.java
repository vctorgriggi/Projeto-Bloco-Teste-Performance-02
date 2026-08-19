package com.blog.engagement.web;

import com.blog.engagement.service.EngagementStatusService;
import com.blog.engagement.web.dto.EngagementStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// endpoint de diagnostico da integracao. responde sempre 200, inclusive quando o
// microsservico esta fora do ar -- a informacao "ele caiu" vai no corpo, porque a
// pergunta "esta disponivel?" foi respondida com sucesso. usar 503 aqui faria a
// interface tratar o proprio diagnostico como falha.
@RestController
@RequestMapping("/api/engagement")
public class EngagementStatusController {

    private final EngagementStatusService engagementStatusService;

    public EngagementStatusController(EngagementStatusService engagementStatusService) {
        this.engagementStatusService = engagementStatusService;
    }

    @GetMapping("/status")
    public EngagementStatusResponse status() {
        return engagementStatusService.current();
    }
}
