// src/main/java/com/workflow/cmsflowable/service/FlowableDeploymentService.java
package com.workflow.cmsflowable.service;

import com.workflow.cmsflowable.model.DeploymentResult;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
// import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.dmn.api.DmnRepositoryService;
// import org.flowable.form.api.FormRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FlowableDeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(FlowableDeploymentService.class);

    @Autowired
    private RepositoryService repositoryService;

    /*@Autowired
    private CmmnRepositoryService cmmnRepositoryService;*/
    @Autowired(required = false)
    private DmnRepositoryService dmnRepositoryService;
    /*@Autowired
    private FormRepositoryService formRepositoryService;*/

    @Value("${flowable.resource.base-path:classpath:/}")
    private String basePath;

    @Value("${flowable.resource.bpmn-path:processes/}")
    private String bpmnPath;

    @Value("${flowable.resource.dmn-path:dmn/}")
    private String dmnPath;

    @Value("${flowable.resource.cmmn-path:cmmn/}")
    private String cmmnPath;

    @Value("${flowable.resource.form-path:forms/}")
    private String formPath;

    /**
     * Deploys all Flowable resources (BPMN, DMN, CMMN, Forms) found in configured directories.
     * @return A map containing lists of deployment results for each resource type.
     */
    public Map<String, List<DeploymentResult>> deployAllFlowableResources() {
        logger.info("Starting deployment of all Flowable resources...");
        Map<String, List<DeploymentResult>> allResults = new HashMap<>();

        try {
            // Check if services are available
            if (repositoryService == null) {
                logger.error("RepositoryService is not available!");
                throw new IllegalStateException("RepositoryService is not injected");
            }
            
            logger.info("Deploying BPMN resources from: {}{}", basePath, bpmnPath);
            allResults.put("bpmn", deployResources(bpmnPath, new String[]{"**/*.bpmn20.xml", "**/*.bpmn"}, "BPMN Process"));
            
            if (dmnRepositoryService != null) {
                logger.info("Deploying DMN resources from: {}{}", basePath, dmnPath);
                allResults.put("dmn", deployDmnResources(dmnPath, new String[]{"**/*.dmn"}, "DMN Decision"));
            } else {
                logger.warn("DMN Repository Service not available, skipping DMN deployment");
                allResults.put("dmn", new ArrayList<>());
            }
            
            // Skip CMMN and Forms as they are disabled
            allResults.put("cmmn", new ArrayList<>());
            allResults.put("forms", new ArrayList<>());

            logger.info("Completed deployment of all Flowable resources");
            return allResults;
        } catch (Exception e) {
            logger.error("Failed to deploy Flowable resources", e);
            throw new RuntimeException("Deployment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deploys BPMN resources using RepositoryService.
     */
    private List<DeploymentResult> deployResources(String directory, String[] extensions, String category) {
        List<DeploymentResult> results = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (String ext : extensions) {
            String locationPattern = basePath + directory + ext;
            logger.debug("Scanning for {} files at pattern: {}", category, locationPattern);
            try {
                Resource[] resources = resolver.getResources(locationPattern);
                logger.info("Found {} {} files in {}", resources.length, category, locationPattern);
                if (resources.length == 0) {
                    logger.info("No {} files found in {}", category, locationPattern);
                }
                for (Resource resource : resources) {
                    logger.debug("Processing resource: {}", resource.getFilename());
                    results.add(deploySingleBpmnResource(resource, category));
                }
            } catch (IOException e) {
                logger.error("Error scanning resources at pattern {}: {}", locationPattern, e.getMessage(), e);
                results.add(new DeploymentResult(locationPattern, category, e));
            }
        }
        return results;
    }

    /**
     * Deploys DMN resources using DmnRepositoryService.
     */
    private List<DeploymentResult> deployDmnResources(String directory, String[] extensions, String category) {
        List<DeploymentResult> results = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (String ext : extensions) {
            String locationPattern = basePath + directory + ext;
            logger.debug("Scanning for {} files at pattern: {}", category, locationPattern);
            try {
                Resource[] resources = resolver.getResources(locationPattern);
                logger.info("Found {} {} files in {}", resources.length, category, locationPattern);
                if (resources.length == 0) {
                    logger.info("No {} files found in {}", category, locationPattern);
                }
                for (Resource resource : resources) {
                    logger.debug("Processing DMN resource: {}", resource.getFilename());
                    results.add(deploySingleDmnResource(resource, category));
                }
            } catch (IOException e) {
                logger.error("Error scanning resources at pattern {}: {}", locationPattern, e.getMessage(), e);
                results.add(new DeploymentResult(locationPattern, category, e));
            }
        }
        return results;
    }

    /**
     * Deploys a single BPMN resource using RepositoryService.
     */
    private DeploymentResult deploySingleBpmnResource(Resource resource, String category) {
        String resourceName = resource.getFilename();
        if (resourceName == null) {
            return new DeploymentResult("Unknown Resource", category, new IllegalArgumentException("Resource has no filename"));
        }

        try (InputStream inputStream = resource.getInputStream()) {
            String deploymentName = String.format("Auto-deployment - %s - %s", resourceName, category);
            logger.debug("Creating BPMN deployment: {}", deploymentName);

            Deployment deployment = repositoryService.createDeployment()
                .name(deploymentName)
                .category(category)
                .addInputStream(resourceName, inputStream)
                .deploy();

            String definitionKey = extractBpmnDefinitionKey(resourceName, deployment.getId());

            logger.info("Successfully deployed BPMN {} (ID: {}) from {}. Definition Key: {}",
                    resourceName, deployment.getId(), resource.getDescription(), definitionKey);

            return new DeploymentResult(resourceName, category, deployment.getId(), definitionKey);

        } catch (org.flowable.common.engine.api.FlowableException e) {
            logger.error("Flowable engine error deploying BPMN {}: {}", resourceName, e.getMessage(), e);
            return new DeploymentResult(resourceName, category, e);
        } catch (IOException e) {
            logger.error("IO error reading BPMN resource {}: {}", resourceName, e.getMessage(), e);
            return new DeploymentResult(resourceName, category, e);
        } catch (Exception e) {
            logger.error("Unexpected error deploying BPMN {}: {}", resourceName, e.getMessage(), e);
            return new DeploymentResult(resourceName, category, e);
        }
    }

    /**
     * Deploys a single DMN resource using DmnRepositoryService.
     */
    private DeploymentResult deploySingleDmnResource(Resource resource, String category) {
        String resourceName = resource.getFilename();
        if (resourceName == null) {
            return new DeploymentResult("Unknown Resource", category, new IllegalArgumentException("Resource has no filename"));
        }

        try (InputStream inputStream = resource.getInputStream()) {
            String deploymentName = String.format("Auto-deployment - %s - %s", resourceName, category);
            logger.debug("Creating DMN deployment: {}", deploymentName);

            org.flowable.dmn.api.DmnDeployment deployment = dmnRepositoryService.createDeployment()
                .name(deploymentName)
                .category(category)
                .addInputStream(resourceName, inputStream)
                .deploy();

            String definitionKey = extractDmnDefinitionKey(resourceName, deployment.getId());

            logger.info("Successfully deployed DMN {} (ID: {}) from {}. Definition Key: {}",
                    resourceName, deployment.getId(), resource.getDescription(), definitionKey);

            return new DeploymentResult(resourceName, category, deployment.getId(), definitionKey);

        } catch (org.flowable.common.engine.api.FlowableException e) {
            logger.error("Flowable engine error deploying DMN {}: {}", resourceName, e.getMessage(), e);
            return new DeploymentResult(resourceName, category, e);
        } catch (IOException e) {
            logger.error("IO error reading DMN resource {}: {}", resourceName, e.getMessage(), e);
            return new DeploymentResult(resourceName, category, e);
        } catch (Exception e) {
            logger.error("Unexpected error deploying DMN {}: {}", resourceName, e.getMessage(), e);
            return new DeploymentResult(resourceName, category, e);
        }
    }

    /**
     * Extract BPMN process definition keys from deployment.
     */
    private String extractBpmnDefinitionKey(String resourceName, String deploymentId) {
        try {
            Set<String> processDefinitionKeys = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .list().stream().map(pd -> pd.getKey()).collect(Collectors.toSet());
            return processDefinitionKeys.isEmpty() ? null : String.join(", ", processDefinitionKeys);
        } catch (Exception e) {
            logger.warn("Could not extract BPMN definition key for resource {}: {}", resourceName, e.getMessage());
            return null;
        }
    }

    /**
     * Extract DMN decision definition keys from deployment.
     */
    private String extractDmnDefinitionKey(String resourceName, String deploymentId) {
        try {
            if (dmnRepositoryService != null) {
                Set<String> decisionDefinitionKeys = dmnRepositoryService.createDecisionQuery()
                    .deploymentId(deploymentId)
                    .list().stream().map(dd -> dd.getKey()).collect(Collectors.toSet());
                return decisionDefinitionKeys.isEmpty() ? null : String.join(", ", decisionDefinitionKeys);
            }
            return null;
        } catch (Exception e) {
            logger.warn("Could not extract DMN definition key for resource {}: {}", resourceName, e.getMessage());
            return null;
        }
    }

    /**
     * Get status of deployed definitions.
     * This provides counts for each type of definition based on existing deployment.
     */
    public Map<String, Object> getDeploymentStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            long bpmnCount = repositoryService != null ? repositoryService.createProcessDefinitionQuery().count() : 0;
            long dmnCount = dmnRepositoryService != null ? dmnRepositoryService.createDecisionQuery().count() : 0;

            status.put("success", true);
            status.put("message", "Current deployed definition counts.");
            status.put("bpmnProcessDefinitions", bpmnCount);
            status.put("dmnDecisionDefinitions", dmnCount);
            status.put("cmmnCaseDefinitions", 0L); // Disabled
            status.put("formDefinitions", 0L); // Disabled
            status.put("totalDefinitions", bpmnCount + dmnCount);

            logger.info("Deployment status: BPMN={}, DMN={}, Total={}", bpmnCount, dmnCount, bpmnCount + dmnCount);
        } catch (Exception e) {
            logger.error("Error getting deployment status", e);
            status.put("success", false);
            status.put("message", "Error retrieving deployment status: " + e.getMessage());
            status.put("error", e.getClass().getSimpleName());
        }

        return status;
    }
}