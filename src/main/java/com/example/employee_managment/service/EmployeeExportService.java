package com.example.employee_managment.service;

import com.example.employee_managment.model.Employee;
import com.example.employee_managment.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmployeeExportService {
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    private static final int EXPORT_BATCH_SIZE = 5000;
    
    /**
     * Export all employees to CSV file in batches
     * This method processes data in chunks to avoid memory issues
     */
    public String exportAllEmployeesToCsv() throws IOException {
        String fileName = "employees_export_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        Path filePath = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
        
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            // Write CSV header
            writer.write("ID,First Name,Last Name,Email,Phone Number,Date of Birth,Hire Date,Salary,Position,Department,Created At,Updated At\n");
            
            int pageNumber = 0;
            boolean hasMoreData = true;
            
            while (hasMoreData) {
                Pageable pageable = PageRequest.of(pageNumber, EXPORT_BATCH_SIZE);
                Page<Employee> employeePage = employeeRepository.findAll(pageable);
                
                if (employeePage.hasContent()) {
                    writeEmployeesToCsv(employeePage.getContent(), writer);
                    pageNumber++;
                } else {
                    hasMoreData = false;
                }
                
                // Optional: Add progress logging
                System.out.println("Processed page " + pageNumber + " with " + employeePage.getNumberOfElements() + " employees");
            }
        }
        
        return filePath.toString();
    }
    
    /**
     * Export employees by department to CSV
     */
    public String exportEmployeesByDepartmentToCsv(String department) throws IOException {
        String fileName = "employees_" + department + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        Path filePath = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
        
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            // Write CSV header
            writer.write("ID,First Name,Last Name,Email,Phone Number,Date of Birth,Hire Date,Salary,Position,Department,Created At,Updated At\n");
            
            List<Employee> employees = employeeRepository.findByDepartment(department);
            writeEmployeesToCsv(employees, writer);
        }
        
        return filePath.toString();
    }
    
    /**
     * Write employee data to CSV
     */
    private void writeEmployeesToCsv(List<Employee> employees, FileWriter writer) throws IOException {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (Employee employee : employees) {
            writer.write(String.format("\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%.2f\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                employee.getId(),
                escapeCsvField(employee.getFirstName()),
                escapeCsvField(employee.getLastName()),
                escapeCsvField(employee.getEmail()),
                escapeCsvField(employee.getPhoneNumber()),
                employee.getDateOfBirth() != null ? employee.getDateOfBirth().format(dateFormatter) : "",
                employee.getHireDate() != null ? employee.getHireDate().format(dateFormatter) : "",
                employee.getSalary() != null ? employee.getSalary() : 0.0,
                escapeCsvField(employee.getPosition()),
                escapeCsvField(employee.getDepartment()),
                employee.getCreatedAt() != null ? employee.getCreatedAt().format(dateTimeFormatter) : "",
                employee.getUpdatedAt() != null ? employee.getUpdatedAt().format(dateTimeFormatter) : ""
            ));
        }
    }
    
    /**
     * Escape CSV fields properly
     */
    private String escapeCsvField(String field) {
        if (field == null) return "";
        return field.replace("\"", "\"\""); // Escape quotes by doubling them
    }
    
    /**
     * Get export statistics
     */
    public long getTotalEmployeeCount() {
        return employeeRepository.count();
    }
    
    /**
     * Estimate export file size (rough calculation)
     */
    public long estimateExportFileSize() {
        long totalEmployees = getTotalEmployeeCount();
        // Rough estimate: 200 bytes per employee record
        return totalEmployees * 200;
    }
} 