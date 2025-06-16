package com.workflow.cmsflowable.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.workflow.cmsflowable.repository"
)
public class DatabaseConfig {
    // Single datasource configuration - Spring Boot will auto-configure based on application.yml
    // Both JPA entities and Flowable will use the primary datasource
}