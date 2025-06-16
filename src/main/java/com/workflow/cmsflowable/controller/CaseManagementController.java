package com.workflow.cmsflowable.controller;

import com.workflow.cmsflowable.dto.request.CreateCaseWithAllegationsRequest;
import com.workflow.cmsflowable.dto.response.CaseWithAllegationsResponse;
import com.workflow.cmsflowable.service.CaseWorkflowService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/cases")
public class CaseManagementController {
    
    @Autowired
    private CaseWorkflowService caseWorkflowService;
    
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Case management controller is working!");
    }
    
    @PostMapping
    public ResponseEntity<CaseWithAllegationsResponse> createCase(@Valid @RequestBody CreateCaseWithAllegationsRequest request) {
        System.out.println("🎯 Received case creation request for: " + request.getTitle());
        CaseWithAllegationsResponse response = caseWorkflowService.createCaseWithWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}