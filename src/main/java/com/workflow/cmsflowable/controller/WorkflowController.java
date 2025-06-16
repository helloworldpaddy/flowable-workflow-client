package com.workflow.cmsflowable.controller;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @PostMapping("/start/{processKey}")
    public ResponseEntity<Map<String, Object>> startProcess(
            @PathVariable String processKey,
            @RequestBody(required = false) Map<String, Object> variables) {
        
        if (variables == null) {
            variables = new HashMap<>();
        }
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processKey, variables);
        
        Map<String, Object> response = new HashMap<>();
        response.put("processInstanceId", processInstance.getId());
        response.put("processDefinitionId", processInstance.getProcessDefinitionId());
        response.put("businessKey", processInstance.getBusinessKey());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<Task>> getTasks(@RequestParam(required = false) String assignee) {
        List<Task> tasks;
        if (assignee != null && !assignee.isEmpty()) {
            tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        } else {
            tasks = taskService.createTaskQuery().list();
        }
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<String> completeTask(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> variables) {
        
        if (variables == null) {
            variables = new HashMap<>();
        }
        
        taskService.complete(taskId, variables);
        return ResponseEntity.ok("Task completed successfully");
    }

    @GetMapping("/process/{processInstanceId}")
    public ResponseEntity<ProcessInstance> getProcessInstance(@PathVariable String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        
        if (processInstance != null) {
            return ResponseEntity.ok(processInstance);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/processes")
    public ResponseEntity<List<ProcessInstance>> getActiveProcesses() {
        List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().list();
        return ResponseEntity.ok(processInstances);
    }
}