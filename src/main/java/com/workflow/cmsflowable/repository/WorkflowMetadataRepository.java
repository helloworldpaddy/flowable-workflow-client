package com.workflow.cmsflowable.repository;

import com.workflow.cmsflowable.entity.WorkflowMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowMetadataRepository extends JpaRepository<WorkflowMetadata, Long> {
    
    // Primary lookup by process definition key
    Optional<WorkflowMetadata> findByProcessDefinitionKey(String processDefinitionKey);
    
    boolean existsByProcessDefinitionKey(String processDefinitionKey);
    
    // Active workflows
    List<WorkflowMetadata> findByActiveTrue();
    
    List<WorkflowMetadata> findByActiveTrueOrderByProcessNameAsc();
    
    // Deployed workflows
    List<WorkflowMetadata> findByDeployedTrue();
    
    List<WorkflowMetadata> findByActiveTrueAndDeployedTrue();
    
    // Version queries
    List<WorkflowMetadata> findByProcessDefinitionKeyOrderByVersionDesc(String processDefinitionKey);
    
    @Query("SELECT wm FROM WorkflowMetadata wm WHERE wm.processDefinitionKey = :processDefinitionKey AND wm.version = (SELECT MAX(wm2.version) FROM WorkflowMetadata wm2 WHERE wm2.processDefinitionKey = :processDefinitionKey)")
    Optional<WorkflowMetadata> findLatestVersionByProcessDefinitionKey(@Param("processDefinitionKey") String processDefinitionKey);
    
    // Deployment queries
    Optional<WorkflowMetadata> findByDeploymentId(String deploymentId);
    
    List<WorkflowMetadata> findByDeploymentIdIsNotNull();
    
    // Created by queries
    List<WorkflowMetadata> findByCreatedByOrderByCreatedAtDesc(String createdBy);
    
    // Search queries
    @Query("SELECT wm FROM WorkflowMetadata wm WHERE LOWER(wm.processName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(wm.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<WorkflowMetadata> searchByProcessNameOrDescription(@Param("searchTerm") String searchTerm);
    
    // Custom queries for queue management
    @Query("SELECT wm FROM WorkflowMetadata wm WHERE wm.active = true AND JSON_EXTRACT(wm.candidateGroupMappings, '$.\"' || :candidateGroup || '\"') IS NOT NULL")
    List<WorkflowMetadata> findByCandidateGroup(@Param("candidateGroup") String candidateGroup);
    
    // Statistics
    long countByActiveTrue();
    
    long countByDeployedTrue();
    
    long countByActiveTrueAndDeployedTrue();
}