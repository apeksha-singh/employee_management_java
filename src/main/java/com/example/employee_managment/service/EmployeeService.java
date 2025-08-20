package com.example.employee_managment.service;

import com.example.employee_managment.model.Employee;
import com.example.employee_managment.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    // Create a new employee
    public Employee createEmployee(Employee employee) {
        // Check if email already exists
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new RuntimeException("Employee with email " + employee.getEmail() + " already exists");
        }
        return employeeRepository.save(employee);
    }
    
    // Get all employees with pagination
    public Page<Employee> getAllEmployeesPaginated(Pageable pageable) {
        return employeeRepository.findAll(pageable);
    }
    
    // Get all employees (deprecated - use paginated version for large datasets)
    @Deprecated
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }
    
    // Get employee by ID
    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }
    
    // Get employee by email
    public Optional<Employee> getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email);
    }
    
    // Update employee
    public Employee updateEmployee(Long id, Employee employeeDetails) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
        
        // Update only non-null fields to preserve existing values
        if (employeeDetails.getFirstName() != null) {
            employee.setFirstName(employeeDetails.getFirstName());
        }
        if (employeeDetails.getLastName() != null) {
            employee.setLastName(employeeDetails.getLastName());
        }
        if (employeeDetails.getPhoneNumber() != null) {
            employee.setPhoneNumber(employeeDetails.getPhoneNumber());
        }
        if (employeeDetails.getDateOfBirth() != null) {
            employee.setDateOfBirth(employeeDetails.getDateOfBirth());
        }
        if (employeeDetails.getHireDate() != null) {
            employee.setHireDate(employeeDetails.getHireDate());
        }
        if (employeeDetails.getSalary() != null) {
            employee.setSalary(employeeDetails.getSalary());
        }
        if (employeeDetails.getPosition() != null) {
            employee.setPosition(employeeDetails.getPosition());
        }
        if (employeeDetails.getDepartment() != null) {
            employee.setDepartment(employeeDetails.getDepartment());
        }
        
        return employeeRepository.save(employee);
    }
    
    // Delete employee
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
    }
    
    // Get employees by department
    public List<Employee> getEmployeesByDepartment(String department) {
        return employeeRepository.findByDepartment(department);
    }
    
    // Get employees by position
    public List<Employee> getEmployeesByPosition(String position) {
        return employeeRepository.findByPosition(position);
    }
    
    // Search employees by name
    public List<Employee> searchEmployeesByName(String name) {
        return employeeRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(name, name);
    }
    
    // Get employees with salary greater than
    public List<Employee> getEmployeesWithSalaryGreaterThan(Double minSalary) {
        return employeeRepository.findEmployeesWithSalaryGreaterThan(minSalary);
    }
} 