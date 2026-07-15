package com.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// ponto de entrada do monolito. a anotacao @SpringBootApplication liga a
// autoconfiguracao e o component scan a partir deste pacote.
@SpringBootApplication
public class BlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
}
