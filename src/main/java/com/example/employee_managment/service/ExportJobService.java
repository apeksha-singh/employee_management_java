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
        List<Employee> employees;
        
        if (request.getEmail() != null) {
            // Single employee by email
            employees = employeeService.getEmployeeByEmail(request.getEmail())
                    .map(List::of)
                    .orElse(List.of());
        } else if (request.getName() != null) {
            // Search by name
            employees = employeeService.searchEmployeesByName(request.getName());
        } else if (request.getDepartment() != null && request.getPosition() != null) {
            // Filter by department and position
            employees = employeeService.findByDepartmentAndPosition(request.getDepartment(), request.getPosition());
        } else if (request.getDepartment() != null) {
            // Filter by department
            employees = employeeService.getEmployeesByDepartment(request.getDepartment());
        } else if (request.getPosition() != null) {
            // Filter by position
            employees = employeeService.getEmployeesByPosition(request.getPosition());
        } else if (request.getMinSalary() != null || request.getMaxSalary() != null) {
            // Filter by salary range
            employees = List.of();
            if (request.getMinSalary() != null) {
                employees = employeeService.getEmployeesWithSalaryGreaterThan(request.getMinSalary());
            }
            // Apply max salary filter if specified
            if (request.getMaxSalary() != null) {
                employees = employees.stream()
                    .filter(emp -> emp.getSalary() != null && emp.getSalary() <= request.getMaxSalary())
                    .toList();
            }
        } else {
            // No filters - get all employees (limited by size)
            employees = employeeService.getAllEmployeesPaginated(
                org.springframework.data.domain.PageRequest.of(
                    request.getPage() - 1, 
                    request.getSize()
                )
            ).getContent();
        }
        
        // Apply sorting to the filtered results
        List<Employee> sortedEmployees = applySorting(employees, request.getSortBy(), request.getSortDir());
        
        // Debug logging
        System.out.println("=== EXPORT DEBUG ===");
        System.out.println("Sort By: " + request.getSortBy());
        System.out.println("Sort Dir: " + request.getSortDir());
        System.out.println("Total Employees: " + sortedEmployees.size());
        System.out.println("First 3 employees after sorting:");
        sortedEmployees.stream().limit(3).forEach(emp -> 
            System.out.println("  - " + emp.getFirstName() + " " + emp.getLastName() + " (ID: " + emp.getId() + ")")
        );
        System.out.println("==================");
        
        return sortedEmployees;
    }
    
    /**
     * Apply sorting to employee list
     */
    private List<Employee> applySorting(List<Employee> employees, String sortBy, String sortDir) {
        if (employees == null || employees.isEmpty() || sortBy == null) {
            return employees;
        }
        
        // Normalize sortBy field name
        String normalizedSortBy = sortBy.toLowerCase().trim();
        
        System.out.println("Sorting by: '" + normalizedSortBy + "' with direction: '" + sortDir + "'");
        
        return employees.stream()
            .sorted((e1, e2) -> {
                int comparison = 0;
                
                switch (normalizedSortBy) {
                    case "id":
                        comparison = Long.compare(e1.getId(), e2.getId());
                        break;
                    case "firstname":
                        comparison = compareStrings(e1.getFirstName(), e2.getFirstName());
                        break;
                    case "lastname":
                        comparison = compareStrings(e1.getLastName(), e2.getLastName());
                        break;
                    case "email":
                        comparison = compareStrings(e1.getEmail(), e2.getEmail());
                        break;
                    case "salary":
                        comparison = compareDoubles(e1.getSalary(), e2.getSalary());
                        break;
                    case "department":
                        comparison = compareStrings(e1.getDepartment(), e2.getDepartment());
                        break;
                    case "position":
                        comparison = compareStrings(e1.getPosition(), e2.getPosition());
                        break;
                    case "hiredate":
                        comparison = compareDates(e1.getHireDate(), e2.getHireDate());
                        break;
                    case "dateofbirth":
                        comparison = compareDates(e1.getDateOfBirth(), e2.getDateOfBirth());
                        break;
                    case "createdat":
                        comparison = compareDateTimes(e1.getCreatedAt(), e2.getCreatedAt());
                        break;
                    case "updatedat":
                        comparison = compareDateTimes(e1.getUpdatedAt(), e2.getUpdatedAt());
                        break;
                    default:
                        System.out.println("Unknown sort field: '" + normalizedSortBy + "', using default sorting");
                        comparison = 0;
                }
                
                // Apply sort direction
                boolean isDescending = "desc".equalsIgnoreCase(sortDir);
                int result = isDescending ? -comparison : comparison;
                
                System.out.println("Comparing: " + e1.getFirstName() + " vs " + e2.getFirstName() + 
                                 " (comparison: " + comparison + ", result: " + result + ")");
                
                return result;
            })
            .toList();
    }
    
    /**
     * Helper methods for comparison
     */
    private int compareStrings(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        return s1.compareToIgnoreCase(s2);
    }
    
    private int compareDoubles(Double d1, Double d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return -1;
        if (d2 == null) return 1;
        return d1.compareTo(d2);
    }
    
    private int compareDates(java.time.LocalDate d1, java.time.LocalDate d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return -1;
        if (d2 == null) return 1;
        return d1.compareTo(d2);
    }
    
    private int compareDateTimes(java.time.LocalDateTime dt1, java.time.LocalDateTime dt2) {
        if (dt1 == null && dt2 == null) return 0;
        if (dt1 == null) return -1;
        if (dt2 == null) return 1;
        return dt1.compareTo(dt2);
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