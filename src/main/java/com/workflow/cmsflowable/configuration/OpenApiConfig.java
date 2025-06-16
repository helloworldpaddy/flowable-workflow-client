package com.workflow.cmsflowable.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Value("${server.servlet.context-path:/api}")
    private String contextPath;
    
    @Value("${cms.deployment.environment:development}")
    private String environment;
    
    @Value("${cms.deployment.base-url:http://localhost}")
    private String baseUrl;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(getServerList())
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }
    
    private Info apiInfo() {
        return new Info()
            .title("CMS Flowable Workflow Management API")
            .description("Comprehensive API for managing Case Management System with Flowable workflow engine. " +
                        "This API provides endpoints for creating cases with allegations, managing workflow tasks, " +
                        "and handling case transitions through HR, Legal, and CSIS departments.")
            .version("v1.0.0")
            .contact(new Contact()
                .name("CMS Development Team")
                .email("cms-dev@company.com")
                .url("https://cms-flowable.com"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT"));
    }
    
    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .bearerFormat("JWT")
            .scheme("bearer")
            .description("JWT token for API authentication. Format: Bearer {token}");
    }
    
    private List<Server> getServerList() {
        List<Server> servers = new java.util.ArrayList<>();
        
        // Always include localhost for development
        servers.add(new Server()
            .url("http://localhost:" + serverPort + contextPath)
            .description("Local Development Server"));
            
        // Add environment-specific servers
        switch (environment.toLowerCase()) {
            case "development":
                servers.add(new Server()
                    .url(baseUrl + ":" + serverPort + contextPath)
                    .description("Development Environment"));
                break;
            case "staging":
                servers.add(new Server()
                    .url("https://staging-api.cms-flowable.com" + contextPath)
                    .description("Staging Environment"));
                break;
            case "production":
                servers.add(new Server()
                    .url("https://api.cms-flowable.com" + contextPath)
                    .description("Production Environment"));
                break;
            default:
                servers.add(new Server()
                    .url(baseUrl + contextPath)
                    .description("Custom Environment"));
        }
        
        return servers;
    }
}