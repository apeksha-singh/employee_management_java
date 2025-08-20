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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "1000") int size,
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
            
            Page<Employee> employeePage = employeeService.getAllEmployeesPaginated(pageable);
            
            // Generate CSV content
            byte[] csvContent = generateCsvContent(employeePage.getContent());
            
            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "employees_page_" + page + ".csv");
            headers.setContentLength(csvContent.length);
            
            return new ResponseEntity<>(csvContent, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Helper method to generate CSV content
    private byte[] generateCsvContent(List<Employee> employees) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
        
        // Write CSV header
        writer.write("ID,First Name,Last Name,Email,Phone Number,Date of Birth,Hire Date,Salary,Position,Department,Created At,Updated At\n");
        
        // Write data rows
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
        
        writer.flush();
        writer.close();
        return baos.toByteArray();
    }
    
    // Helper method to escape CSV fields
    private String escapeCsvField(String field) {
        if (field == null) return "";
        return field.replace("\"", "\"\""); // Escape quotes by doubling them
    }
} 