package com.workflow.cmsflowable.service;

import com.workflow.cmsflowable.security.CerbosPermissionEvaluator;
import com.workflow.cmsflowable.security.UserPrincipal;
import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.builders.AttributeValue;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import dev.cerbos.sdk.CheckResult;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to integrate Cerbos permissions with Flowable BPMN workflow tasks
 */
@Service
@Slf4j
public class CerbosWorkflowIntegration {

    @Autowired(required = false)
    private CerbosBlockingClient cerbosClient;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private CerbosPermissionEvaluator permissionEvaluator;

    /**
     * Check if the current user can claim a workflow task based on Cerbos policies
     */
    public boolean canClaimTask(String taskId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            log.warn("Task {} not found", taskId);
            return false;
        }

        return canPerformTaskAction(task, "claim", authentication);
    }

    /**
     * Check if the current user can complete a workflow task
     */
    public boolean canCompleteTask(String taskId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            log.warn("Task {} not found", taskId);
            return false;
        }

        return canPerformTaskAction(task, "complete", authentication);
    }

    /**
     * Check if user can perform a specific action on a workflow task
     */
    public boolean canPerformTaskAction(Task task, String action, Authentication authentication) {
        try {
            if (cerbosClient == null) {
                log.debug("Cerbos client not available, using fallback logic for task {}", task.getId());
                return checkTaskPermissionFallback(task, action, authentication);
            }

            // Build principal from authentication
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            List<String> roles = userPrincipal.getAuthorities().stream()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());

            Principal principal = Principal.newInstance(authentication.getName())
                .withRoles(roles.toArray(new String[0]))
                .withAttribute("userId", AttributeValue.stringValue(String.valueOf(userPrincipal.getUserId())))
                .withAttribute("email", AttributeValue.stringValue(userPrincipal.getEmail()));

            // Build resource from task
            Resource resource = buildTaskResource(task);

            // Check permission
            CheckResult result = cerbosClient.check(principal, resource, action);
            boolean allowed = result.isAllowed(action);

            log.debug("Cerbos task permission check - User: {}, Task: {}, Action: {}, CandidateGroup: {}, Allowed: {}", 
                authentication.getName(), task.getId(), action, task.getCategory(), allowed);

            return allowed;

        } catch (Exception e) {
            log.error("Error checking Cerbos task permission for user {} on task {}: {}", 
                authentication.getName(), task.getId(), e.getMessage());
            return checkTaskPermissionFallback(task, action, authentication);
        }
    }

    /**
     * Build Cerbos resource from Flowable task
     */
    private Resource buildTaskResource(Task task) {
        Resource resource = Resource.newInstance("workflow_task", task.getId());
        
        // Add task attributes
        if (task.getCategory() != null) {
            resource = resource.withAttribute("candidateGroup", AttributeValue.stringValue(task.getCategory()));
        }
        
        if (task.getName() != null) {
            resource = resource.withAttribute("taskName", AttributeValue.stringValue(task.getName()));
        }
        
        if (task.getProcessInstanceId() != null) {
            resource = resource.withAttribute("processInstanceId", AttributeValue.stringValue(task.getProcessInstanceId()));
        }
        
        if (task.getAssignee() != null) {
            resource = resource.withAttribute("assignee", AttributeValue.stringValue(task.getAssignee()));
        }

        // Add process variables as resource attributes
        Map<String, Object> processVariables = taskService.getVariables(task.getId());
        if (processVariables != null) {
            // Add relevant case information
            if (processVariables.containsKey("caseId")) {
                resource = resource.withAttribute("caseId", AttributeValue.stringValue(processVariables.get("caseId").toString()));
            }
            if (processVariables.containsKey("classification")) {
                resource = resource.withAttribute("classification", AttributeValue.stringValue(processVariables.get("classification").toString()));
            }
            if (processVariables.containsKey("priority")) {
                resource = resource.withAttribute("priority", AttributeValue.stringValue(processVariables.get("priority").toString()));
            }
        }

        return resource;
    }

    /**
     * Fallback permission check when Cerbos is not available
     */
    private boolean checkTaskPermissionFallback(Task task, String action, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream()
            .map(auth -> auth.getAuthority().replace("ROLE_", ""))
            .collect(Collectors.toList());

        // Admin can do everything
        if (roles.contains("ADMIN")) {
            return true;
        }

        // Check based on candidate group and user roles
        String candidateGroup = task.getCategory(); // In Flowable, category often holds candidate group
        
        if (candidateGroup != null) {
            switch (candidateGroup) {
                case "HR_GROUP":
                    return roles.contains("HR_SPECIALIST");
                case "LEGAL_GROUP":
                    return roles.contains("LEGAL_COUNSEL");
                case "CSIS_GROUP":
                    return roles.contains("SECURITY_ANALYST");
                case "INTAKE_ANALYST_GROUP":
                    return roles.contains("INTAKE_ANALYST");
                case "INVESTIGATOR_GROUP":
                    return roles.contains("INVESTIGATOR");
                case "DIRECTOR_GROUP":
                    return roles.contains("DIRECTOR");
                default:
                    log.debug("Unknown candidate group: {}", candidateGroup);
                    return false;
            }
        }

        // If no candidate group, check if user is assignee
        if (task.getAssignee() != null && task.getAssignee().equals(authentication.getName())) {
            return true;
        }

        return false;
    }

    /**
     * Filter tasks based on Cerbos permissions
     */
    public List<Task> filterTasksByPermissions(List<Task> tasks, String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }

        return tasks.stream()
            .filter(task -> canPerformTaskAction(task, action, authentication))
            .collect(Collectors.toList());
    }

    /**
     * Check if user can access tasks for a specific case based on allegation classification
     */
    public boolean canAccessCaseTasks(String caseId, String classification) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream()
            .map(auth -> auth.getAuthority().replace("ROLE_", ""))
            .collect(Collectors.toList());

        // Admin and Director can access all cases
        if (roles.contains("ADMIN") || roles.contains("DIRECTOR")) {
            return true;
        }

        // Role-based access based on classification
        if ("HR".equals(classification)) {
            return roles.contains("HR_SPECIALIST");
        } else if ("LEGAL".equals(classification)) {
            return roles.contains("LEGAL_COUNSEL");
        } else if ("CSIS".equals(classification)) {
            return roles.contains("SECURITY_ANALYST");
        }

        // Investigators can read all cases but may have limited modification rights
        return roles.contains("INVESTIGATOR") || roles.contains("INTAKE_ANALYST");
    }
}