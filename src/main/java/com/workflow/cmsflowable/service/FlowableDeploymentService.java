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
    @Autowired
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
        Map<String, List<DeploymentResult>> allResults = new HashMap<>();

        allResults.put("bpmn", deployResources(bpmnPath, new String[]{"**/*.bpmn20.xml", "**/*.bpmn"}, "BPMN Process"));
        allResults.put("dmn", deployResources(dmnPath, new String[]{"**/*.dmn"}, "DMN Decision"));
        allResults.put("cmmn", deployResources(cmmnPath, new String[]{"**/*.cmmn", "**/*.cmmn.xml"}, "CMMN Case"));
        allResults.put("forms", deployResources(formPath, new String[]{"**/*.form"}, "Flowable Form"));

        return allResults;
    }

    /**
     * Deploys resources of a specific type from a given directory.
     * @param directory The base directory within the classpath (e.g., "processes/").
     * @param extensions An array of file extensions to search for (e.g., ".bpmn20.xml").
     * @param category The category name for the deployment (e.g., "BPMN Process").
     * @return A list of DeploymentResult objects indicating success or failure for each resource.
     */
    private List<DeploymentResult> deployResources(String directory, String[] extensions, String category) {
        List<DeploymentResult> results = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (String ext : extensions) {
            String locationPattern = basePath + directory + ext;
            try {
                Resource[] resources = resolver.getResources(locationPattern);
                if (resources.length == 0) {
                    logger.info("No {} files found in {}", category, locationPattern);
                }
                for (Resource resource : resources) {
                    results.add(deploySingleResource(resource, category));
                }
            } catch (IOException e) {
                logger.error("Error scanning resources at pattern {}: {}", locationPattern, e.getMessage(), e);
                results.add(new DeploymentResult(locationPattern, category, e));
            }
        }
        return results;
    }

    /**
     * Deploys a single Flowable resource.
     * @param resource The Spring Resource object representing the file.
     * @param category The category for the Flowable deployment.
     * @return A DeploymentResult object for the specific resource.
     */
    private DeploymentResult deploySingleResource(Resource resource, String category) {
        String resourceName = resource.getFilename();
        if (resourceName == null) {
            return new DeploymentResult("Unknown Resource", category, new IllegalArgumentException("Resource has no filename"));
        }

        try (InputStream inputStream = resource.getInputStream()) {
            // Flowable deployment name often includes timestamp/auto-generated, this is just descriptive
            String deploymentName = String.format("Auto-deployment - %s - %s", resourceName, category);

            Deployment deployment = repositoryService.createDeployment()
                .name(deploymentName)
                .category(category) // Use a category for better organization
                .addInputStream(resourceName, inputStream)
                .deploy();

            String definitionKey = extractDefinitionKey(resourceName, deployment.getId());

            logger.info("Successfully deployed {} (ID: {}) from {}. Definition Key: {}",
                    resourceName, deployment.getId(), resource.getDescription(), definitionKey);

            return new DeploymentResult(resourceName, category, deployment.getId(), definitionKey);

        } catch (org.flowable.common.engine.api.FlowableException e) {
            logger.error("Flowable engine error deploying {}: {}", resourceName, e.getMessage(), e);
            return new DeploymentResult(resourceName, category, e);
        } catch (IOException e) {
            logger.error("IO error reading resource {}: {}", resourceName, e.getMessage(), e);
            return new DeploymentResult(resourceName, category, e);
        } catch (Exception e) {
            logger.error("Unexpected error deploying {}: {}", resourceName, e.getMessage(), e);
            return new DeploymentResult(resourceName, category, e);
        }
    }

    /**
     * Attempts to extract the primary definition key from the deployed resource.
     * This is a heuristic and might not cover all cases or multiple definitions within one file.
     * For multiple definitions, you'd query the specific definition services.
     */
    private String extractDefinitionKey(String resourceName, String deploymentId) {
        try {
            if (resourceName.endsWith(".bpmn20.xml") || resourceName.endsWith(".bpmn")) {
                Set<String> processDefinitionKeys = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deploymentId)
                    .list().stream().map(pd -> pd.getKey()).collect(Collectors.toSet());
                return processDefinitionKeys.isEmpty() ? null : String.join(", ", processDefinitionKeys);
            } else if (resourceName.endsWith(".dmn")) {
                Set<String> decisionDefinitionKeys = dmnRepositoryService.createDecisionQuery()
                    .deploymentId(deploymentId)
                    .list().stream().map(dd -> dd.getKey()).collect(Collectors.toSet());
                return decisionDefinitionKeys.isEmpty() ? null : String.join(", ", decisionDefinitionKeys);
            } /*else if (resourceName.endsWith(".cmmn") || resourceName.endsWith(".cmmn.xml")) {
                Set<String> caseDefinitionKeys = cmmnRepositoryService.createCaseDefinitionQuery()
                    .deploymentId(deploymentId)
                    .list().stream().map(cd -> cd.getKey()).collect(Collectors.toSet());
                return caseDefinitionKeys.isEmpty() ? null : String.join(", ", caseDefinitionKeys);
            } else if (resourceName.endsWith(".form")) {
                 Set<String> formDefinitionKeys = formRepositoryService.createFormDefinitionQuery()
                    .deploymentId(deploymentId)
                    .list().stream().map(fd -> fd.getKey()).collect(Collectors.toSet());
                return formDefinitionKeys.isEmpty() ? null : String.join(", ", formDefinitionKeys);
            }*/
        } catch (Exception e) {
            logger.warn("Could not extract definition key for resource {}: {}", resourceName, e.getMessage());
            return null;
        }
        return null;
    }

    /**
     * Get status of deployed definitions.
     * This provides counts for each type of definition based on existing deployment.
     */
    public Map<String, Object> getDeploymentStatus() {
        Map<String, Object> status = new HashMap<>();

        long bpmnCount = repositoryService.createProcessDefinitionQuery().count();
        long dmnCount = dmnRepositoryService.createDecisionQuery().count();
        /*long cmmnCount = cmmnRepositoryService.createCaseDefinitionQuery().count();
        long formCount = formRepositoryService.createFormDefinitionQuery().count();*/

        status.put("success", true);
        status.put("message", "Current deployed definition counts.");
        status.put("bpmnProcessDefinitions", bpmnCount);
        status.put("dmnDecisionDefinitions", dmnCount);
        /*status.put("cmmnCaseDefinitions", cmmnCount);
        status.put("formDefinitions", formCount);*/
        status.put("totalDefinitions", bpmnCount + dmnCount);

        return status;
    }
}