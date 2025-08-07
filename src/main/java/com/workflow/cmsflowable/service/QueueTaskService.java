package com.workflow.cmsflowable.service;

import com.workflow.cmsflowable.dto.response.QueueTaskResponse;
import com.workflow.cmsflowable.entity.QueueTask;
import com.workflow.cmsflowable.entity.WorkflowMetadata;
import com.workflow.cmsflowable.enums.TaskStatus;
import com.workflow.cmsflowable.exception.ResourceNotFoundException;
import com.workflow.cmsflowable.model.TaskQueueMapping;
import com.workflow.cmsflowable.repository.QueueTaskRepository;
import com.workflow.cmsflowable.repository.WorkflowMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QueueTaskService {
    
    private final WorkflowMetadataRepository workflowMetadataRepository;
    private final QueueTaskRepository queueTaskRepository;
    private final org.flowable.engine.TaskService taskService;
    private final RuntimeService runtimeService;
    
    /**
     * Populate queue tasks for a newly started process instance
     */
    public void populateQueueTasksForProcessInstance(String processInstanceId, String processDefinitionKey) {
        log.info("Populating queue tasks for process instance: {}", processInstanceId);
        
        // Find workflow metadata
        Optional<WorkflowMetadata> metadataOpt = workflowMetadataRepository
                .findByProcessDefinitionKey(processDefinitionKey);
                
        if (metadataOpt.isEmpty()) {
            log.warn("No workflow metadata found for process: {}", processDefinitionKey);
            return;
        }
        
        WorkflowMetadata metadata = metadataOpt.get();
        
        // Get all active tasks for this process instance
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list();
                
        log.info("Found {} active tasks for process instance {}", tasks.size(), processInstanceId);
        
        // Process each task
        for (Task task : tasks) {
            try {
                populateQueueTask(task, metadata, processDefinitionKey);
            } catch (Exception e) {
                log.error("Failed to populate queue task for task {}: {}", task.getId(), e.getMessage(), e);
                // Continue with other tasks even if one fails
            }
        }
    }
    
    private void populateQueueTask(Task task, WorkflowMetadata metadata, String processDefinitionKey) {
        // Find the queue for this task
        String queueName = findQueueForTask(task, metadata);
        
        if (queueName == null) {
            log.warn("No queue mapping found for task {} in process {}", 
                task.getTaskDefinitionKey(), processDefinitionKey);
            return;
        }
        
        // Insert into queue_tasks table
        insertQueueTask(task, queueName, processDefinitionKey);
    }
    
    private String findQueueForTask(Task task, WorkflowMetadata metadata) {
        // Look for task in the task queue mappings
        if (metadata.getTaskQueueMappings() != null) {
            for (TaskQueueMapping mapping : metadata.getTaskQueueMappings()) {
                if (mapping.getTaskId().equals(task.getTaskDefinitionKey())) {
                    log.debug("Found queue '{}' for task '{}'", mapping.getQueue(), task.getTaskDefinitionKey());
                    return mapping.getQueue();
                }
            }
        }

        // Fallback: use candidate groups to find queue
        List<String> candidateGroups = taskService.getIdentityLinksForTask(task.getId())
                .stream()
                .filter(link -> "candidate".equals(link.getType()) && link.getGroupId() != null)
                .map(link -> link.getGroupId())
                .collect(Collectors.toList());
                
        for (String group : candidateGroups) {
            String queue = metadata.getCandidateGroupMappings().get(group);
            if (queue != null) {
                log.debug("Found queue '{}' for candidate group '{}'", queue, group);
                return queue;
            }
        }

        // Default fallback
        return metadata.getCandidateGroupMappings().getOrDefault("default", "default-queue");
    }
    
    private void insertQueueTask(Task task, String queueName, String processDefinitionKey) {
        // Check if task already exists
        if (queueTaskRepository.existsByTaskId(task.getId())) {
            log.debug("Task {} already exists in queue, skipping", task.getId());
            return;
        }
        
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("description", task.getDescription());
        taskData.put("dueDate", task.getDueDate());
        taskData.put("createTime", task.getCreateTime());
        taskData.put("owner", task.getOwner());
        taskData.put("taskDefinitionKey", task.getTaskDefinitionKey());
        taskData.put("formKey", task.getFormKey());
        
        QueueTask queueTask = QueueTask.builder()
                .taskId(task.getId())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionKey(processDefinitionKey)
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .taskName(task.getName())
                .queueName(queueName)
                .assignee(task.getAssignee())
                .status(TaskStatus.OPEN)
                .priority(task.getPriority() > 0 ? task.getPriority() : 50)
                .taskData(taskData)
                .build();
                
        try {
            queueTaskRepository.save(queueTask);
            log.info("Successfully inserted task {} into queue '{}' for process instance {}", 
                task.getId(), queueName, task.getProcessInstanceId());
        } catch (Exception e) {
            log.error("Failed to insert queue task: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to insert queue task", e);
        }
    }
    
    /**
     * Get tasks by queue name
     */
    public List<QueueTaskResponse> getTasksByQueue(String queueName, boolean unassignedOnly) {
        List<QueueTask> tasks;
        if (unassignedOnly) {
            tasks = queueTaskRepository.findAvailableTasksInQueue(queueName, TaskStatus.OPEN);
        } else {
            tasks = queueTaskRepository.findByQueueNameAndStatusOrderByPriorityDescCreatedAtAsc(queueName, TaskStatus.OPEN);
        }
        
        return tasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get tasks by queue name with pagination
     */
    public List<QueueTaskResponse> getTasksByQueue(String queueName, boolean unassignedOnly, Pageable pageable) {
        List<QueueTask> tasks;
        if (unassignedOnly) {
            tasks = queueTaskRepository.findAvailableTasksInQueue(queueName, TaskStatus.OPEN, pageable);
        } else {
            tasks = queueTaskRepository.findByQueueNameAndStatusOrderByPriorityDescCreatedAtAsc(queueName, TaskStatus.OPEN, pageable);
        }
        
        return tasks.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    
    /**
     * Get tasks by assignee
     */
    public List<QueueTaskResponse> getTasksByAssignee(String userId) {
        List<QueueTask> tasks = queueTaskRepository.findByAssigneeAndStatus(userId, TaskStatus.CLAIMED);
        tasks.addAll(queueTaskRepository.findByAssigneeAndStatus(userId, TaskStatus.OPEN));
        
        return tasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get a single queue task by ID
     */
    public QueueTaskResponse getQueueTask(String taskId) {
        QueueTask queueTask = queueTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        
        return mapToResponse(queueTask);
    }
    
    /**
     * Claim a task
     */
    public QueueTaskResponse claimTask(String taskId, String userId) {
        // Validation
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be null or empty");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        QueueTask queueTask = queueTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        
        // Validate task can be claimed
        if (queueTask.getStatus() != TaskStatus.OPEN) {
            throw new IllegalStateException("Task " + taskId + " cannot be claimed. Current status: " + queueTask.getStatus());
        }
        
        if (queueTask.getAssignee() != null && !queueTask.getAssignee().equals(userId)) {
            throw new IllegalStateException("Task " + taskId + " is already assigned to: " + queueTask.getAssignee());
        }
        
        queueTask.setAssignee(userId);
        queueTask.setStatus(TaskStatus.CLAIMED);
        queueTask.setClaimedAt(Instant.now());
        
        queueTaskRepository.save(queueTask);
        
        log.info("Task {} claimed by user {} in queue", taskId, userId);
        
        return mapToResponse(queueTask);
    }
    
    /**
     * Unclaim a task
     */
    public QueueTaskResponse unclaimTask(String taskId) {
        // Validation
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be null or empty");
        }
        
        QueueTask queueTask = queueTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        
        // Validate task can be unclaimed
        if (queueTask.getStatus() != TaskStatus.CLAIMED) {
            throw new IllegalStateException("Task " + taskId + " cannot be unclaimed. Current status: " + queueTask.getStatus());
        }
        
        queueTask.setAssignee(null);
        queueTask.setStatus(TaskStatus.OPEN);
        queueTask.setClaimedAt(null);
        
        queueTaskRepository.save(queueTask);
        
        log.info("Task {} unclaimed in queue", taskId);
        
        return mapToResponse(queueTask);
    }
    
    /**
     * Complete a task
     */
    public void completeTask(String taskId) {
        // Validation
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new IllegalArgumentException("Task ID cannot be null or empty");
        }
        
        QueueTask queueTask = queueTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));
        
        // Validate task can be completed
        if (queueTask.getStatus() == TaskStatus.COMPLETED) {
            log.debug("Task {} is already completed", taskId);
            return; // Already completed, no need to update
        }
        
        queueTask.setStatus(TaskStatus.COMPLETED);
        queueTask.setCompletedAt(Instant.now());
        
        queueTaskRepository.save(queueTask);
        
        log.info("Task {} marked as completed in queue", taskId);
    }
    
    /**
     * Get tasks by process instance ID
     */
    public List<QueueTaskResponse> getTasksByProcessInstance(String processInstanceId) {
        List<QueueTask> tasks = queueTaskRepository.findByProcessInstanceIdAndStatus(
                processInstanceId, TaskStatus.OPEN);
        
        return tasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get next available (unassigned) task from queue
     */
    public QueueTaskResponse getNextTaskFromQueue(String queueName) {
        // Get unassigned tasks ordered by priority (desc) and creation time (asc)
        List<QueueTask> tasks = queueTaskRepository.findAvailableTasksInQueue(
                queueName, TaskStatus.OPEN);
        
        if (tasks.isEmpty()) {
            return null;
        }
        
        // Return the first task (highest priority, oldest)
        return mapToResponse(tasks.get(0));
    }
    
    /**
     * Get queue statistics
     */
    public Map<String, Object> getQueueStatistics(String queueName) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("queueName", queueName);
        stats.put("openTasks", queueTaskRepository.countByQueueNameAndStatus(queueName, TaskStatus.OPEN));
        stats.put("claimedTasks", queueTaskRepository.countByQueueNameAndStatus(queueName, TaskStatus.CLAIMED));
        stats.put("completedTasks", queueTaskRepository.countByQueueNameAndStatus(queueName, TaskStatus.COMPLETED));
        stats.put("totalTasks", queueTaskRepository.countByQueueName(queueName));
        return stats;
    }
    
    /**
     * Get all queue names
     */
    public List<String> getAllQueueNames() {
        return queueTaskRepository.findDistinctQueueNames();
    }
    
    /**
     * Map QueueTask entity to response DTO
     */
    private QueueTaskResponse mapToResponse(QueueTask queueTask) {
        QueueTaskResponse response = QueueTaskResponse.builder()
                .taskId(queueTask.getTaskId())
                .processInstanceId(queueTask.getProcessInstanceId())
                .processDefinitionKey(queueTask.getProcessDefinitionKey())
                .taskDefinitionKey(queueTask.getTaskDefinitionKey())
                .taskName(queueTask.getTaskName())
                .queueName(queueTask.getQueueName())
                .assignee(queueTask.getAssignee())
                .status(queueTask.getStatus().getValue())
                .priority(queueTask.getPriority())
                .createdAt(queueTask.getCreatedAt())
                .claimedAt(queueTask.getClaimedAt())
                .completedAt(queueTask.getCompletedAt())
                .taskData(queueTask.getTaskData())
                .build();
                
        // Get business key from Flowable API if process instance exists
        if (queueTask.getProcessInstanceId() != null) {
            try {
                ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                        .processInstanceId(queueTask.getProcessInstanceId())
                        .singleResult();
                if (processInstance != null) {
                    response.setBusinessKey(processInstance.getBusinessKey());
                }
            } catch (Exception e) {
                log.debug("Could not fetch business key for process instance: {}", queueTask.getProcessInstanceId());
            }
        }
        
        return response;
    }
}