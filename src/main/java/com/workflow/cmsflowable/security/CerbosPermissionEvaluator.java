package com.workflow.cmsflowable.security;

import com.workflow.cmsflowable.dto.request.CreateCaseWithAllegationsRequest;
import com.workflow.cmsflowable.entity.Case;
import com.workflow.cmsflowable.entity.Allegation;
import com.workflow.cmsflowable.repository.CaseRepository;
import com.workflow.cmsflowable.service.WorkflowStateService;
import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.builders.AttributeValue;
import dev.cerbos.sdk.builders.Principal;
import dev.cerbos.sdk.builders.Resource;
import dev.cerbos.sdk.CheckResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CerbosPermissionEvaluator implements PermissionEvaluator {
    
    @Autowired(required = false)
    private CerbosBlockingClient cerbosClient;
    
    @Autowired
    private WorkflowStateService workflowStateService;
    
    @Autowired
    private CaseRepository caseRepository;
    
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Authentication is null or not authenticated");
            return false;
        }
        
        try {
            List<String> roles = extractRoles(authentication);
            if (roles.isEmpty()) {
                return false; // extractRoles already logged the warning
            }
            
            // Use Cerbos if available, otherwise fallback to basic permission check
            boolean allowed;
            if (cerbosClient != null) {
                allowed = checkCerbosPermission(authentication, targetDomainObject, permission.toString(), roles);
            } else {
                log.debug("Cerbos client not available, using fallback permission logic");
                allowed = checkBasicPermission(roles, targetDomainObject, permission.toString());
            }
                
            log.debug("Permission check - User: {}, Action: {}, Resource: {}, Allowed: {}", 
                authentication.getName(), permission, targetDomainObject.getClass().getSimpleName(), allowed);
                
            return allowed;
            
        } catch (Exception e) {
            log.error("Error checking permission for user {} on action {}: {}", 
                authentication.getName(), permission, e.getMessage());
            return false; // Fail secure
        }
    }
    
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Authentication is null or not authenticated");
            return false;
        }
        
        try {
            List<String> roles = extractRoles(authentication);
            if (roles.isEmpty()) {
                return false; // extractRoles already logged the warning
            }
            
            // TODO: Implement actual Cerbos permission check when SDK API is stable
            // For now, implement basic role-based authorization
            boolean allowed = checkBasicPermissionById(roles, targetId.toString(), targetType, permission.toString());
                
            log.debug("Permission check - User: {}, Action: {}, Resource: {} ({}), Allowed: {}", 
                authentication.getName(), permission, targetType, targetId, allowed);
                
            return allowed;
            
        } catch (Exception e) {
            log.error("Error checking permission for user {} on {} {}: {}", 
                authentication.getName(), targetType, targetId, e.getMessage());
            return false; // Fail secure
        }
    }
    
    /**
     * Extract roles from authentication, handling type safety and role prefix removal
     */
    private List<String> extractRoles(Authentication authentication) {
        try {
            // Type-safe principal extraction
            if (!(authentication.getPrincipal() instanceof UserPrincipal)) {
                log.warn("Authentication principal is not a UserPrincipal: {}", 
                    authentication.getPrincipal().getClass().getSimpleName());
                return Collections.emptyList(); // Return empty list instead of null
            }
            
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            
            // Get roles without ROLE_ prefix for Cerbos
            return userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Error extracting roles from authentication: {}", e.getMessage());
            return Collections.emptyList(); // Return empty list on error
        }
    }
    
    /**
     * Basic permission check implementation until Cerbos SDK is properly configured
     */
    private boolean checkBasicPermission(List<String> roles, Object targetDomainObject, String permission) {
        // Admin users have all permissions
        if (roles.contains("ADMIN") || roles.contains("SYSTEM_ADMIN")) {
            return true;
        }
        
        // Case-specific permissions
        if (targetDomainObject instanceof CreateCaseWithAllegationsRequest || 
            targetDomainObject instanceof Case) {
            
            switch (permission.toLowerCase()) {
                case "create":
                    return roles.contains("INTAKE_ANALYST") || 
                           roles.contains("INTAKE_ANALYST_GROUP") || 
                           roles.contains("INVESTIGATOR") ||
                           roles.contains("INVESTIGATOR_GROUP") ||
                           roles.contains("HR_SPECIALIST") ||
                           roles.contains("HR_GROUP") ||
                           roles.contains("ADMIN");
                           
                case "read":
                case "view":
                    return roles.contains("INTAKE_ANALYST") || 
                           roles.contains("INTAKE_ANALYST_GROUP") || 
                           roles.contains("INVESTIGATOR") ||
                           roles.contains("INVESTIGATOR_GROUP") ||
                           roles.contains("HR_SPECIALIST") ||
                           roles.contains("HR_GROUP") ||
                           roles.contains("LEGAL_COUNSEL") ||
                           roles.contains("LEGAL_GROUP") ||
                           roles.contains("SECURITY_ANALYST") ||
                           roles.contains("CSIS_GROUP") ||
                           roles.contains("DIRECTOR") ||
                           roles.contains("DIRECTOR_GROUP") ||
                           roles.contains("ADMIN");
                           
                case "update":
                case "edit":
                    return roles.contains("INTAKE_ANALYST_GROUP") || 
                           roles.contains("INVESTIGATOR_GROUP") ||
                           roles.contains("HR_GROUP") ||
                           roles.contains("DIRECTOR_GROUP");
                           
                case "delete":
                    return roles.contains("DIRECTOR_GROUP") ||
                           roles.contains("SYSTEM_ADMIN");
                           
                case "approve":
                    return roles.contains("DIRECTOR_GROUP") ||
                           roles.contains("HR_GROUP");
                           
                case "close":
                    return roles.contains("DIRECTOR_GROUP") ||
                           roles.contains("INVESTIGATOR_GROUP");
                           
                default:
                    return false;
            }
        }
        
        return false;
    }
    
    /**
     * Basic permission check by ID implementation until Cerbos SDK is properly configured
     */
    private boolean checkBasicPermissionById(List<String> roles, String targetId, String targetType, String permission) {
        // Admin users have all permissions
        if (roles.contains("ADMIN") || roles.contains("SYSTEM_ADMIN")) {
            return true;
        }
        
        if ("case".equals(targetType)) {
            return checkBasicPermission(roles, targetId, permission);
        }
        
        return false;
    }
    
    /**
     * Check permissions using Cerbos authorization service
     */
    private boolean checkCerbosPermission(Authentication authentication, Object targetDomainObject, String action, List<String> roles) {
        try {
            // Build Principal (user)
            Principal principal = Principal.newInstance(authentication.getName())
                .withRoles(roles.toArray(new String[0]));
            
            // Add user attributes
            if (authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                principal = principal.withAttribute("email", AttributeValue.stringValue(userPrincipal.getEmail()))
                    .withAttribute("userId", AttributeValue.stringValue(String.valueOf(userPrincipal.getUserId())));
            }
            
            // Build Resource based on target object type
            Resource resource = buildCerbosResource(targetDomainObject);
            
            // Check permission
            CheckResult result = cerbosClient.check(principal, resource, action);
            boolean allowed = result.isAllowed(action);
            
            log.debug("Cerbos permission check - User: {}, Action: {}, Resource: {}, Allowed: {}", 
                authentication.getName(), action, "resource", allowed);
                
            return allowed;
            
        } catch (Exception e) {
            log.error("Error checking Cerbos permission for user {} on action {}: {}", 
                authentication.getName(), action, e.getMessage());
            // Fallback to basic permission check on Cerbos error
            return checkBasicPermission(roles, targetDomainObject, action);
        }
    }
    
    /**
     * Build Cerbos Resource from domain object
     */
    private Resource buildCerbosResource(Object targetDomainObject) {
        if (targetDomainObject instanceof Case) {
            Case caseObj = (Case) targetDomainObject;
            return Resource.newInstance("case", caseObj.getCaseId())
                .withAttribute("status", AttributeValue.stringValue(caseObj.getStatus().toString()))
                .withAttribute("priority", AttributeValue.stringValue(caseObj.getPriority().toString()));
                
        } else if (targetDomainObject instanceof Allegation) {
            Allegation allegation = (Allegation) targetDomainObject;
            return Resource.newInstance("allegation", allegation.getAllegationId())
                .withAttribute("classification", AttributeValue.stringValue(allegation.getDepartmentClassification()))
                .withAttribute("severity", AttributeValue.stringValue(allegation.getSeverity().toString()))
                .withAttribute("type", AttributeValue.stringValue(allegation.getAllegationType()));
                
        } else if (targetDomainObject instanceof CreateCaseWithAllegationsRequest) {
            CreateCaseWithAllegationsRequest request = (CreateCaseWithAllegationsRequest) targetDomainObject;
            return Resource.newInstance("case", "new-case")
                .withAttribute("priority", AttributeValue.stringValue(request.getPriority().toString()));
                
        } else {
            // Default resource for unknown types
            return Resource.newInstance("generic", targetDomainObject.getClass().getSimpleName());
        }
    }
    
    /**
     * Build Cerbos Resource from workflow task attributes
     */
    private Resource buildWorkflowTaskResource(String taskId, Map<String, Object> taskAttributes) {
        Resource resource = Resource.newInstance("workflow_task", taskId);
        
        if (taskAttributes != null) {
            for (Map.Entry<String, Object> entry : taskAttributes.entrySet()) {
                if (entry.getValue() != null) {
                    resource = resource.withAttribute(entry.getKey(), AttributeValue.stringValue(entry.getValue().toString()));
                }
            }
        }
        
        return resource;
    }
}