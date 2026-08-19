package com.blog.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

// servidor de configuracao central. e o primeiro processo a subir: os servicos de
// negocio pedem as propriedades deles aqui antes de terminar de inicializar.
//
// o que ele resolve e o problema de configuracao que nasce junto com a distribuicao:
// com um processo so, mudar o endereco do banco ou um limite de timeout era editar um
// arquivo e reiniciar. com varios, a mesma propriedade aparece em varios lugares e sai
// de sincronia. centralizar significa que a propriedade tem um dono.
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
