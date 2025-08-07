package com.workflow.cmsflowable.controller;

import com.workflow.cmsflowable.dto.response.QueueTaskResponse;
import com.workflow.cmsflowable.dto.response.WorkflowTaskResponse;
import com.workflow.cmsflowable.service.QueueTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/workflow")
@Tag(name = "Workflow Management", description = "APIs for managing workflow tasks and processes")
public class WorkflowController {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private QueueTaskService queueTaskService;

    @PostMapping("/start/{processKey}")
    public ResponseEntity<Map<String, Object>> startProcess(
            @PathVariable String processKey,
            @RequestBody(required = false) Map<String, Object> variables) {

        if (variables == null) {
            variables = new HashMap<>();
        }

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey, variables);

        Map<String, Object> response = new HashMap<>();
        response.put("processInstanceId", processInstance.getId());
        response.put("processDefinitionId", processInstance.getProcessDefinitionId());
        response.put("businessKey", processInstance.getBusinessKey());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/tasks")
    @Operation(summary = "Get workflow tasks", description = "Retrieve workflow tasks with optional filtering by assignee or candidate group")
    public ResponseEntity<List<WorkflowTaskResponse>> getTasks(
            @Parameter(description = "Filter by assignee") @RequestParam(required = false) String assignee,
            @Parameter(description = "Filter by candidate group") @RequestParam(required = false) String candidateGroup) {

        List<Task> tasks;

        if (assignee != null && !assignee.isEmpty()) {
            tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        } else if (candidateGroup != null && !candidateGroup.isEmpty()) {
            tasks = taskService.createTaskQuery().taskCandidateGroup(candidateGroup).list();
        } else {
            tasks = taskService.createTaskQuery().list();
        }

        List<WorkflowTaskResponse> taskResponses = tasks.stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(taskResponses);
    }

