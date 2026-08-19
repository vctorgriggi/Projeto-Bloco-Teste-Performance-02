package com.blog.engagement.config;

import com.blog.engagement.domain.Comment;
import com.blog.engagement.domain.Reaction;
import com.blog.engagement.domain.ReactionType;
import com.blog.engagement.repository.CommentRepository;
import com.blog.engagement.repository.ReactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// popula o banco do microsservico com alguma conversa e algumas reacoes, para a
// interface ter conteudo na primeira execucao.
//
// os postId aqui sao os mesmos que o seeder do monolito gera (1 e 2), e isso e um
// acerto combinado entre os dois bancos em memoria, que sobem vazios e geram ids
// em sequencia. e aceitavel para dado de exemplo, mas mostra bem o custo de nao
// ter integridade referencial entre servicos: quem garante a coerencia deixa de
// ser o banco e passa a ser o fluxo da aplicacao.
@Configuration
@Profile("!test")
public class DataSeeder {

    @Bean
    CommandLineRunner seed(CommentRepository comments, ReactionRepository reactions) {
        return args -> {
            if (comments.count() > 0) {
                return;
            }

            comments.save(new Comment(1L, "Carla", "Otimo resumo, ajudou bastante!"));
            comments.save(new Comment(1L, "Diego", "Esperando a proxima parte."));
            comments.save(new Comment(2L, "Ana", "Separar camadas faz toda a diferenca."));

            reactions.save(new Reaction(1L, "Carla", ReactionType.CORACAO));
            reactions.save(new Reaction(1L, "Diego", ReactionType.CORACAO));
            reactions.save(new Reaction(1L, "Carla", ReactionType.IDEIA));
            reactions.save(new Reaction(2L, "Ana", ReactionType.CAFE));
        };
    }
}
