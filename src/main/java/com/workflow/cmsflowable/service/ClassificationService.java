package com.workflow.cmsflowable.service;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClassificationService implements JavaDelegate {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationService.class);

    @Override
    public void execute(DelegateExecution execution) {
        logger.info("Executing classification logic for case: {}", execution.getVariable("caseId"));
        
        try {
            // Get allegations from process variables
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> allegations = (List<Map<String, Object>>) execution.getVariable("allegations");
            
            if (allegations == null || allegations.isEmpty()) {
                logger.warn("No allegations found for classification");
                execution.setVariable("classification", new ArrayList<>());
                return;
            }
            
            List<Map<String, Object>> classification = new ArrayList<>();
            
            for (Map<String, Object> allegation : allegations) {
                String allegationType = (String) allegation.get("allegationType");
                String severity = (String) allegation.get("severity");
                
                Map<String, Object> classificationResult = new HashMap<>();
                classificationResult.put("allegationType", allegationType);
                classificationResult.put("severity", severity);
                classificationResult.put("classification", classifyAllegationType(allegationType));
                
                classification.add(classificationResult);
                
                logger.debug("Classified allegation '{}' as '{}'", allegationType, classificationResult.get("classification"));
            }
            
            execution.setVariable("classification", classification);
            logger.info("Classification completed for {} allegations", classification.size());
            
        } catch (Exception e) {
            logger.error("Error during classification", e);
            throw new RuntimeException("Classification failed: " + e.getMessage(), e);
        }
    }
    
    private String classifyAllegationType(String allegationType) {
        if (allegationType == null) {
            return "GENERAL";
        }
        
        switch (allegationType.toLowerCase()) {
            case "sexual harassment":
            case "harassment":
            case "discrimination":
            case "retaliation":
                return "HR";
            case "financial fraud":
            case "financial_fraud":
            case "embezzlement":
            case "theft":
            case "corruption":
                return "LEGAL";
            case "data breach":
            case "data_breach":
            case "security violation":
            case "security_violation":
            case "unauthorized access":
                return "CSIS";
            case "policy violation":
            case "misconduct":
                return "COMPLIANCE";
            default:
                return "GENERAL";
        }
    }
}