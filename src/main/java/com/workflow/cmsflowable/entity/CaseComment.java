package com.workflow.cmsflowable.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_comments", schema = "cms_flowable_workflow")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false, unique = true, length = 50)
    private String commentId;

    @Column(name = "case_id", nullable = false, length = 50)
    private String caseId;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @NotBlank
    @Size(max = 4000)
    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", length = 50)
    private CommentType commentType = CommentType.GENERAL;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "is_internal")
    private Boolean isInternal = false;

    @Column(name = "is_confidential")
    private Boolean isConfidential = false;

    @Column(name = "character_count")
    private Integer characterCount = 0;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", referencedColumnName = "case_id", insertable = false, updatable = false)
    private Case caseEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", insertable = false, updatable = false)
    private User updatedByUser;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        characterCount = commentText != null ? commentText.length() : 0;
        if (commentId == null) {
            commentId = "CMT-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        characterCount = commentText != null ? commentText.length() : 0;
    }

    public enum CommentType {
        GENERAL, INVESTIGATION, LEGAL, HR, ESCALATION, RESOLUTION, CLOSURE
    }

    // Utility methods
    public boolean isEdited() {
        return updatedAt != null && !updatedAt.equals(createdAt);
    }

    public String getAuthorDisplayName() {
        if (createdByUser != null) {
            return createdByUser.getFullName() + " (" + createdByUser.getUserId() + ")";
        }
        return "Unknown User";
    }
}