package com.workflow.cmsflowable.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "allegations", schema = "cms_flowable_workflow")
public class Allegation {
    
    @Id
    @Column(name = "allegation_id")
    private String allegationId;
    
    @Column(name = "case_id")
    private String caseId;
    
    @Column(name = "allegation_type")
    private String allegationType;
    
    @Column(name = "severity")
    @Enumerated(EnumType.STRING)
    private Severity severity;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "department_classification")
    private String departmentClassification;
    
    @Column(name = "assigned_group")
    private String assignedGroup;
    
    @Column(name = "flowable_plan_item_id")
    private String flowablePlanItemId;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Allegation() {}
    
    public Allegation(String allegationId, String caseId, String allegationType, Severity severity, String description) {
        this.allegationId = allegationId;
        this.caseId = caseId;
        this.allegationType = allegationType;
        this.severity = severity;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Lifecycle callbacks
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getAllegationId() {
        return allegationId;
    }
    
    public void setAllegationId(String allegationId) {
        this.allegationId = allegationId;
    }
    
    public String getCaseId() {
        return caseId;
    }
    
    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }
    
    public String getAllegationType() {
        return allegationType;
    }
    
    public void setAllegationType(String allegationType) {
        this.allegationType = allegationType;
    }
    
    public Severity getSeverity() {
        return severity;
    }
    
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDepartmentClassification() {
        return departmentClassification;
    }
    
    public void setDepartmentClassification(String departmentClassification) {
        this.departmentClassification = departmentClassification;
    }
    
    public String getAssignedGroup() {
        return assignedGroup;
    }
    
    public void setAssignedGroup(String assignedGroup) {
        this.assignedGroup = assignedGroup;
    }
    
    public String getFlowablePlanItemId() {
        return flowablePlanItemId;
    }
    
    public void setFlowablePlanItemId(String flowablePlanItemId) {
        this.flowablePlanItemId = flowablePlanItemId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}