package com.blog.engagement.web;

import com.blog.engagement.projection.EngagementCountersService;
import com.blog.engagement.web.dto.EngagementCounterResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// os contadores de engajamento de todos os posts, numa chamada so.
//
// a resposta sai do banco deste servico, da copia mantida pelos eventos do engajamento,
// e nao de uma chamada ao microsservico. e o que permite a estante mostrar "3 recados"
// em cada cartao sem uma ida e volta de rede por post, e continuar mostrando com o
// engajamento fora do ar. nao ha 503 possivel aqui: o pior caso e um numero defasado.
@RestController
@RequestMapping("/api/engagement")
public class EngagementCountersController {

    private final EngagementCountersService countersService;

    public EngagementCountersController(EngagementCountersService countersService) {
        this.countersService = countersService;
    }

    @GetMapping("/counters")
    public List<EngagementCounterResponse> counters() {
        return countersService.all().stream().map(EngagementCounterResponse::from).toList();
    }
}
