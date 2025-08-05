package com.workflow.cmsflowable.service;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("caseStatusUpdateListener")
public class CaseStatusUpdateListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        String processInstanceId = delegateTask.getProcessInstanceId();
        
        log.debug("Task listener triggered for task: {} in process: {}", taskDefinitionKey, processInstanceId);
        
        try {
            // Update case status based on current task
            String newStatus = mapTaskToStatus(taskDefinitionKey);
            if (newStatus != null) {
                delegateTask.setVariable("status", newStatus);
                log.debug("Updated case status to: {} for task: {}", newStatus, taskDefinitionKey);
            }
            
            // Update currentTaskGroup for Cerbos authorization
            String currentTaskGroup = getCurrentTaskGroup(delegateTask);
            if (currentTaskGroup != null) {
                delegateTask.setVariable("currentTaskGroup", currentTaskGroup);
                log.debug("Updated currentTaskGroup to: {} for task: {}", currentTaskGroup, taskDefinitionKey);
            }
            
            // Update assignedUsers if task has assignee
            if (delegateTask.getAssignee() != null) {
                delegateTask.setVariable("assignedUsers", List.of(delegateTask.getAssignee()));
                log.debug("Updated assignedUsers to: {} for task: {}", delegateTask.getAssignee(), taskDefinitionKey);
            }
            
            // Set task-specific variables for Cerbos
            setTaskSpecificVariables(delegateTask, taskDefinitionKey);
            
        } catch (Exception e) {
            log.error("Error in CaseStatusUpdateListener for task: {}", taskDefinitionKey, e);
        }
    }

    private String mapTaskToStatus(String taskDefinitionKey) {
        switch (taskDefinitionKey) {
            case "Task_EO_Intake":
                return "COMPLAINT_RECEIVED";
            case "Task_IU_IntakeReview":
                return "DEPARTMENT_ROUTED";
            case "Task_DraftInvestigationPlan":
                return "INVESTIGATION_PLANNING";
            case "Task_ConductInvestigation":
                return "ACTIVE_INVESTIGATION";
            case "Task_DraftROI":
                return "REPORT_DRAFTING";
            case "Task_AssessFindings":
                return "FINALIZATION_DISCIPLINE";
            case "Task_HR_LegalAdjudication":
                return "ADJUDICATION";
            case "Task_CoordinateDiscipline":
                return "DISCIPLINARY_ACTION";
            case "Task_AROG_ReviewMeeting":
                return "AROG_REVIEW";
            case "Task_EO_FinalReviewAndClosure":
                return "FINAL_REVIEW";
            case "Task_AssignToDepartment":
                return "DEPARTMENT_ASSIGNMENT";
            default:
                log.warn("Unknown task definition key: {}", taskDefinitionKey);
                return null;
        }
    }

    private String getCurrentTaskGroup(DelegateTask delegateTask) {
        // Extract candidate groups from task
        List<String> candidateGroups = delegateTask.getCandidates()
            .stream()
            .filter(identityLink -> "candidate".equals(identityLink.getType()) && identityLink.getGroupId() != null)
            .map(identityLink -> identityLink.getGroupId())
            .collect(Collectors.toList());
            
        // Return the first candidate group (assuming single group per task)
        return candidateGroups.isEmpty() ? null : candidateGroups.get(0);
    }

    private void setTaskSpecificVariables(DelegateTask delegateTask, String taskDefinitionKey) {
        // Set default approval states
        switch (taskDefinitionKey) {
            case "Task_DraftInvestigationPlan":
                // Initialize IP approval state if not set
                if (delegateTask.getVariable("ipApproved") == null) {
                    delegateTask.setVariable("ipApproved", false);
                }
                break;
            case "Task_DraftROI":
                // Initialize ROI approval state if not set
                if (delegateTask.getVariable("roiApproved") == null) {
                    delegateTask.setVariable("roiApproved", false);
                }
                break;
            case "Task_AssessFindings":
                // Initialize substantiation state if not set
                if (delegateTask.getVariable("allegationsSubstantiated") == null) {
                    delegateTask.setVariable("allegationsSubstantiated", false);
                }
                break;
            case "Task_AROG_ReviewMeeting":
                // Initialize AROG case flag if not set
                if (delegateTask.getVariable("isArogCase") == null) {
                    delegateTask.setVariable("isArogCase", true); // If we reach this task, it's an AROG case
                }
                break;
            case "Task_EO_FinalReviewAndClosure":
                // Initialize closure approval state if not set
                if (delegateTask.getVariable("caseApprovedForClosure") == null) {
                    delegateTask.setVariable("caseApprovedForClosure", false);
                }
                break;
        }
    }
}