package com.workflow.cmsflowable.service;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.dmn.api.DmnDecisionService;
// import org.flowable.dmn.api.ExecuteDecisionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("determineDepartmentRoutingService")
public class DetermineDepartmentRoutingService implements JavaDelegate {

    @Autowired
    private DmnDecisionService dmnDecisionService;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Starting department routing determination for process: {}", execution.getProcessInstanceId());

        try {
            // Get allegations from process variables
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allegations = (List<Map<String, Object>>) execution.getVariable("allegations");
            
            if (allegations == null || allegations.isEmpty()) {
                log.warn("No allegations found in process variables, using default routing");
                setDefaultRouting(execution);
                return;
            }

            log.info("Processing {} allegations for routing determination", allegations.size());

            // Collect department classifications and priorities
            Set<String> relevantDepartments = new HashSet<>();
            String highestPriority = "LOW";
            
            // Process each allegation through DMN
            for (Map<String, Object> allegation : allegations) {
                String allegationType = (String) allegation.get("allegationType");
                String severity = (String) allegation.get("severity");
                
                log.debug("Processing allegation: type={}, severity={}", allegationType, severity);
                
                // Call DMN decision table for each allegation
                Map<String, Object> dmnResult = callAllegationClassificationDMN(allegationType, severity);
                
                if (dmnResult != null) {
                    String classification = (String) dmnResult.get("classification");
                    String assignedGroup = (String) dmnResult.get("assignedGroup");
                    String priority = (String) dmnResult.get("priority");
                    
                    log.debug("DMN result: classification={}, assignedGroup={}, priority={}", 
                             classification, assignedGroup, priority);
                    
                    // Add to relevant departments
                    if (classification != null) {
                        relevantDepartments.add(classification);
                    }
                    
                    // Update allegation with DMN results
                    allegation.put("classification", classification);
                    allegation.put("assignedGroup", assignedGroup);
                    allegation.put("priority", priority);
                    
                    // Track highest priority
                    if (isPriorityHigher(priority, highestPriority)) {
                        highestPriority = priority;
                    }
                } else {
                    log.warn("DMN returned null result for allegation: {}", allegationType);
                    // Set defaults
                    allegation.put("classification", "HR");
                    allegation.put("assignedGroup", "HR_GROUP");
                    allegation.put("priority", "MEDIUM");
                    relevantDepartments.add("HR");
                }
            }

            // Convert Set to List for process variables
            List<String> departmentList = new ArrayList<>(relevantDepartments);
            
            log.info("Department routing determined: departments={}, priority={}", departmentList, highestPriority);
            
            // Set process variables for routing logic
            execution.setVariable("relevantDepartments", departmentList);
            execution.setVariable("casePriority", highestPriority);
            execution.setVariable("allegations", allegations); // Update with DMN results
            
            // Set individual department flags for gateway conditions
            execution.setVariable("hrNeeded", departmentList.contains("HR"));
            execution.setVariable("legalNeeded", departmentList.contains("LEGAL"));
            execution.setVariable("csisNeeded", departmentList.contains("CSIS"));
            
            // Set case status for Cerbos resource attributes
            execution.setVariable("status", "DEPARTMENT_ROUTED");
            execution.setVariable("currentTaskGroup", "IU_ANALYST_GROUP");
            
            log.info("Process variables set successfully for department routing");

        } catch (Exception e) {
            log.error("Error in department routing determination", e);
            // Set default routing in case of error
            setDefaultRouting(execution);
        }
    }

    public Map<String, Object> callAllegationClassificationDMN(String allegationType, String severity) {
        try {
            System.out.println("=== DMN DEBUG START ===");
            System.out.println("Calling DMN with allegationType: '" + allegationType + "', severity: '" + severity + "'");
            log.debug("Calling DMN with allegationType: {}, severity: {}", allegationType, severity);
            
            // Build DMN execution request
            Map<String, Object> inputVariables = new HashMap<>();
            inputVariables.put("allegationType", allegationType);
            inputVariables.put("severity", severity != null ? severity.toUpperCase() : "MEDIUM");
            
            System.out.println("DMN Input Variables: " + inputVariables);
            
            // Execute DMN decision
            Map<String, Object> result = dmnDecisionService.createExecuteDecisionBuilder()
                .decisionKey("allegation-classification")  // This should match the DMN decision ID
                .variables(inputVariables)
                .executeWithSingleResult();
            
            System.out.println("DMN Raw Result: " + result);
            
            if (result != null) {
                System.out.println("DMN SUCCESS - Classification: " + result.get("classification") + 
                                 ", AssignedGroup: " + result.get("assignedGroup") + 
                                 ", Priority: " + result.get("priority"));
                log.debug("DMN decision result: {}", result);
                return result;
            } else {
                System.out.println("DMN RETURNED NULL - Using fallback for allegationType: '" + allegationType + "', severity: '" + severity + "'");
                log.warn("DMN decision returned no results for allegationType: {}, severity: {}", 
                        allegationType, severity);
                return null;
            }
            
        } catch (Exception e) {
            System.out.println("DMN ERROR: " + e.getMessage());
            e.printStackTrace();
            log.error("Error calling DMN decision for allegationType: {}, severity: {}", 
                     allegationType, severity, e);
            return null;
        } finally {
            System.out.println("=== DMN DEBUG END ===");
        }
    }

    private boolean isPriorityHigher(String priority1, String priority2) {
        Map<String, Integer> priorityLevels = Map.of(
            "LOW", 1,
            "MEDIUM", 2,
            "HIGH", 3,
            "CRITICAL", 4
        );
        
        int level1 = priorityLevels.getOrDefault(priority1, 2);
        int level2 = priorityLevels.getOrDefault(priority2, 2);
        
        return level1 > level2;
    }

    private void setDefaultRouting(DelegateExecution execution) {
        log.info("Setting default routing for process: {}", execution.getProcessInstanceId());
        
        // Default to HR department with medium priority
        execution.setVariable("relevantDepartments", List.of("HR"));
        execution.setVariable("casePriority", "MEDIUM");
        execution.setVariable("hrNeeded", true);
        execution.setVariable("legalNeeded", false);
        execution.setVariable("csisNeeded", false);
        execution.setVariable("status", "DEPARTMENT_ROUTED");
        execution.setVariable("currentTaskGroup", "HR_GROUP");
        
        log.info("Default routing set: HR department with MEDIUM priority");
    }
}