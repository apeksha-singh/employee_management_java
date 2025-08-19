package com.example.employee_managment.repository;

import com.example.employee_managment.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    // Find employee by email
    Optional<Employee> findByEmail(String email);
    
    // Find employees by department
    List<Employee> findByDepartment(String department);
    
    // Find employees by position
    List<Employee> findByPosition(String position);
    
    // Find employees by first name or last name (case-insensitive)
    List<Employee> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName);
    
    // Custom query to find employees with salary greater than given amount
    @Query("SELECT e FROM Employee e WHERE e.salary > :minSalary")
    List<Employee> findEmployeesWithSalaryGreaterThan(@Param("minSalary") Double minSalary);
    
    // Check if email exists
    boolean existsByEmail(String email);
    
    // Find employees by department and position
    List<Employee> findByDepartmentAndPosition(String department, String position);
} 