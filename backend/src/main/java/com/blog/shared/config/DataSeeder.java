package com.blog.shared.config;

import com.blog.authoring.domain.Author;
import com.blog.authoring.domain.Post;
import com.blog.authoring.repository.AuthorRepository;
import com.blog.authoring.repository.PostRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// popula o banco em memoria com alguns dados de exemplo no startup, para a api e o
// front terem conteudo logo na primeira execucao. so roda se estiver vazio. fica
// desligado no perfil de teste para os testes partirem de um banco limpo.
//
// os comentarios sairam daqui na terceira entrega: eles nao pertencem mais a este
// banco. o exemplo de conversa e de reacoes e semeado pelo seeder do proprio
// engagement-service, que amarra tudo pelos mesmos ids de post gerados aqui (1, 2 e 3).
@Configuration
@Profile("!test")
public class DataSeeder {

    @Bean
    CommandLineRunner seed(AuthorRepository authors, PostRepository posts) {
        return args -> {
            if (authors.count() > 0) {
                return;
            }

            Author ana = authors.save(new Author("Ana Souza", "ana@blog.dev",
                    "Escreve sobre arquitetura de software e boas praticas."));
            Author bruno = authors.save(new Author("Bruno Lima", "bruno@blog.dev",
                    "Apaixonado por front-end e experiencia do usuario."));

            Post primeiro = new Post("Comecando com Spring Boot",
                    "Spring Boot reduz a configuracao inicial e deixa voce focar no dominio.", ana.getId());
            primeiro.publish();
            posts.save(primeiro);

            posts.save(new Post("Organizando o codigo em camadas",
                    "Controller, service e repository: cada um com uma responsabilidade clara.", ana.getId()));

            posts.save(new Post("React e Vite na pratica",
                    "Um setup rapido de front-end para consumir a sua api.", bruno.getId()));
        };
    }
}
