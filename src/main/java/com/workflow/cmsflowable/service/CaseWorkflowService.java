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
    
    private static final String CASE_WORKFLOW_PROCESS_KEY = "Process_CMS_Workflow";
    
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
        
        // Create work items (allegations) in work_items table
        List<WorkItemEntity> workItems = new ArrayList<>();
        int allegationCounter = 1;
        
        for (CreateCaseWithAllegationsRequest.AllegationRequest allegationReq : request.getAllegations()) {
            String workItemId = java.util.UUID.randomUUID().toString();
            String workItemNumber = caseNumber + "-WI-" + String.format("%02d", allegationCounter);
            
            WorkItemEntity workItem = new WorkItemEntity();
            workItem.setWorkItemId(workItemId);
            workItem.setWorkItemNumber(workItemNumber);
            workItem.setCaseId(savedCase.getCaseId());
            workItem.setType(allegationReq.getAllegationType());
            workItem.setSeverity(allegationReq.getSeverity().toString());
            workItem.setDescription(allegationReq.getDescription());
            workItem.setStatus("OPEN");
            workItem.setPriority(request.getPriority().toString());
            
            allegationCounter++;
            
            // Classify based on allegation type
            String classification = classifyAllegation(allegationReq.getAllegationType());
            workItem.setClassification(classification);
            workItem.setAssignedGroup(getAssignedGroup(classification));
            
            workItems.add(workItem);
            System.out.println("Created work item: " + workItemNumber + " for " + allegationReq.getAllegationType());
        }
        
        // Save work items to database
        System.out.println("Attempting to save " + workItems.size() + " work items to database");
        for (WorkItemEntity workItem : workItems) {
            System.out.println("Work item to save: " + workItem.getWorkItemNumber() + " (ID: " + workItem.getWorkItemId() + ")");
        }
        
        List<WorkItemEntity> savedWorkItems;
        try {
            savedWorkItems = workItemRepository.saveAll(workItems);
            System.out.println("Successfully saved " + savedWorkItems.size() + " work items to database");
            for (WorkItemEntity savedItem : savedWorkItems) {
                System.out.println("Saved work item: " + savedItem.getWorkItemNumber() + " (ID: " + savedItem.getWorkItemId() + ")");
            }
        } catch (Exception e) {
            System.err.println("Error saving work items: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save work items: " + e.getMessage(), e);
        }
        
        // Prepare workflow variables
        Map<String, Object> workflowVariables = new HashMap<>();
        workflowVariables.put("caseId", savedCase.getCaseNumber());
        workflowVariables.put("caseTitle", savedCase.getTitle());
        workflowVariables.put("priority", savedCase.getPriority().toString());
        workflowVariables.put("complainantName", savedCase.getComplainantName());
        workflowVariables.put("complainantEmail", savedCase.getComplainantEmail());
        workflowVariables.put("workItemCount", savedWorkItems.size());
        
        // Prepare allegations data for DMN decision
        if (!savedWorkItems.isEmpty()) {
            WorkItemEntity primaryWorkItem = savedWorkItems.get(0);
            workflowVariables.put("allegationType", primaryWorkItem.getType());
            workflowVariables.put("severity", primaryWorkItem.getSeverity());
            workflowVariables.put("classification", primaryWorkItem.getClassification());
        }
        
        // Create allegations list for multi-instance workflow
        List<Map<String, Object>> allegationsForWorkflow = new ArrayList<>();
        for (WorkItemEntity workItem : savedWorkItems) {
            Map<String, Object> workItemMap = new HashMap<>();
            workItemMap.put("workItemId", workItem.getWorkItemId());
            workItemMap.put("workItemNumber", workItem.getWorkItemNumber());
            workItemMap.put("description", workItem.getDescription());
            workItemMap.put("allegationType", workItem.getType());
            workItemMap.put("severity", workItem.getSeverity());
            workItemMap.put("classification", workItem.getClassification());
            workItemMap.put("assignedGroup", workItem.getAssignedGroup());
            workItemMap.put("priority", workItem.getPriority());
            workItemMap.put("status", workItem.getStatus());
            workItemMap.put("caseId", workItem.getCaseId());
            allegationsForWorkflow.add(workItemMap);
        }
        workflowVariables.put("allegations", allegationsForWorkflow);
        
        System.out.println("Prepared " + allegationsForWorkflow.size() + " work items for multi-instance processing:");
        
        // Determine which departments are needed based on classifications
        boolean hrNeeded = savedWorkItems.stream().anyMatch(w -> "HR".equals(w.getClassification()));
        boolean legalNeeded = savedWorkItems.stream().anyMatch(w -> "LEGAL".equals(w.getClassification()));
        boolean csisNeeded = savedWorkItems.stream().anyMatch(w -> "CSIS".equals(w.getClassification()));
        
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
            
            System.out.println("Updated case and work items with process instance ID");
            
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
        
        // Convert work items back to allegations for response
        List<Allegation> allegationsForResponse = convertWorkItemsToAllegations(savedWorkItems, savedCase.getCaseNumber());
        
        System.out.println("CASE CREATION AND WORKFLOW COMPLETED SUCCESSFULLY!");
        return convertToCaseWithAllegationsResponse(savedCase, allegationsForResponse);
    }
    
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
    
    private String generateCaseNumber() {
        String prefix = "CMS-" + java.time.Year.now() + "-";
        Long count = caseRepository.count() + 1;
        return prefix + String.format("%03d", count);
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
}