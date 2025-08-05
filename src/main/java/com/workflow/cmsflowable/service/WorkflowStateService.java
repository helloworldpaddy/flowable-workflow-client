package com.workflow.cmsflowable.service;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WorkflowStateService {
    
    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private Environment environment;
    
    @Value("${cms.deployment.environment:development}")
    private String deploymentEnvironment;
    
    /**
     * Get current workflow variables and state for a case
     */
    public Map<String, Object> getCurrentWorkflowVariables(String caseNumber) {
        try {
            List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("caseId", caseNumber)
                .active()
                .list();
                
            if (instances.isEmpty()) {
                log.debug("No active process instance found for case: {}", caseNumber);
                return getDefaultWorkflowState();
            }
            
            ProcessInstance instance = instances.get(0);
            Map<String, Object> variables = new HashMap<>(runtimeService.getVariables(instance.getId()));
            
            // Add current task group information
            List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .active()
                .list();
                
            if (!activeTasks.isEmpty()) {
                Task currentTask = activeTasks.get(0);
                List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(currentTask.getId());
                String currentTaskGroup = identityLinks.stream()
                    .filter(link -> "candidate".equals(link.getType()) && link.getGroupId() != null)
                    .map(IdentityLink::getGroupId)
                    .findFirst()
                    .orElse("");
                variables.put("currentTaskGroup", currentTaskGroup);
                variables.put("currentTaskName", currentTask.getName());
                variables.put("currentTaskId", currentTask.getId());
                
                log.debug("Current task for case {}: {} (Group: {})", caseNumber, currentTask.getName(), currentTaskGroup);
            } else {
                variables.put("currentTaskGroup", "");
                variables.put("currentTaskName", "");
                variables.put("currentTaskId", "");
            }
            
            // Ensure required workflow state variables exist
            variables.putIfAbsent("caseStatus", "COMPLAINT_RECEIVED");
            variables.putIfAbsent("ipApproved", false);
            variables.putIfAbsent("roiApproved", false);
            variables.putIfAbsent("allegationsSubstantiated", false);
            variables.putIfAbsent("isArogCase", false);
            variables.putIfAbsent("caseApprovedForClosure", false);
            
            log.debug("Retrieved workflow variables for case {}: {}", caseNumber, variables.keySet());
            return variables;
            
        } catch (Exception e) {
            log.error("Error retrieving workflow variables for case {}: {}", caseNumber, e.getMessage());
            return getDefaultWorkflowState();
        }
    }
    
    /**
     * Get the current workflow status for a case
     */
    public String getCurrentWorkflowStatus(String caseNumber) {
        Map<String, Object> variables = getCurrentWorkflowVariables(caseNumber);
        return variables.getOrDefault("caseStatus", "COMPLAINT_RECEIVED").toString();
    }
    
    /**
     * Get the current task group for a case
     */
    public String getCurrentTaskGroup(String caseNumber) {
        Map<String, Object> variables = getCurrentWorkflowVariables(caseNumber);
        return variables.getOrDefault("currentTaskGroup", "INTAKE_ANALYST_GROUP").toString();
    }
    
    /**
     * Check if a case is in a specific workflow state
     */
    public boolean isInWorkflowState(String caseNumber, String expectedStatus) {
        String currentStatus = getCurrentWorkflowStatus(caseNumber);
        return expectedStatus.equals(currentStatus);
    }
    
    /**
     * Check if a case has a specific task group active
     */
    public boolean hasActiveTaskGroup(String caseNumber, String expectedGroup) {
        String currentGroup = getCurrentTaskGroup(caseNumber);
        return expectedGroup.equals(currentGroup);
    }
    
    /**
     * Get default workflow state for new cases or error scenarios
     */
    private Map<String, Object> getDefaultWorkflowState() {
        Map<String, Object> defaultState = new HashMap<>();
        defaultState.put("caseStatus", "COMPLAINT_RECEIVED");
        defaultState.put("currentTaskGroup", "INTAKE_ANALYST_GROUP");
        defaultState.put("currentTaskName", "EO Intake - Initial Review");
        defaultState.put("currentTaskId", "");
        defaultState.put("ipApproved", false);
        defaultState.put("roiApproved", false);
        defaultState.put("allegationsSubstantiated", false);
        defaultState.put("isArogCase", false);
        defaultState.put("caseApprovedForClosure", false);
        return defaultState;
    }
    
    /**
     * Update workflow status (for testing purposes only)
     * This method is restricted to development and test environments
     */
    public void updateWorkflowStatus(String caseNumber, String newStatus) {
        // Environment restriction check
        if (!isTestingEnvironment()) {
            log.error("updateWorkflowStatus method is not allowed in production environment. Current environment: {}", 
                deploymentEnvironment);
            throw new IllegalStateException("This method is only available in development or test environments");
        }
        
        try {
            List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("caseId", caseNumber)
                .active()
                .list();
                
            if (!instances.isEmpty()) {
                ProcessInstance instance = instances.get(0);
                runtimeService.setVariable(instance.getId(), "caseStatus", newStatus);
                log.info("Updated workflow status for case {} to {} (TEST ENVIRONMENT)", caseNumber, newStatus);
            }
        } catch (Exception e) {
            log.error("Error updating workflow status for case {}: {}", caseNumber, e.getMessage());
        }
    }
    
    /**
     * Check if current environment allows testing operations
     */
    private boolean isTestingEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isTestProfile = false;
        
        for (String profile : activeProfiles) {
            if ("test".equals(profile) || "dev".equals(profile) || "development".equals(profile)) {
                isTestProfile = true;
                break;
            }
        }
        
        boolean isTestDeployment = "development".equals(deploymentEnvironment) || 
                                 "test".equals(deploymentEnvironment) ||
                                 "dev".equals(deploymentEnvironment);
        
        return isTestProfile || isTestDeployment;
    }
}