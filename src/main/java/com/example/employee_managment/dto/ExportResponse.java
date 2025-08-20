package com.example.employee_managment.dto;

import com.example.employee_managment.model.ExportHistory;
import java.time.LocalDateTime;

public class ExportResponse {
    
    private String referenceId;
    private ExportHistory.ExportStatus status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime estimatedCompletion;
    private Long totalRecords;
    private Long fileSize;
    private String downloadUrl;
    
    // Constructors
    public ExportResponse() {}
    
    public ExportResponse(String referenceId, ExportHistory.ExportStatus status, String message) {
        this.referenceId = referenceId;
        this.status = status;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    
    public ExportHistory.ExportStatus getStatus() { return status; }
    public void setStatus(ExportHistory.ExportStatus status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getEstimatedCompletion() { return estimatedCompletion; }
    public void setEstimatedCompletion(LocalDateTime estimatedCompletion) { this.estimatedCompletion = estimatedCompletion; }
    
    public Long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(Long totalRecords) { this.totalRecords = totalRecords; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    
    public LocalDateTime getStartedAt() { return null; } // Placeholder - not used in response
    public void setStartedAt(LocalDateTime startedAt) { /* Not needed in response */ }
    
    public LocalDateTime getCompletedAt() { return null; } // Placeholder - not used in response
    public void setCompletedAt(LocalDateTime completedAt) { /* Not needed in response */ }
} 