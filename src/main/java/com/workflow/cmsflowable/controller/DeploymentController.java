// src/main/java/com/workflow/cmsflowable/controller/DeploymentController.java
package com.workflow.cmsflowable.controller;

import com.workflow.cmsflowable.service.FlowableDeploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.workflow.cmsflowable.model.DeploymentResult;

@RestController
@RequestMapping("/v1/deploy")
@Tag(name = "Flowable Deployment Management", description = "APIs for deploying BPMN, DMN, CMMN, and Form definitions")
public class DeploymentController {

    @Autowired
    private FlowableDeploymentService deploymentService;

    @PostMapping("/all")
    @Operation(summary = "Deploy all Flowable definitions",
               description = "Scans and deploys all BPMN, DMN, CMMN, and Form definitions found in configured classpath directories.")
    public ResponseEntity<Map<String, Object>> deployAll() {
        try {
            Map<String, List<DeploymentResult>> results = deploymentService.deployAllFlowableResources();

            boolean overallSuccess = true;
            for (List<DeploymentResult> typeResults : results.values()) {
                for (DeploymentResult res : typeResults) {
                    if (!res.isSuccess()) {
                        overallSuccess = false;
                        break;
                    }
                }
                if (!overallSuccess) break;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", overallSuccess);
            response.put("message", overallSuccess ? "All identified resources deployed successfully" : "Some resources failed to deploy. Check individual results.");
            response.put("results", results); // Contains detailed results for each type and file

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "An unexpected error occurred during deployment: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Get deployment status",
               description = "Check the counts of currently deployed BPMN, DMN, CMMN, and Form definitions.")
    public ResponseEntity<Map<String, Object>> getDeploymentStatus() {
        try {
            Map<String, Object> status = deploymentService.getDeploymentStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to check deployment status: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}