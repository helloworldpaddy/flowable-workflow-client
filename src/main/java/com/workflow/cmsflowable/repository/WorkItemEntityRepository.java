package com.workflow.cmsflowable.repository;

import com.workflow.cmsflowable.entity.WorkItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkItemEntityRepository extends JpaRepository<WorkItemEntity, String> {
    List<WorkItemEntity> findByFlowableProcessInstanceId(String processInstanceId);
    List<WorkItemEntity> findByType(String type);
    List<WorkItemEntity> findByStatus(String status);
    List<WorkItemEntity> findByAssignedGroup(String assignedGroup);
}