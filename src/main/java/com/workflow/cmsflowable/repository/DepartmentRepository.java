package com.workflow.cmsflowable.repository;

import com.workflow.cmsflowable.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByDepartmentId(Long departmentId);
    Optional<Department> findByDepartmentCode(String departmentCode);
    List<Department> findByIsActiveTrue();
    List<Department> findByManagerUserId(Long managerUserId);
    boolean existsByDepartmentCode(String departmentCode);
}