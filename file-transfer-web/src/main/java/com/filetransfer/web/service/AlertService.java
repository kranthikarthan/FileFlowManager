package com.filetransfer.web.service;

import com.filetransfer.web.dto.AlertConfigurationDto;
import com.filetransfer.web.dto.AlertHistoryDto;
import com.filetransfer.web.entity.AlertConfiguration;
import com.filetransfer.web.entity.AlertHistory;
import com.filetransfer.web.repository.AlertConfigurationRepository;
import com.filetransfer.web.repository.AlertHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertService {
    
    @Autowired
    private AlertConfigurationRepository alertConfigurationRepository;
    
    @Autowired
    private AlertHistoryRepository alertHistoryRepository;
    
    // Alert Configuration Methods
    public List<AlertConfigurationDto> getAlertConfigurationsForTenant(String tenantId) {
        return alertConfigurationRepository.findByTenantId(tenantId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<AlertConfigurationDto> getActiveAlertConfigurationsForTenant(String tenantId) {
        return alertConfigurationRepository.findActiveAlertsForTenant(tenantId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<AlertConfigurationDto> getAlertConfigurationsForService(String tenantId, String serviceName, String subServiceName) {
        return alertConfigurationRepository.findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Optional<AlertConfigurationDto> getAlertConfigurationById(Long id) {
        return alertConfigurationRepository.findById(id)
                .map(this::convertToDto);
    }
    
    public AlertConfigurationDto createAlertConfiguration(AlertConfigurationDto alertConfigDto) {
        AlertConfiguration alertConfig = convertToEntity(alertConfigDto);
        alertConfig.setCreatedAt(LocalDateTime.now());
        alertConfig.setCreatedBy("system"); // TODO: Get from security context
        
        AlertConfiguration savedAlertConfig = alertConfigurationRepository.save(alertConfig);
        return convertToDto(savedAlertConfig);
    }
    
    public AlertConfigurationDto updateAlertConfiguration(Long id, AlertConfigurationDto alertConfigDto) {
        AlertConfiguration existingAlertConfig = alertConfigurationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert configuration not found with id: " + id));
        
        existingAlertConfig.setTenantId(alertConfigDto.getTenantId());
        existingAlertConfig.setServiceName(alertConfigDto.getServiceName());
        existingAlertConfig.setSubServiceName(alertConfigDto.getSubServiceName());
        existingAlertConfig.setAlertType(convertToEntityAlertType(alertConfigDto.getAlertType()));
        existingAlertConfig.setAlertDurationMinutes(alertConfigDto.getAlertDurationMinutes());
        existingAlertConfig.setEnabled(alertConfigDto.getEnabled());
        existingAlertConfig.setEmailRecipients(alertConfigDto.getEmailRecipients());
        existingAlertConfig.setNotificationChannels(alertConfigDto.getNotificationChannels());
        existingAlertConfig.setUpdatedAt(LocalDateTime.now());
        existingAlertConfig.setUpdatedBy("system"); // TODO: Get from security context
        
        AlertConfiguration savedAlertConfig = alertConfigurationRepository.save(existingAlertConfig);
        return convertToDto(savedAlertConfig);
    }
    
    public void deleteAlertConfiguration(Long id) {
        if (!alertConfigurationRepository.existsById(id)) {
            throw new IllegalArgumentException("Alert configuration not found with id: " + id);
        }
        alertConfigurationRepository.deleteById(id);
    }
    
    // Alert History Methods
    public List<AlertHistoryDto> getAlertHistoryForTenant(String tenantId) {
        return alertHistoryRepository.findByTenantIdOrderBySentAtDesc(tenantId).stream()
                .map(this::convertToHistoryDto)
                .collect(Collectors.toList());
    }
    
    public List<AlertHistoryDto> getAlertHistoryForService(String tenantId, String serviceName, String subServiceName) {
        return alertHistoryRepository.findByTenantIdAndServiceNameAndSubServiceName(tenantId, serviceName, subServiceName).stream()
                .map(this::convertToHistoryDto)
                .collect(Collectors.toList());
    }
    
    public List<AlertHistoryDto> getUnacknowledgedAlerts(String tenantId) {
        return alertHistoryRepository.findUnacknowledgedAlerts(tenantId).stream()
                .map(this::convertToHistoryDto)
                .collect(Collectors.toList());
    }
    
    public AlertHistoryDto createAlertHistory(AlertHistoryDto alertHistoryDto) {
        AlertHistory alertHistory = convertToHistoryEntity(alertHistoryDto);
        alertHistory.setSentAt(LocalDateTime.now());
        
        AlertHistory savedAlertHistory = alertHistoryRepository.save(alertHistory);
        return convertToHistoryDto(savedAlertHistory);
    }
    
    public AlertHistoryDto acknowledgeAlert(Long id, String acknowledgedBy) {
        AlertHistory alertHistory = alertHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert history not found with id: " + id));
        
        alertHistory.setAcknowledgedAt(LocalDateTime.now());
        alertHistory.setAcknowledgedBy(acknowledgedBy);
        
        AlertHistory savedAlertHistory = alertHistoryRepository.save(alertHistory);
        return convertToHistoryDto(savedAlertHistory);
    }
    
    public void sendCutOffAlert(String tenantId, String serviceName, String subServiceName, String message) {
        List<AlertConfiguration> alertConfigs = alertConfigurationRepository.findActiveAlertsForService(
                tenantId, serviceName, subServiceName, AlertConfiguration.AlertType.CUT_OFF_MISSED);
        
        for (AlertConfiguration alertConfig : alertConfigs) {
            AlertHistoryDto alertHistoryDto = new AlertHistoryDto(
                    tenantId, serviceName, subServiceName, 
                    convertToDtoAlertType(alertConfig.getAlertType()), 
                    message, AlertHistoryDto.AlertLevel.WARNING);
            
            createAlertHistory(alertHistoryDto);
        }
    }
    
    // Conversion methods
    private AlertConfigurationDto convertToDto(AlertConfiguration alertConfig) {
        AlertConfigurationDto dto = new AlertConfigurationDto();
        dto.setId(alertConfig.getId());
        dto.setTenantId(alertConfig.getTenantId());
        dto.setServiceName(alertConfig.getServiceName());
        dto.setSubServiceName(alertConfig.getSubServiceName());
        dto.setAlertType(convertToDtoAlertType(alertConfig.getAlertType()));
        dto.setAlertDurationMinutes(alertConfig.getAlertDurationMinutes());
        dto.setEnabled(alertConfig.getEnabled());
        dto.setEmailRecipients(alertConfig.getEmailRecipients());
        dto.setNotificationChannels(alertConfig.getNotificationChannels());
        dto.setCreatedAt(alertConfig.getCreatedAt());
        dto.setUpdatedAt(alertConfig.getUpdatedAt());
        dto.setCreatedBy(alertConfig.getCreatedBy());
        dto.setUpdatedBy(alertConfig.getUpdatedBy());
        return dto;
    }
    
    private AlertConfiguration convertToEntity(AlertConfigurationDto dto) {
        AlertConfiguration alertConfig = new AlertConfiguration();
        alertConfig.setId(dto.getId());
        alertConfig.setTenantId(dto.getTenantId());
        alertConfig.setServiceName(dto.getServiceName());
        alertConfig.setSubServiceName(dto.getSubServiceName());
        alertConfig.setAlertType(convertToEntityAlertType(dto.getAlertType()));
        alertConfig.setAlertDurationMinutes(dto.getAlertDurationMinutes());
        alertConfig.setEnabled(dto.getEnabled());
        alertConfig.setEmailRecipients(dto.getEmailRecipients());
        alertConfig.setNotificationChannels(dto.getNotificationChannels());
        alertConfig.setCreatedAt(dto.getCreatedAt());
        alertConfig.setUpdatedAt(dto.getUpdatedAt());
        alertConfig.setCreatedBy(dto.getCreatedBy());
        alertConfig.setUpdatedBy(dto.getUpdatedBy());
        return alertConfig;
    }
    
    private AlertHistoryDto convertToHistoryDto(AlertHistory alertHistory) {
        AlertHistoryDto dto = new AlertHistoryDto();
        dto.setId(alertHistory.getId());
        dto.setTenantId(alertHistory.getTenantId());
        dto.setServiceName(alertHistory.getServiceName());
        dto.setSubServiceName(alertHistory.getSubServiceName());
        dto.setAlertType(convertToDtoAlertType(alertHistory.getAlertType()));
        dto.setAlertMessage(alertHistory.getAlertMessage());
        dto.setAlertLevel(convertToDtoAlertLevel(alertHistory.getAlertLevel()));
        dto.setSentAt(alertHistory.getSentAt());
        dto.setAcknowledgedAt(alertHistory.getAcknowledgedAt());
        dto.setAcknowledgedBy(alertHistory.getAcknowledgedBy());
        return dto;
    }
    
    private AlertHistory convertToHistoryEntity(AlertHistoryDto dto) {
        AlertHistory alertHistory = new AlertHistory();
        alertHistory.setId(dto.getId());
        alertHistory.setTenantId(dto.getTenantId());
        alertHistory.setServiceName(dto.getServiceName());
        alertHistory.setSubServiceName(dto.getSubServiceName());
        alertHistory.setAlertType(convertToEntityAlertType(dto.getAlertType()));
        alertHistory.setAlertMessage(dto.getAlertMessage());
        alertHistory.setAlertLevel(convertToEntityAlertLevel(dto.getAlertLevel()));
        alertHistory.setSentAt(dto.getSentAt());
        alertHistory.setAcknowledgedAt(dto.getAcknowledgedAt());
        alertHistory.setAcknowledgedBy(dto.getAcknowledgedBy());
        return alertHistory;
    }
    
    private AlertConfigurationDto.AlertType convertToDtoAlertType(AlertConfiguration.AlertType alertType) {
        return AlertConfigurationDto.AlertType.valueOf(alertType.name());
    }
    
    private AlertConfiguration.AlertType convertToEntityAlertType(AlertConfigurationDto.AlertType alertType) {
        return AlertConfiguration.AlertType.valueOf(alertType.name());
    }
    
    private AlertHistoryDto.AlertLevel convertToDtoAlertLevel(AlertHistory.AlertLevel alertLevel) {
        return AlertHistoryDto.AlertLevel.valueOf(alertLevel.name());
    }
    
    private AlertHistory.AlertLevel convertToEntityAlertLevel(AlertHistoryDto.AlertLevel alertLevel) {
        return AlertHistory.AlertLevel.valueOf(alertLevel.name());
    }
}