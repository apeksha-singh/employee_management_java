package com.example.employee_managment.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "export_history", indexes = {
    @Index(name = "idx_export_reference_id", columnList = "reference_id"),
    @Index(name = "idx_export_status", columnList = "status"),
    @Index(name = "idx_export_created_at", columnList = "created_at")
})
public class ExportHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "reference_id", unique = true, nullable = false, length = 50)
    private String referenceId;
    
    @Column(name = "user_id", length = 100)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 50)
    private ExportType exportType;
    
    @Column(name = "filters", columnDefinition = "JSON")
    private String filters; // JSON string of filter parameters
    
    @Column(name = "fields", columnDefinition = "TEXT")
    private String fields; // Comma-separated selected fields
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExportStatus status = ExportStatus.PENDING;
    
    @Column(name = "total_records")
    private Long totalRecords;
    
    @Column(name = "file_path", length = 500)
    private String filePath;
    
    @Column(name = "csv_data", columnDefinition = "LONGTEXT")
    private String csvData; // Store CSV content directly
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    // Enums
    public enum ExportType {
        CSV, EXCEL, PDF
    }
    
    public enum ExportStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
    
    // Constructors
    public ExportHistory() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ExportHistory(String referenceId, ExportType exportType, String filters, String fields) {
        this();
        this.referenceId = referenceId;
        this.exportType = exportType;
        this.filters = filters;
        this.fields = fields;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public ExportType getExportType() { return exportType; }
    public void setExportType(ExportType exportType) { this.exportType = exportType; }
    
    public String getFilters() { return filters; }
    public void setFilters(String filters) { this.filters = filters; }
    
    public String getFields() { return fields; }
    public void setFields(String fields) { this.fields = fields; }
    
    public ExportStatus getStatus() { return status; }
    public void setStatus(ExportStatus status) { this.status = status; }
    
    public Long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(Long totalRecords) { this.totalRecords = totalRecords; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getCsvData() { return csvData; }
    public void setCsvData(String csvData) { this.csvData = csvData; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
} 