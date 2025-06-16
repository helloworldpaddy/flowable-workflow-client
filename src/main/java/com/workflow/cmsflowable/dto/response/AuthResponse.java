package com.workflow.cmsflowable.dto.response;  

import java.util.List;

public  class AuthResponse {
    private String token;
    private String username;
    private List<String> roles;
    
    // getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
