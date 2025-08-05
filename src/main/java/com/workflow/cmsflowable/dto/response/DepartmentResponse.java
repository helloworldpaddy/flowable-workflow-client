package com.workflow.cmsflowable.dto.response;

import com.workflow.cmsflowable.entity.Department;

import java.time.LocalDateTime;

public class DepartmentResponse {
    private Long id;
    private Long departmentId;
    private String departmentCode;
    private String departmentName;
    private String departmentDescription;
    private UserSummaryResponse manager;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String departmentRegion;
    private String departmentFunction;

    public DepartmentResponse() {}

    public DepartmentResponse(Department department) {
        this.id = department.getId();
        this.departmentId = department.getDepartmentId();
        this.departmentCode = department.getDepartmentCode();
        this.departmentName = department.getDepartmentName();
        this.departmentDescription = department.getDepartmentDescription();
        if (department.getManager() != null) {
            this.manager = new UserSummaryResponse(department.getManager());
        }
        this.isActive = department.getIsActive();
        this.createdAt = department.getCreatedAt();
        this.updatedAt = department.getUpdatedAt();
        this.departmentRegion = department.getDepartmentRegion();
        this.departmentFunction = department.getDepartmentFunction();
    }

    public static class UserSummaryResponse {
        private Long userId;
        private String username;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;

        public UserSummaryResponse() {}

        public UserSummaryResponse(com.workflow.cmsflowable.entity.User user) {
            this.userId = user.getUserId();
            this.username = user.getUsername();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.fullName = user.getFullName();
            this.email = user.getEmail();
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDepartmentDescription() { return departmentDescription; }
    public void setDepartmentDescription(String departmentDescription) { this.departmentDescription = departmentDescription; }

    public UserSummaryResponse getManager() { return manager; }
    public void setManager(UserSummaryResponse manager) { this.manager = manager; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getDepartmentRegion() { return departmentRegion; }
    public void setDepartmentRegion(String departmentRegion) { this.departmentRegion = departmentRegion; }

    public String getDepartmentFunction() { return departmentFunction; }
    public void setDepartmentFunction(String departmentFunction) { this.departmentFunction = departmentFunction; }
}