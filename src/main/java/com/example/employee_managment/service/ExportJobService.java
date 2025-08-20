package com.example.employee_managment.service;

import com.example.employee_managment.model.Employee;
import com.example.employee_managment.model.ExportHistory;
import com.example.employee_managment.repository.ExportHistoryRepository;
import com.example.employee_managment.dto.ExportRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class ExportJobService {
    
    @Autowired
    private ExportHistoryRepository exportHistoryRepository;
    
    @Autowired
    private EmployeeService employeeService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Process export job asynchronously
     */
    @Async
    public void processExportJob(String referenceId) {
        try {
            // Get export history record
            ExportHistory exportHistory = exportHistoryRepository.findByReferenceId(referenceId)
                    .orElseThrow(() -> new RuntimeException("Export not found: " + referenceId));
            
            // Update status to PROCESSING
            exportHistory.setStatus(ExportHistory.ExportStatus.PROCESSING);
            exportHistory.setStartedAt(LocalDateTime.now());
            exportHistoryRepository.save(exportHistory);
            
            // Parse filters from JSON
            ExportRequest exportRequest = parseExportRequest(exportHistory);
            
            // Get filtered employees
            List<Employee> employees = getFilteredEmployees(exportRequest);
            
            // Update total records
            exportHistory.setTotalRecords((long) employees.size());
            exportHistoryRepository.save(exportHistory);
            
            // Generate CSV content
            String csvContent = generateCsvContent(employees, exportRequest.getFields());
            
            // Store CSV data
            exportHistory.setCsvData(csvContent);
            exportHistory.setFileSize((long) csvContent.getBytes(StandardCharsets.UTF_8).length);
            
            // Update status to COMPLETED
            exportHistory.setStatus(ExportHistory.ExportStatus.COMPLETED);
            exportHistory.setCompletedAt(LocalDateTime.now());
            exportHistoryRepository.save(exportHistory);
            
        } catch (Exception e) {
            // Update status to FAILED
            ExportHistory exportHistory = exportHistoryRepository.findByReferenceId(referenceId).orElse(null);
            if (exportHistory != null) {
                exportHistory.setStatus(ExportHistory.ExportStatus.FAILED);
                exportHistory.setErrorMessage(e.getMessage());
                exportHistory.setCompletedAt(LocalDateTime.now());
                exportHistoryRepository.save(exportHistory);
            }
        }
    }
    
    /**
     * Parse export request from JSON filters
     */
    private ExportRequest parseExportRequest(ExportHistory exportHistory) throws IOException {
        if (exportHistory.getFilters() != null && !exportHistory.getFilters().isEmpty()) {
            return objectMapper.readValue(exportHistory.getFilters(), ExportRequest.class);
        }
        return new ExportRequest();
    }
    
    /**
     * Get filtered employees based on export request
     */
    private List<Employee> getFilteredEmployees(ExportRequest request) {
        if (request.getEmail() != null) {
            // Single employee by email
            return employeeService.getEmployeeByEmail(request.getEmail())
                    .map(List::of)
                    .orElse(List.of());
        } else if (request.getName() != null) {
            // Search by name
            return employeeService.searchEmployeesByName(request.getName());
        } else if (request.getDepartment() != null && request.getPosition() != null) {
            // Filter by department and position
            return employeeService.findByDepartmentAndPosition(request.getDepartment(), request.getPosition());
        } else if (request.getDepartment() != null) {
            // Filter by department
            return employeeService.getEmployeesByDepartment(request.getDepartment());
        } else if (request.getPosition() != null) {
            // Filter by position
            return employeeService.getEmployeesByPosition(request.getPosition());
        } else if (request.getMinSalary() != null || request.getMaxSalary() != null) {
            // Filter by salary range
            List<Employee> employees = List.of();
            if (request.getMinSalary() != null) {
                employees = employeeService.getEmployeesWithSalaryGreaterThan(request.getMinSalary());
            }
            // Apply max salary filter if specified
            if (request.getMaxSalary() != null) {
                employees = employees.stream()
                    .filter(emp -> emp.getSalary() != null && emp.getSalary() <= request.getMaxSalary())
                    .toList();
            }
            return employees;
        } else {
            // No filters - get all employees (limited by size)
            return employeeService.getAllEmployeesPaginated(
                org.springframework.data.domain.PageRequest.of(
                    request.getPage() - 1, 
                    request.getSize()
                )
            ).getContent();
        }
    }
    
    /**
     * Generate CSV content with selected fields
     */
    private String generateCsvContent(List<Employee> employees, String fields) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
        
        // Parse selected fields
        String[] selectedFields = fields.split(",");
        
        // Write CSV header
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < selectedFields.length; i++) {
            if (i > 0) header.append(",");
            header.append(getFieldDisplayName(selectedFields[i].trim()));
        }
        header.append("\n");
        writer.write(header.toString());
        
        // Write data rows
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (Employee employee : employees) {
            StringBuilder row = new StringBuilder();
            for (int i = 0; i < selectedFields.length; i++) {
                if (i > 0) row.append(",");
                String fieldValue = getFieldValue(employee, selectedFields[i].trim(), dateFormatter, dateTimeFormatter);
                row.append("\"").append(escapeCsvField(fieldValue)).append("\"");
            }
            row.append("\n");
            writer.write(row.toString());
        }
        
        writer.flush();
        writer.close();
        return baos.toString();
    }
    
    /**
     * Get field display name
     */
    private String getFieldDisplayName(String field) {
        return switch (field.toLowerCase()) {
            case "id" -> "ID";
            case "firstname" -> "First Name";
            case "lastname" -> "Last Name";
            case "email" -> "Email";
            case "phonenumber" -> "Phone Number";
            case "dateofbirth" -> "Date of Birth";
            case "hiredate" -> "Hire Date";
            case "salary" -> "Salary";
            case "position" -> "Position";
            case "department" -> "Department";
            case "createdat" -> "Created At";
            case "updatedat" -> "Updated At";
            default -> field;
        };
    }
    
    /**
     * Get field value from employee
     */
    private String getFieldValue(Employee employee, String field, DateTimeFormatter dateFormatter, DateTimeFormatter dateTimeFormatter) {
        return switch (field.toLowerCase()) {
            case "id" -> String.valueOf(employee.getId());
            case "firstname" -> employee.getFirstName();
            case "lastname" -> employee.getLastName();
            case "email" -> employee.getEmail();
            case "phonenumber" -> employee.getPhoneNumber();
            case "dateofbirth" -> employee.getDateOfBirth() != null ? employee.getDateOfBirth().format(dateFormatter) : "";
            case "hiredate" -> employee.getHireDate() != null ? employee.getHireDate().format(dateFormatter) : "";
            case "salary" -> employee.getSalary() != null ? String.format("%.2f", employee.getSalary()) : "";
            case "position" -> employee.getPosition();
            case "department" -> employee.getDepartment();
            case "createdat" -> employee.getCreatedAt() != null ? employee.getCreatedAt().format(dateTimeFormatter) : "";
            case "updatedat" -> employee.getUpdatedAt() != null ? employee.getUpdatedAt().format(dateTimeFormatter) : "";
            default -> "";
        };
    }
    
    /**
     * Escape CSV fields
     */
    private String escapeCsvField(String field) {
        if (field == null) return "";
        return field.replace("\"", "\"\""); // Escape quotes by doubling them
    }
    
    /**
     * Generate unique reference ID
     */
    public String generateReferenceId() {
        return "EXP_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
} 