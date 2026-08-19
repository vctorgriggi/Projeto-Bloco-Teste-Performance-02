package com.blog.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

// o config server e o primeiro processo a subir, e os servicos de negocio nao terminam
// de inicializar sem ele. um erro aqui -- um caminho de busca errado, um arquivo com
// nome que nao casa com o nome da aplicacao -- nao aparece como falha deste servico, e
// sim como um servico de negocio subindo sem as propriedades que esperava.
//
// por isso o teste nao se contenta em subir o contexto: ele pede a configuracao pelos
// mesmos nomes que os clientes usam e confere que os valores chegam.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerApplicationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<String> configuracaoDe(String aplicacao) {
        return restTemplate.getForEntity(
                "http://localhost:" + port + "/{aplicacao}/default", String.class, aplicacao);
    }

    @Test
    void serveAConfiguracaoDoMonolito() {
        ResponseEntity<String> resposta = configuracaoDe("blog-api");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        // a politica de resiliencia e a origem do cors sao ajustes de ambiente que o
        // monolito deixou de guardar localmente
        assertThat(resposta.getBody())
                .contains("app.cors.allowed-origin")
                .contains("wait-duration-in-open-state")
                .contains("timeout-duration");
    }

    @Test
    void serveAConfiguracaoDoMicrosservico() {
        ResponseEntity<String> resposta = configuracaoDe("engagement-service");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).contains("lease-renewal-interval-in-seconds");
    }

    // o arquivo application.yml e o coringa: o que esta nele vale para qualquer cliente.
    // o endereco do eureka mora la justamente porque os dois precisam dele.
    @Test
    void oEnderecoDoRegistroChegaAOsDoisServicos() {
        assertThat(configuracaoDe("blog-api").getBody())
                .contains("eureka.client.service-url.defaultZone");
        assertThat(configuracaoDe("engagement-service").getBody())
                .contains("eureka.client.service-url.defaultZone");
    }

    // um servico desconhecido nao e erro: recebe apenas o coringa. e o comportamento
    // esperado do config server, e conferir isso evita a interpretacao errada de que
    // "respondeu 200" significa "achou o arquivo daquele servico".
    @Test
    void servicoSemArquivoProprio_recebeSoOCoringa() {
        ResponseEntity<String> resposta = configuracaoDe("servico-que-nao-existe");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .contains("eureka.client.service-url.defaultZone")
                .doesNotContain("app.cors.allowed-origin");
    }
}
