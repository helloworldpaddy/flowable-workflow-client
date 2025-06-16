package com.workflow.cmsflowable.repository;

import com.workflow.cmsflowable.entity.Allegation;
import com.workflow.cmsflowable.entity.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllegationRepository extends JpaRepository<Allegation, String> {
    
    List<Allegation> findByCaseId(String caseId);
    
    List<Allegation> findByAllegationType(String allegationType);
    
    List<Allegation> findBySeverity(Severity severity);
    
    List<Allegation> findByCaseIdAndSeverity(String caseId, Severity severity);
    
    List<Allegation> findByDepartmentClassification(String departmentClassification);
    
    @Query("SELECT a FROM Allegation a WHERE a.caseId = :caseId ORDER BY a.createdAt DESC")
    List<Allegation> findByCaseIdOrderByCreatedAtDesc(@Param("caseId") String caseId);
    
    @Query("SELECT a FROM Allegation a WHERE a.allegationType IN :types")
    List<Allegation> findByAllegationTypeIn(@Param("types") List<String> types);
    
    @Query("SELECT COUNT(a) FROM Allegation a WHERE a.caseId = :caseId")
    Long countByCaseId(@Param("caseId") String caseId);
}