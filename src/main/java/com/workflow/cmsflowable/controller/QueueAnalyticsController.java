package com.workflow.cmsflowable.controller;

import com.workflow.cmsflowable.service.QueueAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/queue/analytics")
@Tag(name = "Queue Analytics", description = "APIs for queue analytics and monitoring")
@RequiredArgsConstructor
public class QueueAnalyticsController {
    
    private final QueueAnalyticsService queueAnalyticsService;
    
    @GetMapping("/dashboard")
    @Operation(summary = "Get queue dashboard", description = "Get overall queue analytics dashboard with statistics for all queues")
    public ResponseEntity<Map<String, Object>> getQueueDashboard() {
        try {
            Map<String, Object> dashboard = queueAnalyticsService.getQueueDashboard();
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{queueName}")
    @Operation(summary = "Get queue analytics", description = "Get detailed analytics for a specific queue")
    public ResponseEntity<Map<String, Object>> getQueueAnalytics(
            @Parameter(description = "Queue name") @PathVariable String queueName) {
        try {
            Map<String, Object> analytics = queueAnalyticsService.getQueueAnalytics(queueName);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/workload-distribution")
    @Operation(summary = "Get workload distribution", description = "Get workload distribution across queues and by status")
    public ResponseEntity<Map<String, Object>> getWorkloadDistribution() {
        try {
            Map<String, Object> distribution = queueAnalyticsService.getWorkloadDistribution();
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/performance")
    @Operation(summary = "Get performance metrics", description = "Get performance metrics including completion rates and backlog analysis")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        try {
            Map<String, Object> metrics = queueAnalyticsService.getPerformanceMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "Get queue health check", description = "Get health status for all queues based on task age and volume")
    public ResponseEntity<Map<String, Object>> getQueueHealthCheck() {
        try {
            Map<String, Object> healthCheck = queueAnalyticsService.getQueueHealthCheck();
            return ResponseEntity.ok(healthCheck);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}