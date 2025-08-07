package com.workflow.cmsflowable.controller;

import com.workflow.cmsflowable.dto.request.DeployWorkflowRequest;
import com.workflow.cmsflowable.dto.request.RegisterWorkflowMetadataRequest;
import com.workflow.cmsflowable.dto.response.WorkflowMetadataResponse;
import com.workflow.cmsflowable.service.WorkflowMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/workflow-metadata")
@Tag(name = "Workflow Metadata Management", description = "APIs for managing workflow metadata and deployments")
@RequiredArgsConstructor
public class WorkflowMetadataController {
    
    private final WorkflowMetadataService workflowMetadataService;
    
    @PostMapping("/register")
    @Operation(summary = "Register workflow metadata", description = "Register workflow metadata with candidate group to queue mappings")
    public ResponseEntity<WorkflowMetadataResponse> registerWorkflowMetadata(
            @Valid @RequestBody RegisterWorkflowMetadataRequest request) {
        try {
            WorkflowMetadataResponse response = workflowMetadataService.registerWorkflowMetadata(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/deploy")
    @Operation(summary = "Deploy workflow", description = "Deploy BPMN workflow to Flowable engine and build task mappings")
    public ResponseEntity<WorkflowMetadataResponse> deployWorkflow(
            @Valid @RequestBody DeployWorkflowRequest request) {
        try {
            WorkflowMetadataResponse response = workflowMetadataService.deployWorkflow(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{processDefinitionKey}")
    @Operation(summary = "Get workflow metadata", description = "Get workflow metadata by process definition key")
    public ResponseEntity<WorkflowMetadataResponse> getWorkflowMetadata(
            @Parameter(description = "Process definition key") @PathVariable String processDefinitionKey) {
        try {
            WorkflowMetadataResponse response = workflowMetadataService.getWorkflowMetadata(processDefinitionKey);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get all active workflows", description = "Get all active workflow metadata")
    public ResponseEntity<List<WorkflowMetadataResponse>> getAllActiveWorkflows() {
        try {
            List<WorkflowMetadataResponse> workflows = workflowMetadataService.getAllActiveWorkflows();
            return ResponseEntity.ok(workflows);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/deployed")
    @Operation(summary = "Get all deployed workflows", description = "Get all deployed workflow metadata")
    public ResponseEntity<List<WorkflowMetadataResponse>> getAllDeployedWorkflows() {
        try {
            List<WorkflowMetadataResponse> workflows = workflowMetadataService.getAllDeployedWorkflows();
            return ResponseEntity.ok(workflows);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{processDefinitionKey}")
    @Operation(summary = "Update workflow metadata", description = "Update workflow metadata for a specific process")
    public ResponseEntity<WorkflowMetadataResponse> updateWorkflowMetadata(
            @Parameter(description = "Process definition key") @PathVariable String processDefinitionKey,
            @Valid @RequestBody RegisterWorkflowMetadataRequest request) {
        try {
            WorkflowMetadataResponse response = workflowMetadataService.updateWorkflowMetadata(processDefinitionKey, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{processDefinitionKey}")
    @Operation(summary = "Delete workflow metadata", description = "Mark workflow metadata as inactive")
    public ResponseEntity<String> deleteWorkflowMetadata(
            @Parameter(description = "Process definition key") @PathVariable String processDefinitionKey) {
        try {
            workflowMetadataService.deleteWorkflowMetadata(processDefinitionKey);
            return ResponseEntity.ok("Workflow metadata marked as inactive successfully");
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}