package com.workflow.cmsflowable.repository;

import com.workflow.cmsflowable.entity.QueueTask;
import com.workflow.cmsflowable.enums.TaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueTaskRepository extends JpaRepository<QueueTask, String> {
    
    // Basic queue operations
    List<QueueTask> findByQueueNameAndStatusOrderByPriorityDescCreatedAtAsc(String queueName, TaskStatus status);
    
    List<QueueTask> findByQueueNameAndStatusOrderByPriorityDescCreatedAtAsc(String queueName, TaskStatus status, Pageable pageable);
    
    // Assignee operations
    List<QueueTask> findByAssigneeAndStatus(String assignee, TaskStatus status);
    
    List<QueueTask> findByAssignee(String assignee);
    
    // Process instance queries
    List<QueueTask> findByProcessInstanceId(String processInstanceId);
    
    List<QueueTask> findByProcessInstanceIdAndStatus(String processInstanceId, TaskStatus status);
    
    // Status queries
    List<QueueTask> findByStatus(TaskStatus status);
    
    long countByStatus(TaskStatus status);
    
    long countByQueueNameAndStatus(String queueName, TaskStatus status);
    
    // Time-based queries
    List<QueueTask> findByStatusAndCreatedAtBefore(TaskStatus status, Instant cutoffTime);
    
    // Custom queries
    @Query("SELECT qt FROM QueueTask qt WHERE qt.queueName = :queueName AND qt.status = :status AND qt.assignee IS NULL ORDER BY qt.priority DESC, qt.createdAt ASC")
    List<QueueTask> findAvailableTasksInQueue(@Param("queueName") String queueName, @Param("status") TaskStatus status);
    
    @Query("SELECT qt FROM QueueTask qt WHERE qt.queueName = :queueName AND qt.status = :status AND qt.assignee IS NULL ORDER BY qt.priority DESC, qt.createdAt ASC")
    List<QueueTask> findAvailableTasksInQueue(@Param("queueName") String queueName, @Param("status") TaskStatus status, Pageable pageable);
    
    @Query("SELECT DISTINCT qt.queueName FROM QueueTask qt")
    List<String> findDistinctQueueNames();
    
    @Query("SELECT COUNT(qt) FROM QueueTask qt WHERE qt.queueName = :queueName")
    long countByQueueName(@Param("queueName") String queueName);
    
    // Process definition queries
    List<QueueTask> findByProcessDefinitionKey(String processDefinitionKey);
    
    long countByProcessDefinitionKeyAndStatus(String processDefinitionKey, TaskStatus status);
    
    // Combined queries for dashboard statistics
    @Query("SELECT qt.queueName, COUNT(qt) FROM QueueTask qt WHERE qt.status = :status GROUP BY qt.queueName")
    List<Object[]> countTasksByQueueAndStatus(@Param("status") TaskStatus status);
    
    @Query("SELECT qt.status, COUNT(qt) FROM QueueTask qt GROUP BY qt.status")
    List<Object[]> countTasksByStatus();
    
    // Task lookup by Flowable task ID
    Optional<QueueTask> findByTaskId(String taskId);
    
    boolean existsByTaskId(String taskId);
}