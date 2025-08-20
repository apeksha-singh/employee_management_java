package com.example.employee_managment.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeTest {

    private Validator validator;
    private Employee employee;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        employee = new Employee();
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john.doe@example.com");
    }

    @Test
    void testDefaultConstructor() {
        Employee emp = new Employee();
        assertNotNull(emp);
        assertNotNull(emp.getCreatedAt());
        assertNotNull(emp.getUpdatedAt());
    }

    @Test
    void testParameterizedConstructor() {
        Employee emp = new Employee("Jane", "Smith", "jane.smith@example.com");
        
        assertEquals("Jane", emp.getFirstName());
        assertEquals("Smith", emp.getLastName());
        assertEquals("jane.smith@example.com", emp.getEmail());
        assertNotNull(emp.getCreatedAt());
        assertNotNull(emp.getUpdatedAt());
    }

    @Test
    void testValidEmployee() {
        Set<ConstraintViolation<Employee>> violations = validator.validate(employee);
        assertTrue(violations.isEmpty(), "Employee should be valid");
    }

    @Test
    void testFirstNameRequired() {
        employee.setFirstName(null);
        Set<ConstraintViolation<Employee>> violations = validator.validate(employee);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("firstName")));
    }

    @Test
    void testFirstNameNotBlank() {
        employee.setFirstName("");
        Set<ConstraintViolation<Employee>> violations = validator.validate(employee);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("firstName")));
    }

    @Test
    void testLastNameRequired() {
        employee.setLastName(null);
        Set<ConstraintViolation<Employee>> violations = validator.validate(employee);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("lastName")));
    }

    @Test
    void testEmailRequired() {
        // Test with null email
        employee.setEmail(null);
        Set<ConstraintViolation<Employee>> violations = validator.validate(employee);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        
        // Test with empty string - @NotBlank will reject this
        employee.setEmail("");
        violations = validator.validate(employee);
        assertFalse(violations.isEmpty(), "Empty email should be rejected with @NotBlank annotation");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
        
        // Test with blank string (spaces only) - @NotBlank will reject this
        employee.setEmail("   ");
        violations = validator.validate(employee);
        assertFalse(violations.isEmpty(), "Blank email should be rejected with @NotBlank annotation");
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void testEmailFormat() {
        employee.setEmail("invalid-email");
        Set<ConstraintViolation<Employee>> violations = validator.validate(employee);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void testValidEmailFormats() {
        String[] validEmails = {
            "test@example.com",
            "user.name@domain.co.uk",
            "user+tag@example.org"
        };
        
        for (String email : validEmails) {
            employee.setEmail(email);
            Set<ConstraintViolation<Employee>> violations = validator.validate(employee);
            assertTrue(violations.isEmpty(), "Email " + email + " should be valid");
        }
    }

    @Test
    void testDateOfBirthPast() {
        employee.setDateOfBirth(LocalDate.now().plusDays(1)); // Future date
        Set<ConstraintViolation<Employee>> violations = validator.validate(employee);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dateOfBirth")));
    }

    @Test
    void testValidDateOfBirth() {
        employee.setDateOfBirth(LocalDate.now().minusYears(25)); // Past date
        Set<ConstraintViolation<Employee>> violations = validator.validate(employee);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        LocalDate dob = LocalDate.of(1990, 5, 15);
        LocalDate hireDate = LocalDate.now();
        Double salary = 75000.0;
        String position = "Software Engineer";
        String department = "Engineering";
        String phoneNumber = "+1-555-0123";

        employee.setDateOfBirth(dob);
        employee.setHireDate(hireDate);
        employee.setSalary(salary);
        employee.setPosition(position);
        employee.setDepartment(department);
        employee.setPhoneNumber(phoneNumber);

        assertEquals(dob, employee.getDateOfBirth());
        assertEquals(hireDate, employee.getHireDate());
        assertEquals(salary, employee.getSalary());
        assertEquals(position, employee.getPosition());
        assertEquals(department, employee.getDepartment());
        assertEquals(phoneNumber, employee.getPhoneNumber());
    }

    @Test
    void testToString() {
        String toString = employee.toString();
        assertTrue(toString.contains("John"));
        assertTrue(toString.contains("Doe"));
        assertTrue(toString.contains("john.doe@example.com"));
    }

    @Test
    void testIdGeneration() {
        assertNull(employee.getId());
        employee.setId(1L);
        assertEquals(1L, employee.getId());
    }

    @Test
    void testTimestamps() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Employee emp = new Employee();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        
        assertTrue(emp.getCreatedAt().isAfter(before) || emp.getCreatedAt().equals(before));
        assertTrue(emp.getCreatedAt().isBefore(after) || emp.getCreatedAt().equals(after));
    }
} 