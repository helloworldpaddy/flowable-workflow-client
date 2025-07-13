package com.workflow.cmsflowable.controller;

import com.workflow.cmsflowable.dto.request.CreateCaseWithAllegationsRequest;
import com.workflow.cmsflowable.dto.response.CaseWithAllegationsResponse;
import com.workflow.cmsflowable.service.CaseWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/cases")
@Tag(name = "Case Management", description = "APIs for managing cases and workflow processes")
public class CaseManagementController {
    
    @Autowired
    private CaseWorkflowService caseWorkflowService;
    
    @Autowired
    private RuntimeService runtimeService;
    
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Case management controller is working!");
    }
    
    @PostMapping
    @Operation(summary = "Create a new case", description = "Create a new case with allegations and start the workflow process")
    public ResponseEntity<CaseWithAllegationsResponse> createCase(@Valid @RequestBody CreateCaseWithAllegationsRequest request) {
        System.out.println("🎯 Received case creation request for: " + request.getTitle());
        CaseWithAllegationsResponse response = caseWorkflowService.createCaseWithWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/multi-department")
    @Operation(summary = "Create a multi-department case", description = "Create a complex case that requires multiple departments (HR, Legal, CSIS)")
    public ResponseEntity<CaseWithAllegationsResponse> createMultiDepartmentCase(@Valid @RequestBody CreateCaseWithAllegationsRequest request) {
        System.out.println("🎯 Creating multi-department case: " + request.getTitle());
        
        // Ensure the case will trigger multi-department workflow
        if (request.getAllegations() == null || request.getAllegations().size() < 2) {
            throw new IllegalArgumentException("Multi-department cases require at least 2 allegations with different classifications");
        }
        
        CaseWithAllegationsResponse response = caseWorkflowService.createCaseWithWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{caseNumber}")
    @Operation(summary = "Get case details", description = "Retrieve detailed information about a specific case")
    public ResponseEntity<CaseWithAllegationsResponse> getCaseDetails(
            @Parameter(description = "Case number (e.g., CMS-2025-010)") @PathVariable String caseNumber) {
        try {
            CaseWithAllegationsResponse caseDetails = caseWorkflowService.getCaseDetailsByCaseNumber(caseNumber);
            return ResponseEntity.ok(caseDetails);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping
    @Operation(summary = "Get all cases", description = "Retrieve a list of all cases in the system")
    public ResponseEntity<List<CaseWithAllegationsResponse>> getAllCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        try {
            List<CaseWithAllegationsResponse> cases = caseWorkflowService.getAllCases(page, size, status);
            return ResponseEntity.ok(cases);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{caseNumber}/workflow-status")
    @Operation(summary = "Get workflow status", description = "Get the current workflow status and progress for a case")
    public ResponseEntity<Map<String, Object>> getWorkflowStatus(
            @Parameter(description = "Case number") @PathVariable String caseNumber) {
        try {
            Map<String, Object> workflowStatus = new HashMap<>();
            
            // Find the process instance for this case
            List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
                .variableValueEquals("caseNumber", caseNumber)
                .active()
                .list();
            
            if (processInstances.isEmpty()) {
                // Check if process is completed
                long historicCount = runtimeService.createProcessInstanceQuery()
                    .variableValueEquals("caseNumber", caseNumber)
                    .count();
                
                if (historicCount > 0) {
                    workflowStatus.put("status", "COMPLETED");
                    workflowStatus.put("message", "Workflow has been completed");
                } else {
                    workflowStatus.put("status", "NOT_FOUND");
                    workflowStatus.put("message", "No workflow found for this case");
                }
            } else {
                ProcessInstance processInstance = processInstances.get(0);
                workflowStatus.put("status", "ACTIVE");
                workflowStatus.put("processInstanceId", processInstance.getId());
                workflowStatus.put("processDefinitionId", processInstance.getProcessDefinitionId());
                workflowStatus.put("startTime", processInstance.getStartTime());
                
                // Get current activity
                Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
                workflowStatus.put("variables", variables);
            }
            
            return ResponseEntity.ok(workflowStatus);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve workflow status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/{caseNumber}/journey")
    @Operation(summary = "Get workflow journey", description = "Track the complete journey of a case through the workflow")
    public ResponseEntity<Map<String, Object>> getWorkflowJourney(
            @Parameter(description = "Case number") @PathVariable String caseNumber) {
        try {
            Map<String, Object> journey = new HashMap<>();
            
            // This would ideally integrate with your case service to get journey details
            journey.put("caseNumber", caseNumber);
            journey.put("stages", List.of(
                Map.of("stage", "Intake", "status", "completed", "timestamp", "2025-07-12T23:42:38"),
                Map.of("stage", "Classification", "status", "completed", "timestamp", "2025-07-12T23:43:15"),
                Map.of("stage", "HR Review", "status", "in_progress", "timestamp", "2025-07-12T23:45:00"),
                Map.of("stage", "Legal Review", "status", "pending"),
                Map.of("stage", "CSIS Review", "status", "pending"),
                Map.of("stage", "Investigation", "status", "pending"),
                Map.of("stage", "Case Closure", "status", "pending")
            ));
            
            return ResponseEntity.ok(journey);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve workflow journey: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}