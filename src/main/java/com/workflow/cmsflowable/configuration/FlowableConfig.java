package com.workflow.cmsflowable.configuration;

import org.springframework.context.annotation.Configuration;

@Configuration
public class FlowableConfig {
    // Deployment configuration enabled
    // Both auto-deployment and manual deployment endpoints are available
    // Manual deployment via /api/v1/deploy endpoints provides better error handling
}