package com.workflow.cmsflowable.service;

import com.workflow.cmsflowable.dto.response.CountryClusterResponse;
import com.workflow.cmsflowable.dto.response.DataSourceResponse;
import com.workflow.cmsflowable.dto.response.EscalationMethodResponse;
import com.workflow.cmsflowable.entity.CountryCluster;
import com.workflow.cmsflowable.entity.DataSource;
import com.workflow.cmsflowable.entity.EscalationMethod;
import com.workflow.cmsflowable.repository.CountryClusterRepository;
import com.workflow.cmsflowable.repository.DataSourceRepository;
import com.workflow.cmsflowable.repository.EscalationMethodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReferenceDataService {

    @Autowired
    private EscalationMethodRepository escalationMethodRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private CountryClusterRepository countryClusterRepository;

    public List<EscalationMethodResponse> getAllEscalationMethods() {
        return escalationMethodRepository.findAllActive()
                .stream()
                .map(this::convertToEscalationMethodResponse)
                .collect(Collectors.toList());
    }

    public List<DataSourceResponse> getAllDataSources() {
        return dataSourceRepository.findAllActive()
                .stream()
                .map(this::convertToDataSourceResponse)
                .collect(Collectors.toList());
    }

    public List<CountryClusterResponse> getAllCountryClusters() {
        return countryClusterRepository.findAllActive()
                .stream()
                .map(this::convertToCountryClusterResponse)
                .collect(Collectors.toList());
    }

    public List<CountryClusterResponse> getCountriesByRegion(String region) {
        return countryClusterRepository.findByRegionAndActive(region)
                .stream()
                .map(this::convertToCountryClusterResponse)
                .collect(Collectors.toList());
    }

    public List<CountryClusterResponse> getCountriesByCluster(String clusterName) {
        return countryClusterRepository.findByClusterNameOrderByCountryName(clusterName)
                .stream()
                .map(this::convertToCountryClusterResponse)
                .collect(Collectors.toList());
    }

    // Conversion methods
    private EscalationMethodResponse convertToEscalationMethodResponse(EscalationMethod entity) {
        return new EscalationMethodResponse(
                entity.getId(),
                entity.getMethodCode(),
                entity.getMethodName(),
                entity.getDescription(),
                entity.getIsActive()
        );
    }

    private DataSourceResponse convertToDataSourceResponse(DataSource entity) {
        return new DataSourceResponse(
                entity.getId(),
                entity.getSourceCode(),
                entity.getSourceName(),
                entity.getDescription(),
                entity.getIsActive()
        );
    }

    private CountryClusterResponse convertToCountryClusterResponse(CountryCluster entity) {
        return new CountryClusterResponse(
                entity.getId(),
                entity.getCountryCode(),
                entity.getCountryName(),
                entity.getClusterName(),
                entity.getRegion(),
                entity.getIsActive()
        );
    }
}