package com.example.employee_managment.controller;

import com.example.employee_managment.model.Employee;
import com.example.employee_managment.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*") // Allow all origins for development
public class EmployeeController {
    
    @Autowired
    private EmployeeService employeeService;
    
    // CREATE - POST /api/employees
    @PostMapping
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody Employee employee) {
        try {
            Employee createdEmployee = employeeService.createEmployee(employee);
            return new ResponseEntity<>(createdEmployee, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    // READ ALL - GET /api/employees (with pagination)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllEmployees(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        // Validate page size to prevent abuse
        if (size > 100) {
            size = 100;
        }
        
        // Convert from 1-based to 0-based for Spring Data
        int zeroBasedPage = page - 1;
        
        Pageable pageable = PageRequest.of(zeroBasedPage, size, 
            sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : 
            Sort.by(sortBy).ascending());
        
        Page<Employee> employeePage = employeeService.getAllEmployeesPaginated(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("employees", employeePage.getContent());
        response.put("currentPage", page); // Return 1-based page number to user
        response.put("totalItems", employeePage.getTotalElements());
        response.put("totalPages", employeePage.getTotalPages());
        response.put("hasNext", employeePage.hasNext());
        response.put("hasPrevious", employeePage.hasPrevious());
        response.put("pageSize", size);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    // READ BY ID - GET /api/employees/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        Optional<Employee> employee = employeeService.getEmployeeById(id);
        return employee.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    // READ BY EMAIL - GET /api/employees/email/{email}
    @GetMapping("/email/{email}")
    public ResponseEntity<Employee> getEmployeeByEmail(@PathVariable String email) {
        Optional<Employee> employee = employeeService.getEmployeeByEmail(email);
        return employee.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    // UPDATE - PUT /api/employees/{id}
    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @Valid @RequestBody Employee employeeDetails) {
        try {
            Employee updatedEmployee = employeeService.updateEmployee(id, employeeDetails);
            return new ResponseEntity<>(updatedEmployee, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // DELETE - DELETE /api/employees/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        try {
            employeeService.deleteEmployee(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // SEARCH BY DEPARTMENT - GET /api/employees/department/{department}
    @GetMapping("/department/{department}")
    public ResponseEntity<List<Employee>> getEmployeesByDepartment(@PathVariable String department) {
        List<Employee> employees = employeeService.getEmployeesByDepartment(department);
        return new ResponseEntity<>(employees, HttpStatus.OK);
    }
    
    // SEARCH BY POSITION - GET /api/employees/position/{position}
    @GetMapping("/position/{position}")
    public ResponseEntity<List<Employee>> getEmployeesByPosition(@PathVariable String position) {
        List<Employee> employees = employeeService.getEmployeesByPosition(position);
        return new ResponseEntity<>(employees, HttpStatus.OK);
    }
    
    // SEARCH BY NAME - GET /api/employees/search?name={name}
    @GetMapping("/search")
    public ResponseEntity<List<Employee>> searchEmployeesByName(@RequestParam String name) {
        List<Employee> employees = employeeService.searchEmployeesByName(name);
        return new ResponseEntity<>(employees, HttpStatus.OK);
    }
    
    // SEARCH BY SALARY - GET /api/employees/salary?min={minSalary}
    @GetMapping("/salary")
    public ResponseEntity<List<Employee>> getEmployeesWithSalaryGreaterThan(@RequestParam Double min) {
        List<Employee> employees = employeeService.getEmployeesWithSalaryGreaterThan(min);
        return new ResponseEntity<>(employees, HttpStatus.OK);
    }

    // EXPORT TO CSV - GET /api/employees/export/csv
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportEmployeesToCsv(
            // Pagination
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "1000") int size,
            
            // Filtering
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Double minSalary,
            @RequestParam(required = false) Double maxSalary,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            
            // Field Selection (comma-separated list)
            @RequestParam(defaultValue = "id,firstName,lastName,email,department,position,salary,hireDate") String fields,
            
            // Sorting
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        try {
            // Validate page size for export (allow larger chunks)
            if (size > 10000) {
                size = 10000;
            }
            
            // Convert from 1-based to 0-based for Spring Data
            int zeroBasedPage = page - 1;
            
            Pageable pageable = PageRequest.of(zeroBasedPage, size, 
                sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending());
            
            // Get filtered employees
            List<Employee> employees = getFilteredEmployees(department, position, minSalary, maxSalary, name, email, pageable);
            
            // Generate CSV content with selected fields
            byte[] csvContent = generateCsvContentWithFields(employees, fields);
            
            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            
            // Generate filename based on filters
            String fileName = generateFileName(department, position, minSalary, maxSalary, name, email);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(csvContent.length);
            
            return new ResponseEntity<>(csvContent, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Helper method to get filtered employees
    private List<Employee> getFilteredEmployees(String department, String position, 
                                              Double minSalary, Double maxSalary, 
                                              String name, String email, Pageable pageable) {
        
        // If no filters, return paginated results
        if (department == null && position == null && minSalary == null && 
            maxSalary == null && name == null && email == null) {
            return employeeService.getAllEmployeesPaginated(pageable).getContent();
        }
        
        // Apply filters
        List<Employee> filteredEmployees = new ArrayList<>();
        
        if (email != null) {
            // Single employee by email
            Optional<Employee> employee = employeeService.getEmployeeByEmail(email);
            if (employee.isPresent()) {
                filteredEmployees.add(employee.get());
            }
        } else if (name != null) {
            // Search by name
            filteredEmployees = employeeService.searchEmployeesByName(name);
        } else if (department != null && position != null) {
            // Filter by department and position
            filteredEmployees = employeeService.findByDepartmentAndPosition(department, position);
        } else if (department != null) {
            // Filter by department
            filteredEmployees = employeeService.getEmployeesByDepartment(department);
        } else if (position != null) {
            // Filter by position
            filteredEmployees = employeeService.getEmployeesByPosition(position);
        } else if (minSalary != null || maxSalary != null) {
            // Filter by salary range
            if (minSalary != null) {
                filteredEmployees = employeeService.getEmployeesWithSalaryGreaterThan(minSalary);
            }
            // Apply max salary filter if specified
            if (maxSalary != null) {
                filteredEmployees = filteredEmployees.stream()
                    .filter(emp -> emp.getSalary() != null && emp.getSalary() <= maxSalary)
                    .collect(Collectors.toList());
            }
        }
        
        // Apply pagination manually for filtered results
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredEmployees.size());
        
        if (start < filteredEmployees.size()) {
            return filteredEmployees.subList(start, end);
        }
        
        return new ArrayList<>();
    }
    
    // Helper method to generate CSV content with selected fields
    private byte[] generateCsvContentWithFields(List<Employee> employees, String fields) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
        
        // Parse selected fields
        String[] selectedFields = fields.split(",");
        
        // Write CSV header with selected fields
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
        return baos.toByteArray();
    }
    
    // Helper method to get field display name
    private String getFieldDisplayName(String field) {
        switch (field.toLowerCase()) {
            case "id": return "ID";
            case "firstname": return "First Name";
            case "lastname": return "Last Name";
            case "email": return "Email";
            case "phonenumber": return "Phone Number";
            case "dateofbirth": return "Date of Birth";
            case "hiredate": return "Hire Date";
            case "salary": return "Salary";
            case "position": return "Position";
            case "department": return "Department";
            case "createdat": return "Created At";
            case "updatedat": return "Updated At";
            default: return field;
        }
    }
    
    // Helper method to get field value
    private String getFieldValue(Employee employee, String field, DateTimeFormatter dateFormatter, DateTimeFormatter dateTimeFormatter) {
        switch (field.toLowerCase()) {
            case "id": return String.valueOf(employee.getId());
            case "firstname": return employee.getFirstName();
            case "lastname": return employee.getLastName();
            case "email": return employee.getEmail();
            case "phonenumber": return employee.getPhoneNumber();
            case "dateofbirth": 
                return employee.getDateOfBirth() != null ? employee.getDateOfBirth().format(dateFormatter) : "";
            case "hiredate": 
                return employee.getHireDate() != null ? employee.getHireDate().format(dateFormatter) : "";
            case "salary": 
                return employee.getSalary() != null ? String.format("%.2f", employee.getSalary()) : "";
            case "position": return employee.getPosition();
            case "department": return employee.getDepartment();
            case "createdat": 
                return employee.getCreatedAt() != null ? employee.getCreatedAt().format(dateTimeFormatter) : "";
            case "updatedat": 
                return employee.getUpdatedAt() != null ? employee.getUpdatedAt().format(dateTimeFormatter) : "";
            default: return "";
        }
    }
    
    // Helper method to generate filename based on filters
    private String generateFileName(String department, String position, Double minSalary, Double maxSalary, String name, String email) {
        StringBuilder fileName = new StringBuilder("employees_export");
        
        if (email != null) {
            fileName.append("_email_").append(email.replace("@", "_at_"));
        } else if (name != null) {
            fileName.append("_name_").append(name.replace(" ", "_"));
        } else if (department != null) {
            fileName.append("_dept_").append(department);
        } else if (position != null) {
            fileName.append("_pos_").append(position);
        } else if (minSalary != null || maxSalary != null) {
            fileName.append("_salary");
            if (minSalary != null) fileName.append("_min_").append(minSalary.intValue());
            if (maxSalary != null) fileName.append("_max_").append(maxSalary.intValue());
        }
        
        fileName.append("_").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))).append(".csv");
        return fileName.toString();
    }
    
    // Helper method to escape CSV fields
    private String escapeCsvField(String field) {
        if (field == null) return "";
        return field.replace("\"", "\"\""); // Escape quotes by doubling them
    }
} 