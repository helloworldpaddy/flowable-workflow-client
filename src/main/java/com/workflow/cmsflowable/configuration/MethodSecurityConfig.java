package com.workflow.cmsflowable.configuration;

import com.workflow.cmsflowable.security.CerbosPermissionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class MethodSecurityConfig {

    @Autowired
    private CerbosPermissionEvaluator cerbosPermissionEvaluator;

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        try {
            DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
            expressionHandler.setPermissionEvaluator(cerbosPermissionEvaluator);
            log.info("Method Security Expression Handler configured with Cerbos Permission Evaluator");
            return expressionHandler;
        } catch (Exception e) {
            log.error("Failed to configure Method Security Expression Handler: {}", e.getMessage());
            // Return a fallback expression handler without custom permission evaluator
            log.warn("Using fallback Method Security Expression Handler without Cerbos integration");
            return new DefaultMethodSecurityExpressionHandler();
        }
    }
}