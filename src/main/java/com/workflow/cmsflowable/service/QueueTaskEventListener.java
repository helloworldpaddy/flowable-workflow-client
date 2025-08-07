package com.workflow.cmsflowable.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueTaskEventListener implements FlowableEventListener {
    
    private final QueueTaskService queueTaskService;
    
    @Override
    public void onEvent(FlowableEvent event) {
        if (event.getType() == FlowableEngineEventType.PROCESS_STARTED) {
            handleProcessStarted(event);
        }
        // Task events will be handled through the existing workflow service integration
        // to avoid compatibility issues with different Flowable versions
    }
    
    private void handleProcessStarted(FlowableEvent event) {
        try {
            log.info("Process started event received");
            
            // For now, we'll rely on the existing CaseWorkflowService integration
            // to populate queue tasks rather than using event listeners
            log.debug("Queue task population handled by CaseWorkflowService");
            
        } catch (Exception e) {
            log.error("Failed to handle process started event: {}", e.getMessage(), e);
            // Don't throw exception to avoid disrupting the workflow
        }
    }
    
    @Override
    public boolean isFailOnException() {
        return false; // Don't fail the workflow if queue operations fail
    }
    
    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }
    
    @Override
    public String getOnTransaction() {
        return null;
    }
}