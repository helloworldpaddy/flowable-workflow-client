package com.workflow.cmsflowable.service;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("caseProcessExecutionListener")
public class CaseProcessExecutionListener implements ExecutionListener {

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        String activityId = execution.getCurrentActivityId();
        String processInstanceId = execution.getProcessInstanceId();
        
        log.debug("Execution listener triggered: event={}, activity={}, process={}", 
                 eventName, activityId, processInstanceId);
        
        try {
            // Handle different execution events
            switch (eventName) {
                case "start":
                    handleProcessStart(execution);
                    break;
                case "end":
                    handleActivityEnd(execution, activityId);
                    break;
                case "take":
                    handleSequenceFlowTaken(execution);
                    break;
                default:
                    log.debug("Unhandled execution event: {}", eventName);
            }
            
        } catch (Exception e) {
            log.error("Error in CaseProcessExecutionListener for event: {} in activity: {}", 
                     eventName, activityId, e);
        }
    }

    private void handleProcessStart(DelegateExecution execution) {
        log.info("Process started: {}", execution.getProcessInstanceId());
        
        // Initialize case state variables for Cerbos
        execution.setVariable("status", "COMPLAINT_RECEIVED");
        execution.setVariable("currentTaskGroup", "INTAKE_ANALYST_GROUP");
        execution.setVariable("ipApproved", false);
        execution.setVariable("roiApproved", false);
        execution.setVariable("allegationsSubstantiated", false);
        execution.setVariable("isArogCase", false);
        execution.setVariable("caseApprovedForClosure", false);
        
        log.debug("Initialized case state variables for process: {}", execution.getProcessInstanceId());
    }

    private void handleActivityEnd(DelegateExecution execution, String activityId) {
        if (activityId == null) return;
        
        log.debug("Activity ended: {} in process: {}", activityId, execution.getProcessInstanceId());
        
        // Update state based on completed activities
        switch (activityId) {
            case "ExclusiveGateway_IP_Approved":
                Boolean ipApproved = (Boolean) execution.getVariable("ipApproved");
                log.debug("IP approval decision: {}", ipApproved);
                break;
                
            case "ExclusiveGateway_ROI_Approved":
                Boolean roiApproved = (Boolean) execution.getVariable("roiApproved");
                log.debug("ROI approval decision: {}", roiApproved);
                break;
                
            case "ExclusiveGateway_AllegationsSubstantiated":
                Boolean substantiated = (Boolean) execution.getVariable("allegationsSubstantiated");
                log.debug("Allegations substantiation decision: {}", substantiated);
                break;
                
            case "ExclusiveGateway_AROG_CriteriaMet":
                Boolean isArogCase = (Boolean) execution.getVariable("isArogCase");
                log.debug("AROG criteria decision: {}", isArogCase);
                break;
                
            case "ExclusiveGateway_CaseApprovedForClosure":
                Boolean approvedForClosure = (Boolean) execution.getVariable("caseApprovedForClosure");
                log.debug("Case closure approval decision: {}", approvedForClosure);
                break;
                
            case "EndEvent_CaseClosed":
                execution.setVariable("status", "CLOSED");
                execution.setVariable("caseApprovedForClosure", true);
                log.info("Case closed: {}", execution.getProcessInstanceId());
                break;
        }
    }

    private void handleSequenceFlowTaken(DelegateExecution execution) {
        String activityId = execution.getCurrentActivityId();
        log.debug("Sequence flow taken to: {} in process: {}", activityId, execution.getProcessInstanceId());
        
        // Update current activity context for Cerbos
        execution.setVariable("currentActivity", activityId);
    }
}