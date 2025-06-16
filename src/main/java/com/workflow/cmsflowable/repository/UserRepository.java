package com.workflow.cmsflowable.repository;

import com.workflow.cmsflowable.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUserId(Long userId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
