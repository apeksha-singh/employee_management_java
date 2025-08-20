package com.example.employee_managment.service;

import com.example.employee_managment.model.Employee;
import com.example.employee_managment.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee1;
    private Employee employee2;
    private Employee employee3;

    @BeforeEach
    void setUp() {
        employee1 = new Employee("John", "Doe", "john.doe@example.com");
        employee1.setId(1L);
        employee1.setPosition("Software Engineer");
        employee1.setDepartment("Engineering");
        employee1.setSalary(75000.0);
        employee1.setHireDate(LocalDate.now().minusYears(2));

        employee2 = new Employee("Jane", "Smith", "jane.smith@example.com");
        employee2.setId(2L);
        employee2.setPosition("Product Manager");
        employee2.setDepartment("Product");
        employee2.setSalary(85000.0);
        employee2.setHireDate(LocalDate.now().minusYears(1));

        employee3 = new Employee("Bob", "Johnson", "bob.johnson@example.com");
        employee3.setId(3L);
        employee3.setPosition("Software Engineer");
        employee3.setDepartment("Engineering");
        employee3.setSalary(80000.0);
        employee3.setHireDate(LocalDate.now().minusMonths(6));
    }

    @Test
    void testCreateEmployeeSuccess() {
        // Arrange
        Employee newEmployee = new Employee("Alice", "Brown", "alice.brown@example.com");
        when(employeeRepository.existsByEmail(anyString())).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(newEmployee);

        // Act
        Employee result = employeeService.createEmployee(newEmployee);

        // Assert
        assertNotNull(result);
        assertEquals("Alice", result.getFirstName());
        assertEquals("Brown", result.getLastName());
        assertEquals("alice.brown@example.com", result.getEmail());
        verify(employeeRepository).existsByEmail("alice.brown@example.com");
        verify(employeeRepository).save(newEmployee);
    }

    @Test
    void testCreateEmployeeWithDuplicateEmail() {
        // Arrange
        Employee newEmployee = new Employee("Alice", "Brown", "alice.brown@example.com");
        when(employeeRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> employeeService.createEmployee(newEmployee));
        
        assertEquals("Employee with email alice.brown@example.com already exists", exception.getMessage());
        verify(employeeRepository).existsByEmail("alice.brown@example.com");
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testGetAllEmployees() {
        // Arrange
        List<Employee> expectedEmployees = Arrays.asList(employee1, employee2, employee3);
        when(employeeRepository.findAll()).thenReturn(expectedEmployees);

        // Act
        List<Employee> result = employeeService.getAllEmployees();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expectedEmployees, result);
        verify(employeeRepository).findAll();
    }

    @Test
    void testGetEmployeeByIdFound() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));

        // Act
        Optional<Employee> result = employeeService.getEmployeeById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(employee1, result.get());
        verify(employeeRepository).findById(1L);
    }

    @Test
    void testGetEmployeeByIdNotFound() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Employee> result = employeeService.getEmployeeById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(employeeRepository).findById(999L);
    }

    @Test
    void testGetEmployeeByEmailFound() {
        // Arrange
        when(employeeRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(employee1));

        // Act
        Optional<Employee> result = employeeService.getEmployeeByEmail("john.doe@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(employee1, result.get());
        verify(employeeRepository).findByEmail("john.doe@example.com");
    }

    @Test
    void testGetEmployeeByEmailNotFound() {
        // Arrange
        when(employeeRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        Optional<Employee> result = employeeService.getEmployeeByEmail("nonexistent@example.com");

        // Assert
        assertFalse(result.isPresent());
        verify(employeeRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void testUpdateEmployeeSuccess() {
        // Arrange
        Employee updateDetails = new Employee();
        updateDetails.setFirstName("John Updated");
        updateDetails.setLastName("Doe Updated");
        updateDetails.setSalary(80000.0);
        updateDetails.setPosition("Senior Software Engineer");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee1);

        // Act
        Employee result = employeeService.updateEmployee(1L, updateDetails);

        // Assert
        assertNotNull(result);
        assertEquals("John Updated", result.getFirstName());
        assertEquals("Doe Updated", result.getLastName());
        assertEquals(80000.0, result.getSalary());
        assertEquals("Senior Software Engineer", result.getPosition());
        verify(employeeRepository).findById(1L);
        verify(employeeRepository).save(employee1);
    }

    @Test
    void testUpdateEmployeeNotFound() {
        // Arrange
        Employee updateDetails = new Employee();
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> employeeService.updateEmployee(999L, updateDetails));
        
        assertEquals("Employee not found with id: 999", exception.getMessage());
        verify(employeeRepository).findById(999L);
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void testDeleteEmployeeSuccess() {
        // Arrange
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));
        doNothing().when(employeeRepository).delete(employee1);

        // Act
        assertDoesNotThrow(() -> employeeService.deleteEmployee(1L));

        // Assert
        verify(employeeRepository).findById(1L);
        verify(employeeRepository).delete(employee1);
    }

    @Test
    void testDeleteEmployeeNotFound() {
        // Arrange
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> employeeService.deleteEmployee(999L));
        
        assertEquals("Employee not found with id: 999", exception.getMessage());
        verify(employeeRepository).findById(999L);
        verify(employeeRepository, never()).delete(any(Employee.class));
    }

    @Test
    void testGetEmployeesByDepartment() {
        // Arrange
        List<Employee> expectedEmployees = Arrays.asList(employee1, employee3);
        when(employeeRepository.findByDepartment("Engineering")).thenReturn(expectedEmployees);

        // Act
        List<Employee> result = employeeService.getEmployeesByDepartment("Engineering");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> "Engineering".equals(e.getDepartment())));
        verify(employeeRepository).findByDepartment("Engineering");
    }

    @Test
    void testGetEmployeesByPosition() {
        // Arrange
        List<Employee> expectedEmployees = Arrays.asList(employee1, employee3);
        when(employeeRepository.findByPosition("Software Engineer")).thenReturn(expectedEmployees);

        // Act
        List<Employee> result = employeeService.getEmployeesByPosition("Software Engineer");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> "Software Engineer".equals(e.getPosition())));
        verify(employeeRepository).findByPosition("Software Engineer");
    }

    @Test
    void testSearchEmployeesByName() {
        // Arrange
        List<Employee> expectedEmployees = Arrays.asList(employee1);
        when(employeeRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("john", "john"))
            .thenReturn(expectedEmployees);

        // Act
        List<Employee> result = employeeService.searchEmployeesByName("john");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(employee1, result.get(0));
        verify(employeeRepository).findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("john", "john");
    }

    @Test
    void testGetEmployeesWithSalaryGreaterThan() {
        // Arrange
        List<Employee> expectedEmployees = Arrays.asList(employee2, employee3);
        when(employeeRepository.findEmployeesWithSalaryGreaterThan(75000.0)).thenReturn(expectedEmployees);

        // Act
        List<Employee> result = employeeService.getEmployeesWithSalaryGreaterThan(75000.0);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> e.getSalary() > 75000.0));
        verify(employeeRepository).findEmployeesWithSalaryGreaterThan(75000.0);
    }

    @Test
    void testUpdateEmployeePreservesExistingFields() {
        // Arrange
        Employee updateDetails = new Employee();
        updateDetails.setFirstName("John Updated");
        // Only updating firstName, other fields should remain unchanged

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee1));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee1);

        // Act
        Employee result = employeeService.updateEmployee(1L, updateDetails);

        // Assert
        assertEquals("John Updated", result.getFirstName());
        // These fields should remain unchanged from the original employee1
        assertEquals("Doe", result.getLastName()); 
        assertEquals("john.doe@example.com", result.getEmail()); 
        assertEquals("Engineering", result.getDepartment()); 
    }
} 