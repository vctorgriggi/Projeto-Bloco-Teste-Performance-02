package com.blog.engagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// ponto de entrada do microsservico de engajamento. e um processo independente,
// com o seu proprio banco e o seu proprio ciclo de deploy: sobe, se registra no
// eureka e passa a atender comentarios e reacoes.
@SpringBootApplication
public class EngagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngagementServiceApplication.class, args);
    }
}
