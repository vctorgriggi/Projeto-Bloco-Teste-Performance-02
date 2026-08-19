package com.blog.engagement.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.ErrorDecoder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// liga a integracao com o microsservico: a varredura dos @FeignClient e as duas
// pecas que definem como a fronteira de rede se comporta em caso de erro.
@Configuration
@EnableFeignClients(basePackages = "com.blog.engagement.client")
public class EngagementIntegrationConfig {

    @Bean
    ErrorDecoder engagementErrorDecoder(ObjectMapper objectMapper) {
        return new EngagementErrorDecoder(objectMapper);
    }

    @Bean
    EngagementFallbackFactory engagementFallbackFactory() {
        return new EngagementFallbackFactory();
    }
}
