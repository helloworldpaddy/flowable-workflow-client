package com.workflow.cmsflowable.controller;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.dmn.api.DmnRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/deploy")
public class DeploymentController {

    @Autowired
    private RepositoryService repositoryService;
    
    @Autowired
    private DmnRepositoryService dmnRepositoryService;

    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> deployAll() {
        try {
            // Deploy BPMN and DMN files
            Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("processes/Process_CMS_Workflow_Updated.bpmn20.xml")
                .addClasspathResource("dmn/allegation-classification.dmn")
                .name("CMS Workflow Deployment")
                .deploy();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deploymentId", deployment.getId());
            response.put("deploymentName", deployment.getName());
            response.put("message", "Successfully deployed BPMN and DMN files");

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDeploymentStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            
            // Check if process definition exists
            long processCount = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("Process_CMS_Workflow_Updated")
                .count();
            
            // Check if DMN decision exists  
            long dmnCount = dmnRepositoryService.createDecisionQuery()
                .decisionKey("allegation-classification")
                .count();
            
            status.put("processDefinitionCount", processCount);
            status.put("dmnDecisionCount", dmnCount);
            status.put("processDeployed", processCount > 0);
            status.put("dmnDeployed", dmnCount > 0);
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}