package com.workflow.cmsflowable.repository;

import com.workflow.cmsflowable.entity.EscalationMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EscalationMethodRepository extends JpaRepository<EscalationMethod, Long> {
    
    Optional<EscalationMethod> findByMethodCode(String methodCode);
    
    @Query("SELECT em FROM EscalationMethod em WHERE em.isActive = true ORDER BY em.methodName")
    List<EscalationMethod> findAllActive();
    
    List<EscalationMethod> findByIsActiveTrueOrderByMethodName();
}