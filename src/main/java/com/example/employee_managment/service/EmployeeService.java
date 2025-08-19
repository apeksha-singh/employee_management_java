package com.example.employee_managment.service;

import com.example.employee_managment.model.Employee;
import com.example.employee_managment.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    // Get all employees
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
        
        // Update fields
        employee.setFirstName(employeeDetails.getFirstName());
        employee.setLastName(employeeDetails.getLastName());
        employee.setPhoneNumber(employeeDetails.getPhoneNumber());
        employee.setDateOfBirth(employeeDetails.getDateOfBirth());
        employee.setHireDate(employeeDetails.getHireDate());
        employee.setSalary(employeeDetails.getSalary());
        employee.setPosition(employeeDetails.getPosition());
        employee.setDepartment(employeeDetails.getDepartment());
        
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