package com.workflow.cmsflowable.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases", schema = "cms_flowable_workflow")
public class Case {
    
    @Id
    @Column(name = "case_id")
    private String caseId;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "case_number", unique = true)
    private String caseNumber;
    
    @NotBlank
    @Size(max = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_type_id", referencedColumnName = "case_type_id")
    private CaseType caseType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", referencedColumnName = "department_id")
    private Department department;
    
    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;
    
    @Enumerated(EnumType.STRING)
    private CaseStatus status = CaseStatus.OPEN;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id", referencedColumnName = "user_id")
    private User assignedTo;
    
    @Size(max = 200)
    @Column(name = "complainant_name")
    private String complainantName;
    
    @Size(max = 255)
    @Column(name = "complainant_email")
    private String complainantEmail;
    
    @Column(name = "workflow_instance_key")
    private Long workflowInstanceKey;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", referencedColumnName = "user_id")
    private User createdBy;
    
    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkItem> workItems = new ArrayList<>();
    
    /* 
    @OneToMany(mappedBy = "case", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CaseTransition> transitions = new ArrayList<>();
    */

    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CaseTransition> transitions = new ArrayList<>();

    @OneToMany(mappedBy = "caseId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Allegation> allegations = new ArrayList<>();

    // Additional case fields from frontend requirements
    @Column(name = "occurrence_date")
    private LocalDate occurrenceDate;

    @Column(name = "date_reported_to_citi")
    private LocalDate dateReportedToCiti;

    @Column(name = "date_received_by_escalation_channel")
    private LocalDate dateReceivedByEscalationChannel;

    @Column(name = "complaint_escalated_by", length = 50)
    private String complaintEscalatedBy;

    @Column(name = "data_source_id", length = 50)
    private String dataSourceId;

    @Column(name = "cluster_country", length = 50)
    private String clusterCountry;

    @Column(name = "legal_hold")
    private Boolean legalHold = false;

    @Column(name = "outside_counsel")
    private Boolean outsideCounsel = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intake_analyst_id", referencedColumnName = "user_id")
    private User intakeAnalyst;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investigation_manager_id", referencedColumnName = "user_id")
    private User investigationManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investigator_id", referencedColumnName = "user_id")
    private User investigator;

    // One-to-many relationships with new entities
    @OneToMany(mappedBy = "caseId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CaseEntity> entities = new ArrayList<>();

    @OneToMany(mappedBy = "caseId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CaseNarrative> narratives = new ArrayList<>();

    // Constructors
    public Case() {}
    
    public Case(String caseNumber, String title, String description, CaseType caseType) {
        this.caseNumber = caseNumber;
        this.title = title;
        this.description = description;
        this.caseType = caseType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Lifecycle Callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    
    public String getCaseNumber() { return caseNumber; }
    public void setCaseNumber(String caseNumber) { this.caseNumber = caseNumber; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public CaseType getCaseType() { return caseType; }
    public void setCaseType(CaseType caseType) { this.caseType = caseType; }
    
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    
    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }
    
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    
    public String getComplainantName() { return complainantName; }
    public void setComplainantName(String complainantName) { this.complainantName = complainantName; }
    
    public String getComplainantEmail() { return complainantEmail; }
    public void setComplainantEmail(String complainantEmail) { this.complainantEmail = complainantEmail; }
    
    public Long getWorkflowInstanceKey() { return workflowInstanceKey; }
    public void setWorkflowInstanceKey(Long workflowInstanceKey) { this.workflowInstanceKey = workflowInstanceKey; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public List<WorkItem> getWorkItems() { return workItems; }
    public void setWorkItems(List<WorkItem> workItems) { this.workItems = workItems; }
    
    public List<Allegation> getAllegations() { return allegations; }
    public void setAllegations(List<Allegation> allegations) { this.allegations = allegations; }

    // Helper method to add transition
    public void addTransition(CaseTransition transition) {
        transitions.add(transition);
        transition.setCaseEntity(this);
    }
    
    // Helper method to add allegation
    public void addAllegation(Allegation allegation) {
        allegations.add(allegation);
        allegation.setCaseId(this.caseId);
    }
    
    // Getters and Setters for new fields
    public LocalDate getOccurrenceDate() { return occurrenceDate; }
    public void setOccurrenceDate(LocalDate occurrenceDate) { this.occurrenceDate = occurrenceDate; }
    
    public LocalDate getDateReportedToCiti() { return dateReportedToCiti; }
    public void setDateReportedToCiti(LocalDate dateReportedToCiti) { this.dateReportedToCiti = dateReportedToCiti; }
    
    public LocalDate getDateReceivedByEscalationChannel() { return dateReceivedByEscalationChannel; }
    public void setDateReceivedByEscalationChannel(LocalDate dateReceivedByEscalationChannel) { this.dateReceivedByEscalationChannel = dateReceivedByEscalationChannel; }
    
    public String getComplaintEscalatedBy() { return complaintEscalatedBy; }
    public void setComplaintEscalatedBy(String complaintEscalatedBy) { this.complaintEscalatedBy = complaintEscalatedBy; }
    
    public String getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(String dataSourceId) { this.dataSourceId = dataSourceId; }
    
    public String getClusterCountry() { return clusterCountry; }
    public void setClusterCountry(String clusterCountry) { this.clusterCountry = clusterCountry; }
    
    public Boolean getLegalHold() { return legalHold; }
    public void setLegalHold(Boolean legalHold) { this.legalHold = legalHold; }
    
    public Boolean getOutsideCounsel() { return outsideCounsel; }
    public void setOutsideCounsel(Boolean outsideCounsel) { this.outsideCounsel = outsideCounsel; }
    
    public User getIntakeAnalyst() { return intakeAnalyst; }
    public void setIntakeAnalyst(User intakeAnalyst) { this.intakeAnalyst = intakeAnalyst; }
    
    public User getInvestigationManager() { return investigationManager; }
    public void setInvestigationManager(User investigationManager) { this.investigationManager = investigationManager; }
    
    public User getInvestigator() { return investigator; }
    public void setInvestigator(User investigator) { this.investigator = investigator; }
    
    public List<CaseEntity> getEntities() { return entities; }
    public void setEntities(List<CaseEntity> entities) { this.entities = entities; }
    
    public List<CaseNarrative> getNarratives() { return narratives; }
    public void setNarratives(List<CaseNarrative> narratives) { this.narratives = narratives; }
    
    // Helper methods for new entities
    public void addEntity(CaseEntity entity) {
        entities.add(entity);
        entity.setCaseId(this.caseId);
    }
    
    public void addNarrative(CaseNarrative narrative) {
        narratives.add(narrative);
        narrative.setCaseId(this.caseId);
    }
}