    @GetMapping("/my-tasks")
    @Operation(summary = "Get my tasks", description = "Get tasks assigned to the current user based on their role")
    public ResponseEntity<List<WorkflowTaskResponse>> getMyTasks(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).build();
            }

            String username = authentication.getName();
            String userRole = getUserRoleFromAuthentication(authentication);

            // Debug logging
            System.out.println("🔍 My tasks request from user: " + username);
            System.out.println("🔍 User role: " + userRole);
            System.out.println("🔍 User authorities: " + authentication.getAuthorities());

            List<Task> tasks;

            // Map user roles to candidate groups
            switch (userRole.toUpperCase()) {
                case "IU_MANAGER":
                case "INTAKE_ANALYST":
                    tasks = taskService.createTaskQuery().taskCandidateGroup("INTAKE_ANALYST_GROUP").list();
                    break;
                case "HR_SPECIALIST":
                    tasks = taskService.createTaskQuery().taskCandidateGroup("HR_GROUP").list();
                    break;
                case "LEGAL_COUNSEL":
                    tasks = taskService.createTaskQuery().taskCandidateGroup("LEGAL_GROUP").list();
                    break;
                case "SECURITY_ANALYST":
                    tasks = taskService.createTaskQuery().taskCandidateGroup("CSIS_GROUP").list();
                    break;
                case "INVESTIGATOR":
                    tasks = taskService.createTaskQuery().taskCandidateGroup("INVESTIGATOR_GROUP").list();
                    break;
                case "DIRECTOR":
                    tasks = taskService.createTaskQuery().taskCandidateGroup("DIRECTOR_GROUP").list();
                    break;
                default:
                    // If no specific role mapping, get tasks assigned to user
                    tasks = taskService.createTaskQuery().taskAssignee(username).list();
            }

            List<WorkflowTaskResponse> taskResponses = tasks.stream()
                    .map(this::convertToTaskResponse)
                    .collect(Collectors.toList());

            System.out.println("✅ Returning " + taskResponses.size() + " tasks for user: " + username);

            return ResponseEntity.ok(taskResponses);
        } catch (Exception e) {
            System.err.println("❌ Error fetching tasks for user " + authentication.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "Get task details", description = "Get detailed information about a specific task")
    public ResponseEntity<WorkflowTaskResponse> getTaskDetails(
            @Parameter(description = "Task ID") @PathVariable String taskId) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                return ResponseEntity.notFound().build();
            }

            WorkflowTaskResponse taskResponse = convertToTaskResponse(task);
            return ResponseEntity.ok(taskResponse);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/cases/{caseId}/tasks")
    @Operation(summary = "Get tasks for case", description = "Get all tasks associated with a specific case")
    public ResponseEntity<List<WorkflowTaskResponse>> getTasksForCase(
            @Parameter(description = "Case ID") @PathVariable String caseId) {
        try {
            List<Task> tasks = taskService.createTaskQuery()
                    .processVariableValueEquals("caseId", caseId)
                    .list();

            List<WorkflowTaskResponse> taskResponses = tasks.stream()
                    .map(this::convertToTaskResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(taskResponses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/tasks/{taskId}/assign")
    @Operation(summary = "Assign task", description = "Assign a task to a specific user")
    public ResponseEntity<String> assignTask(
            @Parameter(description = "Task ID") @PathVariable String taskId,
            @Parameter(description = "User ID to assign task to") @RequestParam String userId) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                return ResponseEntity.notFound().build();
            }

            taskService.setAssignee(taskId, userId);
            return ResponseEntity.ok("Task assigned successfully to user: " + userId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to assign task: " + e.getMessage());
        }
    }

    private WorkflowTaskResponse convertToTaskResponse(Task task) {
        WorkflowTaskResponse response = new WorkflowTaskResponse();
        response.setTaskId(task.getId());
        response.setTaskName(task.getName());
        response.setTaskDefinitionKey(task.getTaskDefinitionKey());
        response.setProcessInstanceId(task.getProcessInstanceId());
        response.setAssignee(task.getAssignee());
        response.setCreated(task.getCreateTime());
        response.setDueDate(task.getDueDate());
        response.setPriority(task.getPriority());
        response.setDescription(task.getDescription());
        response.setFormKey(task.getFormKey());

        // Get process variables
        Map<String, Object> variables = taskService.getVariables(task.getId());
        response.setVariables(variables);

        return response;
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<String> completeTask(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> variables) {

        if (variables == null) {
            variables = new HashMap<>();
        }

        taskService.complete(taskId, variables);
        return ResponseEntity.ok("Task completed successfully");
    }

    @GetMapping("/process/{processInstanceId}")
    public ResponseEntity<ProcessInstance> getProcessInstance(@PathVariable String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance != null) {
            return ResponseEntity.ok(processInstance);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/processes")
    public ResponseEntity<List<ProcessInstance>> getActiveProcesses() {
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
        return ResponseEntity.ok(processInstances);
    }

    // ========== QUEUE-BASED TASK MANAGEMENT ==========

    @GetMapping("/queue/{queueName}/tasks")
    @Operation(summary = "Get tasks from queue", description = "Get tasks from a specific queue, optionally filtering for unassigned tasks only")
    public ResponseEntity<List<QueueTaskResponse>> getQueueTasks(
            @Parameter(description = "Queue name") @PathVariable String queueName,
            @Parameter(description = "Show unassigned tasks only") @RequestParam(defaultValue = "false") boolean unassignedOnly) {
        try {
            List<QueueTaskResponse> tasks = queueTaskService.getTasksByQueue(queueName, unassignedOnly);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/queue/{queueName}/next")
    @Operation(summary = "Get next task from queue", description = "Get the next available (highest priority, oldest) task from a queue")
    public ResponseEntity<QueueTaskResponse> getNextTaskFromQueue(
            @Parameter(description = "Queue name") @PathVariable String queueName) {
        try {
            QueueTaskResponse task = queueTaskService.getNextTaskFromQueue(queueName);
            if (task != null) {
                return ResponseEntity.ok(task);
            } else {
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/queue/tasks/{taskId}/claim")
    @Operation(summary = "Claim a queue task", description = "Claim a task from the queue for a specific user")
    public ResponseEntity<QueueTaskResponse> claimQueueTask(
            @Parameter(description = "Task ID") @PathVariable String taskId,
            @Parameter(description = "User ID") @RequestParam String userId) {
        try {
            QueueTaskResponse task = queueTaskService.claimTask(taskId, userId);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/queue/tasks/{taskId}/unclaim")
    @Operation(summary = "Unclaim a queue task", description = "Release a claimed task back to the queue")
    public ResponseEntity<QueueTaskResponse> unclaimQueueTask(
            @Parameter(description = "Task ID") @PathVariable String taskId) {
        try {
            QueueTaskResponse task = queueTaskService.unclaimTask(taskId);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/queue/my-tasks")
    @Operation(summary = "Get my claimed tasks", description = "Get tasks claimed by the current user")
    public ResponseEntity<List<QueueTaskResponse>> getMyQueueTasks(Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).build();
            }

            String username = authentication.getName();
            List<QueueTaskResponse> tasks = queueTaskService.getTasksByAssignee(username);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/queue/tasks/{taskId}")
    @Operation(summary = "Get queue task details", description = "Get detailed information about a queue task")
    public ResponseEntity<QueueTaskResponse> getQueueTaskDetails(
            @Parameter(description = "Task ID") @PathVariable String taskId) {
        try {
            QueueTaskResponse task = queueTaskService.getQueueTask(taskId);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/queue/statistics/{queueName}")
    @Operation(summary = "Get queue statistics", description = "Get statistics for a specific queue")
    public ResponseEntity<Map<String, Object>> getQueueStatistics(
            @Parameter(description = "Queue name") @PathVariable String queueName) {
        try {
            Map<String, Object> stats = queueTaskService.getQueueStatistics(queueName);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/queue/names")
    @Operation(summary = "Get all queue names", description = "Get list of all available queue names")
    public ResponseEntity<List<String>> getAllQueueNames() {
        try {
            List<String> queueNames = queueTaskService.getAllQueueNames();
            return ResponseEntity.ok(queueNames);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/queue/cases/{caseId}/tasks")
    @Operation(summary = "Get queue tasks for case", description = "Get all queue tasks associated with a specific case")
    public ResponseEntity<List<QueueTaskResponse>> getQueueTasksForCase(
            @Parameter(description = "Case ID") @PathVariable String caseId) {
        try {
            // Find process instance by business key (case ID)
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(caseId)
                    .singleResult();
            
            if (processInstance != null) {
                List<QueueTaskResponse> tasks = queueTaskService.getTasksByProcessInstance(processInstance.getId());
                return ResponseEntity.ok(tasks);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Helper method to extract user role from authentication
     */
    private String getUserRoleFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                    .orElse("USER");
        }
        return "USER";
    }
}