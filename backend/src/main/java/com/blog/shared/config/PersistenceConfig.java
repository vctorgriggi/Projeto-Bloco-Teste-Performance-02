package com.blog.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// troca a fabrica padrao dos repositorios pela do spring data envers. e ela que
// permite um repositorio estender RevisionRepository e ganhar os metodos de
// consulta de historico (findRevisions, findLastChangeRevision) sem deixar de
// funcionar como um JpaRepository comum. o basePackages cobre os dois contextos.
@Configuration
@EnableJpaRepositories(
        basePackages = "com.blog",
        repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class
)
public class PersistenceConfig {
}
