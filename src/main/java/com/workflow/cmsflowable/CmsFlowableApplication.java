package com.workflow.cmsflowable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.workflow.cmsflowable")
public class CmsFlowableApplication {

    public static void main(String[] args) {
        SpringApplication.run(CmsFlowableApplication.class, args);
    }
}