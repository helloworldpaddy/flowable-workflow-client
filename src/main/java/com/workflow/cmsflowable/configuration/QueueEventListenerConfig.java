package com.workflow.cmsflowable.configuration;

import com.workflow.cmsflowable.service.QueueTaskEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class QueueEventListenerConfig {
    
    private final ProcessEngine processEngine;
    private final QueueTaskEventListener queueTaskEventListener;
    
    @PostConstruct
    public void registerEventListeners() {
        processEngine.getRuntimeService()
                .addEventListener(queueTaskEventListener);
        
        // TaskService doesn't have addEventListener method, only RuntimeService does
        log.info("Queue event listener registered successfully");
    }
}