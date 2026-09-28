package com.blog.engagement.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// o executor das publicacoes de melhor esforco (EngagementEventPublisher).
//
// proprio, e nao o executor padrao do spring, por causa do que acontece com o broker fora
// do ar: cada publicacao fica presa no timeout de conexao, e a fila de publicacoes
// pendentes cresce. aqui ela tem limite. cheia, a mais nova e descartada com um aviso no
// log -- e nada se perde de verdade, porque o que estas mensagens carregam e um
// contador que a proxima mudanca corrige, e a republicacao do estado refaz tudo.
@Configuration
@EnableAsync
public class PublicacaoAssincronaConfig {

    public static final String EXECUTOR = "publicacaoDeEventos";

    private static final Logger log = LoggerFactory.getLogger(PublicacaoAssincronaConfig.class);

    @Bean(EXECUTOR)
    Executor publicacaoDeEventos() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("publica-evento-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(1000);
        executor.setRejectedExecutionHandler((tarefa, pool) ->
                log.warn("fila de publicacao cheia ({} pendentes): evento de engajamento descartado",
                        pool.getQueue().size()));
        // no encerramento, termina o que estava na fila antes de sair
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
