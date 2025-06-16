package com.workflow.cmsflowable.repository;

import com.workflow.cmsflowable.entity.CaseTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseTransitionRepository extends JpaRepository<CaseTransition, Long> {
    List<CaseTransition> findByCaseEntityCaseIdOrderByTransitionDateDesc(String caseId);
    List<CaseTransition> findByPerformedByUserId(Long userId);
}