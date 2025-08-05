package com.workflow.cmsflowable.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseNarrativeResponse {
    private Long id;
    private String narrativeId;
    private String caseId;
    private String investigationFunction;
    private String narrativeType;
    private String narrativeTitle;
    private String narrativeText;
    private Boolean isRecalled;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}