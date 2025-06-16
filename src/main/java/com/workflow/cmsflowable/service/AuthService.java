package com.workflow.cmsflowable.service;

import com.workflow.cmsflowable.dto.request.LoginRequest;
import com.workflow.cmsflowable.dto.request.UserCreateRequest;
import com.workflow.cmsflowable.dto.response.LoginResponse;
import com.workflow.cmsflowable.entity.User;

public interface AuthService {
    LoginResponse authenticateUser(LoginRequest loginRequest);
    boolean validateToken(String token);
    void logout(String token);
    User createUser(UserCreateRequest userCreateRequest);
}