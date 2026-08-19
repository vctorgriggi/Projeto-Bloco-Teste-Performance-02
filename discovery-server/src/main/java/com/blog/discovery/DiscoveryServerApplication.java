package com.blog.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

// servidor de descoberta do sistema. e o unico processo que os outros dois
// precisam conhecer por endereco fixo: o monolito e o microsservico se registram
// aqui no startup e passam a se encontrar pelo nome logico do servico, sem
// url chumbada em nenhum dos lados.
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
