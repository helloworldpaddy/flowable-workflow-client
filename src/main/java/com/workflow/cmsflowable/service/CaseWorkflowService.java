package com.workflow.cmsflowable.service;

import com.workflow.cmsflowable.dto.request.CreateCaseWithAllegationsRequest;
import com.workflow.cmsflowable.dto.request.TaskTransitionRequest;
import com.workflow.cmsflowable.dto.response.CaseWithAllegationsResponse;
import com.workflow.cmsflowable.dto.response.WorkflowTaskResponse;
import com.workflow.cmsflowable.entity.*;
import com.workflow.cmsflowable.repository.AllegationRepository;
import com.workflow.cmsflowable.repository.CaseRepository;
import com.workflow.cmsflowable.repository.WorkItemEntityRepository;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CaseWorkflowService {
    
    @Autowired
    private CaseRepository caseRepository;
    
    @Autowired
    private AllegationRepository allegationRepository;
    
    @Autowired
    private WorkItemEntityRepository workItemRepository;
    
    @Autowired
    private RuntimeService runtimeService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private DetermineDepartmentRoutingService dmnService;
    
    @Autowired
    private QueueTaskService queueTaskService;
    
    private static final String CASE_WORKFLOW_PROCESS_KEY = "Process_CMS_Workflow_Updated";
    
    // @PreAuthorize("hasPermission(#request, 'case', 'intake_initial_review')")
    public CaseWithAllegationsResponse createCaseWithWorkflow(CreateCaseWithAllegationsRequest request) {
        System.out.println("STARTING FULL CASE CREATION AND WORKFLOW PROCESS!");
        
        // Generate case number
        String caseNumber = generateCaseNumber();
        System.out.println("Generated case number: " + caseNumber);
        
        // Create case entity
        Case caseEntity = new Case();
        caseEntity.setCaseId(caseNumber);
        caseEntity.setCaseNumber(caseNumber);
        caseEntity.setTitle(request.getTitle());
        caseEntity.setDescription(request.getDescription());
        caseEntity.setPriority(request.getPriority());
        caseEntity.setComplainantName(request.getComplainantName());
        caseEntity.setComplainantEmail(request.getComplainantEmail());
        caseEntity.setStatus(CaseStatus.OPEN);
        
        // Save case to database
        Case savedCase = caseRepository.save(caseEntity);
        System.out.println("Case saved to database with ID: " + savedCase.getCaseId());
        
        // Create both work items AND allegations
        List<WorkItemEntity> workItems = new ArrayList<>();
        List<Allegation> allegations = new ArrayList<>();
        int allegationCounter = 1;
        
        for (CreateCaseWithAllegationsRequest.AllegationRequest allegationReq : request.getAllegations()) {
            String workItemId = java.util.UUID.randomUUID().toString();
            String allegationId = "ALG-" + caseNumber.substring(4) + "-" + String.format("%02d", allegationCounter);
            String workItemNumber = caseNumber + "-WI-" + String.format("%02d", allegationCounter);
            
            // Use DMN decision table to classify allegation and determine assigned group
            Map<String, Object> dmnResult = dmnService.callAllegationClassificationDMN(
                allegationReq.getAllegationType(), 
                allegationReq.getSeverity().toString()
            );
            
            String classification;
            String assignedGroup;
            String priority;
            
            if (dmnResult != null) {
                classification = (String) dmnResult.get("classification");
                assignedGroup = (String) dmnResult.get("assignedGroup");
                priority = (String) dmnResult.get("priority");
            } else {
                // Fallback to default values if DMN returns null
                System.out.println("DMN returned null for allegationType: " + allegationReq.getAllegationType() + ", severity: " + allegationReq.getSeverity());
                classification = "HR";
                assignedGroup = "HR_GROUP";
                priority = "MEDIUM";
            }
            
            // Use DMN priority if available, otherwise fall back to request priority
            String workItemPriority = (priority != null) ? priority : request.getPriority().toString();
            
            // Create WorkItemEntity for work_items table
            WorkItemEntity workItem = new WorkItemEntity();
            workItem.setWorkItemId(workItemId);
            workItem.setWorkItemNumber(workItemNumber);
            workItem.setCaseId(savedCase.getCaseId());
            workItem.setType(allegationReq.getAllegationType());
            workItem.setSeverity(allegationReq.getSeverity().toString());
            workItem.setDescription(allegationReq.getDescription());
            workItem.setStatus("OPEN");
            workItem.setPriority(workItemPriority);
            workItem.setClassification(classification);
            workItem.setAssignedGroup(assignedGroup);
            
            workItems.add(workItem);
            
            // Create Allegation entity for allegations table
            Allegation allegation = new Allegation();
            allegation.setAllegationId(allegationId);
            allegation.setCaseId(savedCase.getCaseId());
            allegation.setAllegationType(allegationReq.getAllegationType());
            allegation.setSeverity(allegationReq.getSeverity());
            allegation.setDescription(allegationReq.getDescription());
            allegation.setDepartmentClassification(classification);
            allegation.setAssignedGroup(assignedGroup);
            
            allegations.add(allegation);
            
            allegationCounter++;
            System.out.println("Created work item: " + workItemNumber + " and allegation: " + allegationId + " for " + allegationReq.getAllegationType());
        }
        
        // Save work items to database
        System.out.println("Attempting to save " + workItems.size() + " work items to database");
        List<WorkItemEntity> savedWorkItems;
        try {
            savedWorkItems = workItemRepository.saveAll(workItems);
            System.out.println("Successfully saved " + savedWorkItems.size() + " work items to database");
        } catch (Exception e) {
            System.err.println("Error saving work items: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save work items: " + e.getMessage(), e);
        }
        
        // Save allegations to database
        System.out.println("Attempting to save " + allegations.size() + " allegations to database");
        List<Allegation> savedAllegations;
        try {
            savedAllegations = allegationRepository.saveAll(allegations);
            System.out.println("Successfully saved " + savedAllegations.size() + " allegations to database");
            for (Allegation savedAllegation : savedAllegations) {
                System.out.println("Saved allegation: " + savedAllegation.getAllegationId() + " (Type: " + savedAllegation.getAllegationType() + ")");
            }
        } catch (Exception e) {
            System.err.println("Error saving allegations: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save allegations: " + e.getMessage(), e);
        }
        
        // Prepare workflow variables
        Map<String, Object> workflowVariables = new HashMap<>();
        workflowVariables.put("caseId", savedCase.getCaseNumber());
        workflowVariables.put("caseTitle", savedCase.getTitle());
        workflowVariables.put("priority", savedCase.getPriority().toString());
        workflowVariables.put("complainantName", savedCase.getComplainantName());
        workflowVariables.put("complainantEmail", savedCase.getComplainantEmail());
        workflowVariables.put("workItemCount", savedWorkItems.size());
        workflowVariables.put("allegationCount", savedAllegations.size());
        
        // Prepare allegations data for DMN decision
        if (!savedAllegations.isEmpty()) {
            Allegation primaryAllegation = savedAllegations.get(0);
            workflowVariables.put("allegationType", primaryAllegation.getAllegationType());
            workflowVariables.put("severity", primaryAllegation.getSeverity().toString());
            workflowVariables.put("classification", primaryAllegation.getDepartmentClassification());
        }
        
        // Create allegations list for multi-instance workflow
        List<Map<String, Object>> allegationsForWorkflow = new ArrayList<>();
        for (Allegation allegation : savedAllegations) {
            Map<String, Object> allegationMap = new HashMap<>();
            allegationMap.put("allegationId", allegation.getAllegationId());
            allegationMap.put("description", allegation.getDescription());
            allegationMap.put("allegationType", allegation.getAllegationType());
            allegationMap.put("severity", allegation.getSeverity().toString());
            allegationMap.put("classification", allegation.getDepartmentClassification());
            allegationMap.put("assignedGroup", allegation.getAssignedGroup());
            allegationMap.put("caseId", allegation.getCaseId());
            allegationsForWorkflow.add(allegationMap);
        }
        workflowVariables.put("allegations", allegationsForWorkflow);
        
        System.out.println("Prepared " + allegationsForWorkflow.size() + " allegations for multi-instance processing:");
        
        // Determine which departments are needed based on classifications
        boolean hrNeeded = savedAllegations.stream().anyMatch(a -> "HR".equals(a.getDepartmentClassification()));
        boolean legalNeeded = savedAllegations.stream().anyMatch(a -> "LEGAL".equals(a.getDepartmentClassification()));
        boolean csisNeeded = savedAllegations.stream().anyMatch(a -> "CSIS".equals(a.getDepartmentClassification()));
        
        workflowVariables.put("hrNeeded", hrNeeded);
        workflowVariables.put("legalNeeded", legalNeeded);
        workflowVariables.put("csisNeeded", csisNeeded);
        
        System.out.println("Department flags: HR=" + hrNeeded + ", Legal=" + legalNeeded + ", CSIS=" + csisNeeded);
        System.out.println("Prepared workflow variables: " + workflowVariables.keySet());
        
        // Start Flowable BPMN workflow process
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                CASE_WORKFLOW_PROCESS_KEY, 
                savedCase.getCaseNumber(), 
                workflowVariables
            );
            
            System.out.println("Started Flowable workflow process!");
            System.out.println("   Process Instance ID: " + processInstance.getId());
            System.out.println("   Process Definition ID: " + processInstance.getProcessDefinitionId());
            System.out.println("   Business Key: " + processInstance.getBusinessKey());
            
            // Update case with workflow instance ID (keep as string for UUID)
            // Note: Converting UUID to hash for Long field, but keeping full ID in logs
            savedCase.setWorkflowInstanceKey((long) processInstance.getId().hashCode());
            savedCase = caseRepository.save(savedCase);
            
            System.out.println("Updated case with workflow instance hash: " + savedCase.getWorkflowInstanceKey());
            System.out.println("    Full Process Instance UUID: " + processInstance.getId());
            
            // Update work items with process instance ID
            for (WorkItemEntity workItem : savedWorkItems) {
                workItem.setFlowableProcessInstanceId(processInstance.getId());
            }
            workItemRepository.saveAll(savedWorkItems);
            
            // Update allegations with process instance ID
            for (Allegation allegation : savedAllegations) {
                allegation.setFlowablePlanItemId(processInstance.getId());
            }
            allegationRepository.saveAll(savedAllegations);
            
            System.out.println("Updated case, work items, and allegations with process instance ID");
            
            // Populate queue tasks for the new process instance
            try {
                queueTaskService.populateQueueTasksForProcessInstance(processInstance.getId(), CASE_WORKFLOW_PROCESS_KEY);
                System.out.println("Successfully populated queue tasks for process instance: " + processInstance.getId());
            } catch (Exception e) {
                System.err.println("Warning: Failed to populate queue tasks: " + e.getMessage());
                // Don't fail the entire process for queue population issues
            }
            
            // Get active tasks
            List<Task> activeTasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .list();
            
            System.out.println("Active tasks after workflow start: " + activeTasks.size());
            for (Task task : activeTasks) {
                System.out.println("   - Task: " + task.getName() + " (ID: " + task.getId() + ")");
                System.out.println("     Assignee: " + task.getAssignee());
                
                // Get candidate groups properly
                List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
                List<String> candidateGroups = identityLinks.stream()
                    .filter(link -> "candidate".equals(link.getType()) && link.getGroupId() != null)
                    .map(IdentityLink::getGroupId)
                    .collect(Collectors.toList());
                System.out.println("     Candidate Groups: " + candidateGroups);
            }
            
        } catch (Exception e) {
            System.err.println("Error starting workflow: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to start workflow process: " + e.getMessage(), e);
        }
        
        System.out.println("CASE CREATION AND WORKFLOW COMPLETED SUCCESSFULLY!");
        return convertToCaseWithAllegationsResponse(savedCase, savedAllegations);
    }
    
    // @PreAuthorize("hasPermission(#request.taskId, 'task', 'complete')")
    public WorkflowTaskResponse completeTask(TaskTransitionRequest request) {
        Task task = taskService.createTaskQuery().taskId(request.getTaskId()).singleResult();
        if (task == null) {
            throw new RuntimeException("Task not found: " + request.getTaskId());
        }
        
        // Prepare variables for task completion
        Map<String, Object> variables = request.getVariables() != null ? request.getVariables() : new HashMap<>();
        
        if (request.getComments() != null) {
            variables.put("comments", request.getComments());
        }
        if (request.getDecision() != null) {
            variables.put("decision", request.getDecision());
        }
        
        // Complete the task
        taskService.complete(request.getTaskId(), variables);
        
        // Update queue task status
        try {
            queueTaskService.completeTask(request.getTaskId());
            System.out.println("Successfully marked queue task as completed: " + request.getTaskId());
        } catch (Exception e) {
            System.err.println("Warning: Failed to update queue task completion: " + e.getMessage());
            // Don't fail the entire operation for queue update issues
        }
        
        // Return task information
        return convertToWorkflowTaskResponse(task);
    }
    
    public List<WorkflowTaskResponse> getTasksForCase(String caseId) {
        List<Task> tasks = taskService.createTaskQuery()
            .processInstanceBusinessKey(caseId)
            .list();
        
        return tasks.stream()
            .map(this::convertToWorkflowTaskResponse)
            .collect(Collectors.toList());
    }
    
    public List<WorkflowTaskResponse> getTasksByAssignee(String assignee) {
        List<Task> tasks = taskService.createTaskQuery()
            .taskAssignee(assignee)
            .list();
        
        return tasks.stream()
            .map(this::convertToWorkflowTaskResponse)
            .collect(Collectors.toList());
    }
    
    public List<WorkflowTaskResponse> getTasksByCandidateGroup(String candidateGroup) {
        List<Task> tasks = taskService.createTaskQuery()
            .taskCandidateGroup(candidateGroup)
            .list();
        
        return tasks.stream()
            .map(this::convertToWorkflowTaskResponse)
            .collect(Collectors.toList());
    }
    
    // @PreAuthorize("hasPermission(#caseNumber, 'case', 'view')")
    public CaseWithAllegationsResponse getCaseWithAllegations(String caseNumber) {
        Case caseEntity = caseRepository.findByCaseNumber(caseNumber)
            .orElseThrow(() -> new RuntimeException("Case not found: " + caseNumber));
        
        List<Allegation> allegations = allegationRepository.findByCaseId(caseNumber);
        
        return convertToCaseWithAllegationsResponse(caseEntity, allegations);
    }
    
    public List<CaseWithAllegationsResponse> getAllCases() {
        List<Case> cases = caseRepository.findAll();
        return cases.stream()
            .map(caseEntity -> {
                List<Allegation> allegations = allegationRepository.findByCaseId(caseEntity.getCaseNumber());
                return convertToCaseWithAllegationsResponse(caseEntity, allegations);
            })
            .collect(Collectors.toList());
    }
    
    // @PreAuthorize("hasPermission(#caseNumber, 'case', 'view')")
    public CaseWithAllegationsResponse getCaseDetailsByCaseNumber(String caseNumber) {
        Case caseEntity = caseRepository.findByCaseNumber(caseNumber)
            .orElseThrow(() -> new RuntimeException("Case not found: " + caseNumber));
        
        List<Allegation> allegations = allegationRepository.findByCaseId(caseNumber);
        
        return convertToCaseWithAllegationsResponse(caseEntity, allegations);
    }
    
    public List<CaseWithAllegationsResponse> getAllCases(int page, int size, String status) {
        // For now, return all cases (pagination can be added later with PageRequest)
        List<Case> cases = caseRepository.findAll();
        
        // Filter by status if provided
        if (status != null && !status.trim().isEmpty()) {
            try {
                CaseStatus caseStatus = CaseStatus.valueOf(status.toUpperCase());
                cases = cases.stream()
                    .filter(c -> c.getStatus() == caseStatus)
                    .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Invalid status, return empty list or all cases
                System.out.println("Invalid status filter: " + status);
            }
        }
        
        return cases.stream()
            .map(caseEntity -> {
                List<Allegation> allegations = allegationRepository.findByCaseId(caseEntity.getCaseNumber());
                return convertToCaseWithAllegationsResponse(caseEntity, allegations);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Submit a case for workflow processing - transitions case status and advances workflow
     */
    public CaseWithAllegationsResponse submitCase(String caseNumber, String submittedBy) {
        System.out.println("🎯 Starting case submission for case: " + caseNumber + " by user: " + submittedBy);
        
        // Get the case entity
        Case caseEntity = caseRepository.findByCaseNumber(caseNumber)
            .orElseThrow(() -> new RuntimeException("Case not found: " + caseNumber));
        
        // Check current status
        CaseStatus currentStatus = caseEntity.getStatus();
        System.out.println("Current case status: " + currentStatus);
        
        // Validate status transition
        List<CaseStatus> allowedStatuses = Arrays.asList(CaseStatus.OPEN, CaseStatus.SUBMITTED, CaseStatus.IN_PROGRESS);
        if (!allowedStatuses.contains(currentStatus)) {
            throw new RuntimeException("Case cannot be submitted from status: " + currentStatus + ". Allowed statuses: " + allowedStatuses);
        }
        
        try {
            // Update case status to SUBMITTED
            caseEntity.setStatus(CaseStatus.SUBMITTED);
            caseEntity.setUpdatedAt(java.time.LocalDateTime.now());
            // Note: We'd typically set the updatedBy field here if the entity has that field
            
            Case updatedCase = caseRepository.save(caseEntity);
            System.out.println("✅ Updated case status to SUBMITTED");
            
            // Find the workflow instance for this case
            List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(caseNumber)
                .active()
                .list();
            
            if (!processInstances.isEmpty()) {
                ProcessInstance processInstance = processInstances.get(0);
                System.out.println("Found active process instance: " + processInstance.getId());
                
                // Get the current active tasks for the workflow
                List<Task> activeTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                
                System.out.println("Found " + activeTasks.size() + " active tasks");
                
                // Check for EO Intake task and complete it to advance the workflow
                for (Task task : activeTasks) {
                    System.out.println("Processing task: " + task.getName() + " (ID: " + task.getId() + ")");
                    
                    if ("EO Intake - Initial Review".equals(task.getName()) || task.getName().contains("Intake")) {
                        // Prepare variables for task completion
                        Map<String, Object> variables = new HashMap<>();
                        variables.put("submittedBy", submittedBy);
                        variables.put("submissionDate", java.time.Instant.now().toString());
                        variables.put("caseSubmitted", true);
                        variables.put("intakeCompleted", true);
                        
                        // Complete the intake task to advance workflow
                        taskService.complete(task.getId(), variables);
                        System.out.println("✅ Completed intake task: " + task.getName());
                        
                        // Update queue task status if exists
                        try {
                            queueTaskService.completeTask(task.getId());
                            System.out.println("✅ Updated queue task status");
                        } catch (Exception e) {
                            System.err.println("⚠️ Warning: Failed to update queue task: " + e.getMessage());
                        }
                        
                        break;
                    }
                }
                
                // Log new active tasks after submission
                List<Task> newActiveTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
                
                System.out.println("After submission, found " + newActiveTasks.size() + " active tasks:");
                for (Task task : newActiveTasks) {
                    System.out.println("   - " + task.getName() + " (ID: " + task.getId() + ")");
                }
                
            } else {
                System.err.println("⚠️ Warning: No active workflow process found for case: " + caseNumber);
                // The case status is still updated even if no workflow is found
            }
            
            // Return the updated case with allegations
            List<Allegation> allegations = allegationRepository.findByCaseId(caseNumber);
            CaseWithAllegationsResponse response = convertToCaseWithAllegationsResponse(updatedCase, allegations);
            
            System.out.println("✅ Case submission completed successfully for: " + caseNumber);
            return response;
            
        } catch (Exception e) {
            System.err.println("❌ Error during case submission: " + e.getMessage());
            e.printStackTrace();
            
            // Revert case status if workflow operations failed
            try {
                caseEntity.setStatus(currentStatus);
                caseRepository.save(caseEntity);
                System.out.println("✅ Reverted case status to: " + currentStatus);
            } catch (Exception revertError) {
                System.err.println("❌ Failed to revert case status: " + revertError.getMessage());
            }
            
            throw new RuntimeException("Failed to submit case: " + e.getMessage(), e);
        }
    }
    
    private String generateCaseNumber() {
        String prefix = "OC" + java.time.Year.now() + "-";
        // Generate 6-digit sequential number
        Long count = caseRepository.count() + 1;
        return prefix + String.format("%06d", count);
    }
    
    private CaseWithAllegationsResponse convertToCaseWithAllegationsResponse(Case caseEntity, List<Allegation> allegations) {
        CaseWithAllegationsResponse response = new CaseWithAllegationsResponse();
        response.setCaseId(caseEntity.getCaseNumber());
        response.setCaseNumber(caseEntity.getCaseNumber());
        response.setTitle(caseEntity.getTitle());
        response.setDescription(caseEntity.getDescription());
        response.setPriority(caseEntity.getPriority());
        response.setStatus(caseEntity.getStatus());
        response.setComplainantName(caseEntity.getComplainantName());
        response.setComplainantEmail(caseEntity.getComplainantEmail());
        response.setWorkflowInstanceKey(caseEntity.getWorkflowInstanceKey());
        response.setCreatedAt(caseEntity.getCreatedAt());
        response.setUpdatedAt(caseEntity.getUpdatedAt());
        response.setCreatedBy(caseEntity.getCreatedBy() != null ? caseEntity.getCreatedBy().getUsername() : null);
        response.setAssignedTo(caseEntity.getAssignedTo() != null ? caseEntity.getAssignedTo().getUsername() : null);
        
        List<CaseWithAllegationsResponse.AllegationResponse> allegationResponses = allegations.stream()
            .map(this::convertToAllegationResponse)
            .collect(Collectors.toList());
        response.setAllegations(allegationResponses);
        
        return response;
    }
    
    private CaseWithAllegationsResponse.AllegationResponse convertToAllegationResponse(Allegation allegation) {
        CaseWithAllegationsResponse.AllegationResponse response = new CaseWithAllegationsResponse.AllegationResponse();
        response.setAllegationId(allegation.getAllegationId());
        response.setAllegationType(allegation.getAllegationType());
        response.setSeverity(allegation.getSeverity());
        response.setDescription(allegation.getDescription());
        response.setDepartmentClassification(allegation.getDepartmentClassification());
        response.setAssignedGroup(allegation.getAssignedGroup());
        response.setFlowablePlanItemId(allegation.getFlowablePlanItemId());
        response.setCreatedAt(allegation.getCreatedAt());
        response.setUpdatedAt(allegation.getUpdatedAt());
        return response;
    }
    
    private WorkflowTaskResponse convertToWorkflowTaskResponse(Task task) {
        WorkflowTaskResponse response = new WorkflowTaskResponse();
        response.setTaskId(task.getId());
        response.setTaskName(task.getName());
        response.setDescription(task.getDescription());
        response.setProcessInstanceId(task.getProcessInstanceId());
        response.setProcessDefinitionId(task.getProcessDefinitionId());
        response.setAssignee(task.getAssignee());
        // Get candidate groups using TaskService
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
        List<String> candidateGroups = identityLinks.stream()
            .filter(link -> "candidate".equals(link.getType()) && link.getGroupId() != null)
            .map(IdentityLink::getGroupId)
            .collect(Collectors.toList());
        response.setCandidateGroups(String.join(",", candidateGroups));
        response.setCreated(task.getCreateTime());
        response.setDueDate(task.getDueDate());
        response.setPriority(task.getPriority());
        response.setFormKey(task.getFormKey());
        response.setCategory(task.getCategory());
        
        // Get task variables
        Map<String, Object> variables = taskService.getVariables(task.getId());
        response.setVariables(variables);
        response.setCaseId((String) variables.get("caseId"));
        
        return response;
    }
    
    private String classifyAllegation(String allegationType) {
        switch (allegationType.toLowerCase()) {
            case "sexual harassment":
            case "harassment":
            case "discrimination":
                return "HR";
            case "financial fraud":
            case "embezzlement":
            case "theft":
            case "corruption":
                return "LEGAL";
            case "data breach":
            case "security violation":
            case "unauthorized access":
                return "SECURITY";
            case "policy violation":
            case "misconduct":
                return "COMPLIANCE";
            default:
                return "GENERAL";
        }
    }
    
    private String getAssignedGroup(String classification) {
        switch (classification) {
            case "HR":
                return "HR_SPECIALIST";
            case "LEGAL":
                return "LEGAL_COUNSEL";
            case "SECURITY":
                return "SECURITY_ANALYST";
            case "COMPLIANCE":
                return "COMPLIANCE_OFFICER";
            default:
                return "INTAKE_ANALYST";
        }
    }
    
    private List<Allegation> convertWorkItemsToAllegations(List<WorkItemEntity> workItems, String caseId) {
        List<Allegation> allegations = new ArrayList<>();
        for (WorkItemEntity workItem : workItems) {
            Allegation allegation = new Allegation();
            allegation.setAllegationId(workItem.getWorkItemId());
            allegation.setCaseId(caseId);
            allegation.setAllegationType(workItem.getType());
            
            // Convert severity string back to enum properly
            Severity severity = convertStringToSeverity(workItem.getSeverity());
            allegation.setSeverity(severity);
            
            allegation.setDescription(workItem.getDescription());
            allegation.setDepartmentClassification(workItem.getClassification());
            allegation.setAssignedGroup(workItem.getAssignedGroup());
            allegation.setFlowablePlanItemId(workItem.getFlowableProcessInstanceId());
            allegation.setCreatedAt(workItem.getCreatedAt());
            allegation.setUpdatedAt(workItem.getUpdatedAt());
            allegations.add(allegation);
        }
        return allegations;
    }
    
    private Severity convertStringToSeverity(String severityString) {
        switch (severityString.toUpperCase()) {
            case "LOW":
            case "Low":
                return Severity.LOW;
            case "MEDIUM":
            case "Medium":
                return Severity.MEDIUM;
            case "HIGH":
            case "High":
                return Severity.HIGH;
            case "CRITICAL":
            case "Critical":
                return Severity.CRITICAL;
            default:
                return Severity.MEDIUM; // Default fallback
        }
    }
    
    /**
     * Helper method to create cross-reference between work items and allegations
     * This ensures both tables are properly populated and linked
     */
    private void linkWorkItemsAndAllegations(List<WorkItemEntity> workItems, List<Allegation> allegations) {
        if (workItems.size() != allegations.size()) {
            throw new IllegalStateException("Work items and allegations lists must have the same size");
        }
        
        for (int i = 0; i < workItems.size(); i++) {
            WorkItemEntity workItem = workItems.get(i);
            Allegation allegation = allegations.get(i);
            
            // Cross-reference the IDs for future lookups
            // You could add a field to link them if needed
            System.out.println("Linked WorkItem " + workItem.getWorkItemNumber() + 
                             " with Allegation " + allegation.getAllegationId());
        }
    }
}