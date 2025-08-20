package com.example.employee_managment.repository;

import com.example.employee_managment.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Employee employee1;
    private Employee employee2;
    private Employee employee3;

    @BeforeEach
    void setUp() {
        // Create test employees
        employee1 = new Employee("John", "Doe", "john.doe@example.com");
        employee1.setPosition("Software Engineer");
        employee1.setDepartment("Engineering");
        employee1.setSalary(75000.0);
        employee1.setHireDate(LocalDate.now().minusYears(2));

        employee2 = new Employee("Jane", "Smith", "jane.smith@example.com");
        employee2.setPosition("Product Manager");
        employee2.setDepartment("Product");
        employee2.setSalary(85000.0);
        employee2.setHireDate(LocalDate.now().minusYears(1));

        employee3 = new Employee("Bob", "Johnson", "bob.johnson@example.com");
        employee3.setPosition("Software Engineer");
        employee3.setDepartment("Engineering");
        employee3.setSalary(80000.0);
        employee3.setHireDate(LocalDate.now().minusMonths(6));

        // Persist employees
        entityManager.persistAndFlush(employee1);
        entityManager.persistAndFlush(employee2);
        entityManager.persistAndFlush(employee3);
    }

    @Test
    void testSaveEmployee() {
        Employee newEmployee = new Employee("Alice", "Brown", "alice.brown@example.com");
        newEmployee.setPosition("Designer");
        newEmployee.setDepartment("Design");

        Employee savedEmployee = employeeRepository.save(newEmployee);
        
        assertNotNull(savedEmployee.getId());
        assertEquals("Alice", savedEmployee.getFirstName());
        assertEquals("Brown", savedEmployee.getLastName());
        assertEquals("alice.brown@example.com", savedEmployee.getEmail());
    }

    @Test
    void testFindById() {
        Optional<Employee> found = employeeRepository.findById(employee1.getId());
        
        assertTrue(found.isPresent());
        assertEquals("John", found.get().getFirstName());
        assertEquals("Doe", found.get().getLastName());
    }

    @Test
    void testFindByIdNotFound() {
        Optional<Employee> found = employeeRepository.findById(999L);
        assertFalse(found.isPresent());
    }

    @Test
    void testFindAll() {
        List<Employee> allEmployees = employeeRepository.findAll();
        
        assertNotNull(allEmployees);
        assertTrue(allEmployees.size() >= 3);
        assertTrue(allEmployees.stream().anyMatch(e -> e.getEmail().equals("john.doe@example.com")));
        assertTrue(allEmployees.stream().anyMatch(e -> e.getEmail().equals("jane.smith@example.com")));
        assertTrue(allEmployees.stream().anyMatch(e -> e.getEmail().equals("bob.johnson@example.com")));
    }

    @Test
    void testFindByEmail() {
        Optional<Employee> found = employeeRepository.findByEmail("john.doe@example.com");
        
        assertTrue(found.isPresent());
        assertEquals("John", found.get().getFirstName());
        assertEquals("Doe", found.get().getLastName());
    }

    @Test
    void testFindByEmailNotFound() {
        Optional<Employee> found = employeeRepository.findByEmail("nonexistent@example.com");
        assertFalse(found.isPresent());
    }

    @Test
    void testFindByDepartment() {
        List<Employee> engineeringEmployees = employeeRepository.findByDepartment("Engineering");
        
        assertNotNull(engineeringEmployees);
        assertEquals(2, engineeringEmployees.size());
        assertTrue(engineeringEmployees.stream().allMatch(e -> "Engineering".equals(e.getDepartment())));
    }

    @Test
    void testFindByPosition() {
        List<Employee> softwareEngineers = employeeRepository.findByPosition("Software Engineer");
        
        assertNotNull(softwareEngineers);
        assertEquals(2, softwareEngineers.size());
        assertTrue(softwareEngineers.stream().allMatch(e -> "Software Engineer".equals(e.getPosition())));
    }

    @Test
    void testFindByFirstNameContainingIgnoreCase() {
        List<Employee> johnEmployees = employeeRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("john", "john");
        
        assertNotNull(johnEmployees);
        assertTrue(johnEmployees.size() >= 1);
        assertTrue(johnEmployees.stream().anyMatch(e -> 
            e.getFirstName().toLowerCase().contains("john") || 
            e.getLastName().toLowerCase().contains("john")));
    }

    @Test
    void testFindEmployeesWithSalaryGreaterThan() {
        List<Employee> highSalaryEmployees = employeeRepository.findEmployeesWithSalaryGreaterThan(75000.0);
        
        assertNotNull(highSalaryEmployees);
        assertTrue(highSalaryEmployees.size() >= 2);
        assertTrue(highSalaryEmployees.stream().allMatch(e -> e.getSalary() > 75000.0));
    }

    @Test
    void testExistsByEmail() {
        assertTrue(employeeRepository.existsByEmail("john.doe@example.com"));
        assertFalse(employeeRepository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    void testFindByDepartmentAndPosition() {
        List<Employee> engineeringSoftwareEngineers = employeeRepository
                .findByDepartmentAndPosition("Engineering", "Software Engineer");
        
        assertNotNull(engineeringSoftwareEngineers);
        assertEquals(2, engineeringSoftwareEngineers.size());
        assertTrue(engineeringSoftwareEngineers.stream().allMatch(e -> 
            "Engineering".equals(e.getDepartment()) && "Software Engineer".equals(e.getPosition())));
    }

    @Test
    void testUpdateEmployee() {
        Employee employeeToUpdate = employeeRepository.findById(employee1.getId()).orElse(null);
        assertNotNull(employeeToUpdate);
        
        employeeToUpdate.setSalary(80000.0);
        employeeToUpdate.setPosition("Senior Software Engineer");
        
        Employee updatedEmployee = employeeRepository.save(employeeToUpdate);
        
        assertEquals(80000.0, updatedEmployee.getSalary());
        assertEquals("Senior Software Engineer", updatedEmployee.getPosition());
    }

    @Test
    void testDeleteEmployee() {
        Long employeeId = employee1.getId();
        assertTrue(employeeRepository.findById(employeeId).isPresent());
        
        employeeRepository.deleteById(employeeId);
        
        assertFalse(employeeRepository.findById(employeeId).isPresent());
    }

    @Test
    void testCountEmployees() {
        long count = employeeRepository.count();
        assertTrue(count >= 3);
    }
} 