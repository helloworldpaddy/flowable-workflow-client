package com.workflow.cmsflowable.service;

import com.workflow.cmsflowable.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> getAllUsers();
    Optional<User> getUserById(Integer id);
    Optional<User> getUserByUserId(Long userId);
    Optional<User> getUserByUsername(String username);
    User createUser(User user);
    User updateUser(User user);
    void deleteUser(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}