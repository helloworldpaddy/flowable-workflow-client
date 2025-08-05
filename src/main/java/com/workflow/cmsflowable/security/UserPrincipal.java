package com.workflow.cmsflowable.security;

import com.workflow.cmsflowable.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class UserPrincipal implements UserDetails {
    
    private Long userId;
    private String username;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private List<String> departments;
    private boolean isManager;

    public UserPrincipal(Long userId, String username, String email, String password, 
                            Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.departments = new ArrayList<>();
        this.isManager = false;
    }

    public UserPrincipal(Long userId, String username, String email, String password, 
                            Collection<? extends GrantedAuthority> authorities,
                            List<String> departments, boolean isManager) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.departments = departments != null ? departments : new ArrayList<>();
        this.isManager = isManager;
    }

    public static UserPrincipal create(User user) {
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleCode())) // Remove ROLE_ prefix for Cerbos
                .collect(Collectors.toList());

        // Extract departments from roles or user entity
        List<String> departments = extractDepartments(user);
        
        // Check if user is a manager
        boolean isManager = user.getRoles().stream()
                .anyMatch(role -> role.getRoleCode().contains("MANAGER") || role.getRoleCode().contains("DIRECTOR"));

        return new UserPrincipal(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                authorities,
                departments,
                isManager
        );
    }

    private static List<String> extractDepartments(User user) {
        List<String> departments = new ArrayList<>();
        
        // Extract departments from roles
        user.getRoles().forEach(role -> {
            String roleCode = role.getRoleCode();
            if (roleCode.contains("HR_") || roleCode.equals("HR_GROUP")) {
                departments.add("HR");
            } else if (roleCode.contains("LEGAL_") || roleCode.equals("LEGAL_GROUP")) {
                departments.add("LEGAL");
            } else if (roleCode.contains("CSIS_") || roleCode.equals("CSIS_GROUP")) {
                departments.add("CSIS");
            } else if (roleCode.contains("INVESTIGATOR_") || roleCode.equals("INVESTIGATOR_GROUP")) {
                departments.add("INVESTIGATION");
            } else if (roleCode.contains("INTAKE_") || roleCode.equals("INTAKE_ANALYST_GROUP")) {
                departments.add("INTAKE");
            } else if (roleCode.contains("DIRECTOR_") || roleCode.equals("DIRECTOR_GROUP")) {
                departments.add("DIRECTOR");
            } else if (roleCode.contains("AROG_") || roleCode.equals("AROG_GROUP")) {
                departments.add("AROG");
            }
        });
        
        // If no specific department found, add a default
        if (departments.isEmpty()) {
            departments.add("GENERAL");
        }
        
        return departments.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // Getters for Cerbos integration
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public List<String> getDepartments() { return departments; }
    public boolean isManager() { return isManager; }
}