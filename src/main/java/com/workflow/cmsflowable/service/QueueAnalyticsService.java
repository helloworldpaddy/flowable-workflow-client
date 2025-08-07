package com.workflow.cmsflowable.service;

import com.workflow.cmsflowable.enums.TaskStatus;
import com.workflow.cmsflowable.repository.QueueTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QueueAnalyticsService {
    
    private final QueueTaskRepository queueTaskRepository;
    
    /**
     * Get overall queue analytics dashboard
     */
    public Map<String, Object> getQueueDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Get all queue names
        List<String> queueNames = queueTaskRepository.findDistinctQueueNames();
        dashboard.put("totalQueues", queueNames.size());
        dashboard.put("queueNames", queueNames);
        
        // Overall statistics
        dashboard.put("totalTasks", queueTaskRepository.count());
        dashboard.put("openTasks", queueTaskRepository.countByStatus(TaskStatus.OPEN));
        dashboard.put("claimedTasks", queueTaskRepository.countByStatus(TaskStatus.CLAIMED));
        dashboard.put("completedTasks", queueTaskRepository.countByStatus(TaskStatus.COMPLETED));
        
        // Queue-wise breakdown
        Map<String, Map<String, Long>> queueBreakdown = new HashMap<>();
        for (String queueName : queueNames) {
            Map<String, Long> queueStats = new HashMap<>();
            queueStats.put("total", queueTaskRepository.countByQueueName(queueName));
            queueStats.put("open", queueTaskRepository.countByQueueNameAndStatus(queueName, TaskStatus.OPEN));
            queueStats.put("claimed", queueTaskRepository.countByQueueNameAndStatus(queueName, TaskStatus.CLAIMED));
            queueStats.put("completed", queueTaskRepository.countByQueueNameAndStatus(queueName, TaskStatus.COMPLETED));
            queueBreakdown.put(queueName, queueStats);
        }
        dashboard.put("queueBreakdown", queueBreakdown);
        
        return dashboard;
    }
    
    /**
     * Get detailed analytics for a specific queue
     */
    public Map<String, Object> getQueueAnalytics(String queueName) {
        Map<String, Object> analytics = new HashMap<>();
        
        analytics.put("queueName", queueName);
        analytics.put("totalTasks", queueTaskRepository.countByQueueName(queueName));
        analytics.put("openTasks", queueTaskRepository.countByQueueNameAndStatus(queueName, TaskStatus.OPEN));
        analytics.put("claimedTasks", queueTaskRepository.countByQueueNameAndStatus(queueName, TaskStatus.CLAIMED));
        analytics.put("completedTasks", queueTaskRepository.countByQueueNameAndStatus(queueName, TaskStatus.COMPLETED));
        
        // Age analysis for open tasks
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);
        Instant oneWeekAgo = now.minus(7, ChronoUnit.DAYS);
        
        Map<String, Object> ageAnalysis = new HashMap<>();
        ageAnalysis.put("openTasksOlderThan1Hour", 
            queueTaskRepository.findByQueueNameAndStatusOrderByPriorityDescCreatedAtAsc(queueName, TaskStatus.OPEN)
                .stream()
                .filter(task -> task.getCreatedAt().isBefore(oneHourAgo))
                .count());
        ageAnalysis.put("openTasksOlderThan1Day", 
            queueTaskRepository.findByQueueNameAndStatusOrderByPriorityDescCreatedAtAsc(queueName, TaskStatus.OPEN)
                .stream()
                .filter(task -> task.getCreatedAt().isBefore(oneDayAgo))
                .count());
        ageAnalysis.put("openTasksOlderThan1Week", 
            queueTaskRepository.findByQueueNameAndStatusOrderByPriorityDescCreatedAtAsc(queueName, TaskStatus.OPEN)
                .stream()
                .filter(task -> task.getCreatedAt().isBefore(oneWeekAgo))
                .count());
        
        analytics.put("ageAnalysis", ageAnalysis);
        
        return analytics;
    }
    
    /**
     * Get workload distribution by assignee
     */
    public Map<String, Object> getWorkloadDistribution() {
        Map<String, Object> distribution = new HashMap<>();
        
        // Get all tasks by status
        List<Object[]> tasksByStatus = queueTaskRepository.countTasksByStatus();
        Map<String, Long> statusDistribution = new HashMap<>();
        
        for (Object[] row : tasksByStatus) {
            TaskStatus status = (TaskStatus) row[0];
            Long count = (Long) row[1];
            statusDistribution.put(status.getValue(), count);
        }
        
        distribution.put("statusDistribution", statusDistribution);
        
        // Get tasks by queue and status
        List<Object[]> tasksByQueueAndStatus = queueTaskRepository.countTasksByQueueAndStatus(TaskStatus.OPEN);
        Map<String, Long> openTasksByQueue = new HashMap<>();
        
        for (Object[] row : tasksByQueueAndStatus) {
            String queueName = (String) row[0];
            Long count = (Long) row[1];
            openTasksByQueue.put(queueName, count);
        }
        
        distribution.put("openTasksByQueue", openTasksByQueue);
        
        return distribution;
    }
    
    /**
     * Get performance metrics
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        Instant now = Instant.now();
        Instant last24Hours = now.minus(1, ChronoUnit.DAYS);
        Instant lastWeek = now.minus(7, ChronoUnit.DAYS);
        
        // Tasks completed in last 24 hours
        long completedLast24Hours = queueTaskRepository.findByStatusAndCreatedAtBefore(TaskStatus.COMPLETED, now)
                .stream()
                .filter(task -> task.getCompletedAt() != null && task.getCompletedAt().isAfter(last24Hours))
                .count();
        
        // Tasks completed in last week
        long completedLastWeek = queueTaskRepository.findByStatusAndCreatedAtBefore(TaskStatus.COMPLETED, now)
                .stream()
                .filter(task -> task.getCompletedAt() != null && task.getCompletedAt().isAfter(lastWeek))
                .count();
        
        metrics.put("completedLast24Hours", completedLast24Hours);
        metrics.put("completedLastWeek", completedLastWeek);
        
        // Average completion rate per day (last week)
        double avgCompletionRatePerDay = completedLastWeek / 7.0;
        metrics.put("avgCompletionRatePerDay", avgCompletionRatePerDay);
        
        // Backlog metrics
        long totalBacklog = queueTaskRepository.countByStatus(TaskStatus.OPEN) + 
                           queueTaskRepository.countByStatus(TaskStatus.CLAIMED);
        metrics.put("totalBacklog", totalBacklog);
        
        // Estimated days to clear backlog
        double estimatedDaysToClearBacklog = avgCompletionRatePerDay > 0 ? totalBacklog / avgCompletionRatePerDay : -1;
        metrics.put("estimatedDaysToClearBacklog", estimatedDaysToClearBacklog);
        
        return metrics;
    }
    
    /**
     * Get health check for all queues
     */
    public Map<String, Object> getQueueHealthCheck() {
        Map<String, Object> healthCheck = new HashMap<>();
        
        List<String> queueNames = queueTaskRepository.findDistinctQueueNames();
        Map<String, String> queueHealth = new HashMap<>();
        
        Instant now = Instant.now();
        Instant warningThreshold = now.minus(4, ChronoUnit.HOURS); // Tasks older than 4 hours
        Instant criticalThreshold = now.minus(24, ChronoUnit.HOURS); // Tasks older than 24 hours
        
        for (String queueName : queueNames) {
            long openTasks = queueTaskRepository.countByQueueNameAndStatus(queueName, TaskStatus.OPEN);
            
            if (openTasks == 0) {
                queueHealth.put(queueName, "HEALTHY");
                continue;
            }
            
            long oldTasks = queueTaskRepository.findByQueueNameAndStatusOrderByPriorityDescCreatedAtAsc(queueName, TaskStatus.OPEN)
                    .stream()
                    .filter(task -> task.getCreatedAt().isBefore(criticalThreshold))
                    .count();
            
            long warningTasks = queueTaskRepository.findByQueueNameAndStatusOrderByPriorityDescCreatedAtAsc(queueName, TaskStatus.OPEN)
                    .stream()
                    .filter(task -> task.getCreatedAt().isBefore(warningThreshold) && task.getCreatedAt().isAfter(criticalThreshold))
                    .count();
            
            if (oldTasks > 0) {
                queueHealth.put(queueName, "CRITICAL");
            } else if (warningTasks > 0 || openTasks > 100) {
                queueHealth.put(queueName, "WARNING");
            } else {
                queueHealth.put(queueName, "HEALTHY");
            }
        }
        
        healthCheck.put("queueHealth", queueHealth);
        
        // Overall health status
        boolean hasCritical = queueHealth.values().contains("CRITICAL");
        boolean hasWarning = queueHealth.values().contains("WARNING");
        
        String overallHealth = hasCritical ? "CRITICAL" : hasWarning ? "WARNING" : "HEALTHY";
        healthCheck.put("overallHealth", overallHealth);
        
        return healthCheck;
    }
}