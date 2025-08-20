package com.example.employee_managment.dto;

import com.example.employee_managment.model.ExportHistory;

public class ExportRequest {
    
    private String userId;
    private ExportHistory.ExportType exportType = ExportHistory.ExportType.CSV;
    
    // Filtering parameters
    private String department;
    private String position;
    private Double minSalary;
    private Double maxSalary;
    private String name;
    private String email;
    
    // Field selection
    private String fields = "id,firstName,lastName,email,department,position,salary,hireDate";
    
    // Sorting
    private String sortBy = "id";
    private String sortDir = "asc";
    
    // Pagination (for large exports)
    private Integer page = 1;
    private Integer size = 1000;
    
    // Constructors
    public ExportRequest() {}
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public ExportHistory.ExportType getExportType() { return exportType; }
    public void setExportType(ExportHistory.ExportType exportType) { this.exportType = exportType; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    public Double getMinSalary() { return minSalary; }
    public void setMinSalary(Double minSalary) { this.minSalary = minSalary; }
    
    public Double getMaxSalary() { return maxSalary; }
    public void setMaxSalary(Double maxSalary) { this.maxSalary = maxSalary; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFields() { return fields; }
    public void setFields(String fields) { this.fields = fields; }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    
    public String getSortDir() { return sortDir; }
    public void setSortDir(String sortDir) { this.sortDir = sortDir; }
    
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
} 