package com.example.employee_managment.controller;

import com.example.employee_managment.model.Employee;
import com.example.employee_managment.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Autowired
    private ObjectMapper objectMapper;

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
    void testCreateEmployeeSuccess() throws Exception {
        // Arrange
        Employee newEmployee = new Employee("Alice", "Brown", "alice.brown@example.com");
        newEmployee.setPosition("Designer");
        newEmployee.setDepartment("Design");

        when(employeeService.createEmployee(any(Employee.class))).thenReturn(newEmployee);

        // Act & Assert
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newEmployee)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Brown"))
                .andExpect(jsonPath("$.email").value("alice.brown@example.com"));

        verify(employeeService).createEmployee(any(Employee.class));
    }

    @Test
    void testCreateEmployeeValidationFailure() throws Exception {
        // Arrange
        Employee invalidEmployee = new Employee();
        // Missing required fields - this should fail validation

        // Act & Assert
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest());

        verify(employeeService, never()).createEmployee(any(Employee.class));
    }

    @Test
    void testCreateEmployeeServiceException() throws Exception {
        // Arrange
        Employee newEmployee = new Employee("Alice", "Brown", "alice.brown@example.com");
        when(employeeService.createEmployee(any(Employee.class)))
                .thenThrow(new RuntimeException("Employee with email already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newEmployee)))
                .andExpect(status().isBadRequest());

        verify(employeeService).createEmployee(any(Employee.class));
    }

    @Test
    void testGetAllEmployees() throws Exception {
        // Arrange
        List<Employee> employees = Arrays.asList(employee1, employee2, employee3);
        when(employeeService.getAllEmployees()).thenReturn(employees);

        // Act & Assert
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[1].firstName").value("Jane"))
                .andExpect(jsonPath("$[2].firstName").value("Bob"));

        verify(employeeService).getAllEmployees();
    }

    @Test
    void testGetEmployeeByIdFound() throws Exception {
        // Arrange
        when(employeeService.getEmployeeById(1L)).thenReturn(Optional.of(employee1));

        // Act & Assert
        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        verify(employeeService).getEmployeeById(1L);
    }

    @Test
    void testGetEmployeeByIdNotFound() throws Exception {
        // Arrange
        when(employeeService.getEmployeeById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/employees/999"))
                .andExpect(status().isNotFound());

        verify(employeeService).getEmployeeById(999L);
    }

    @Test
    void testGetEmployeeByEmailFound() throws Exception {
        // Arrange
        when(employeeService.getEmployeeByEmail("john.doe@example.com")).thenReturn(Optional.of(employee1));

        // Act & Assert
        mockMvc.perform(get("/api/employees/email/john.doe@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        verify(employeeService).getEmployeeByEmail("john.doe@example.com");
    }

    @Test
    void testGetEmployeeByEmailNotFound() throws Exception {
        // Arrange
        when(employeeService.getEmployeeByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/employees/email/nonexistent@example.com"))
                .andExpect(status().isNotFound());

        verify(employeeService).getEmployeeByEmail("nonexistent@example.com");
    }

    @Test
    void testUpdateEmployeeSuccess() throws Exception {
        // Arrange
        Employee updateDetails = new Employee("John Updated", "Doe Updated", "john.updated@example.com");
        updateDetails.setSalary(80000.0);
        updateDetails.setPosition("Senior Software Engineer");

        when(employeeService.updateEmployee(eq(1L), any(Employee.class))).thenReturn(employee1);

        // Act & Assert
        mockMvc.perform(put("/api/employees/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        verify(employeeService).updateEmployee(eq(1L), any(Employee.class));
    }

    @Test
    void testUpdateEmployeeNotFound() throws Exception {
        // Arrange
        Employee updateDetails = new Employee("John Updated", "Doe Updated", "john.updated@example.com");

        when(employeeService.updateEmployee(eq(999L), any(Employee.class)))
                .thenThrow(new RuntimeException("Employee not found with id: 999"));

        // Act & Assert
        mockMvc.perform(put("/api/employees/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDetails)))
                .andExpect(status().isNotFound()); // Changed back to isNotFound() since controller returns 404

        // Verify the service was called
        verify(employeeService, times(1)).updateEmployee(eq(999L), any(Employee.class));
    }

    @Test
    void testDeleteEmployeeSuccess() throws Exception {
        // Arrange
        doNothing().when(employeeService).deleteEmployee(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());

        verify(employeeService).deleteEmployee(1L);
    }

    @Test
    void testDeleteEmployeeNotFound() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Employee not found with id: 999"))
                .when(employeeService).deleteEmployee(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/employees/999"))
                .andExpect(status().isNotFound());

        verify(employeeService).deleteEmployee(999L);
    }

    @Test
    void testGetEmployeesByDepartment() throws Exception {
        // Arrange
        List<Employee> engineeringEmployees = Arrays.asList(employee1, employee3);
        when(employeeService.getEmployeesByDepartment("Engineering")).thenReturn(engineeringEmployees);

        // Act & Assert
        mockMvc.perform(get("/api/employees/department/Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].department").value("Engineering"))
                .andExpect(jsonPath("$[1].department").value("Engineering"));

        verify(employeeService).getEmployeesByDepartment("Engineering");
    }

    @Test
    void testGetEmployeesByPosition() throws Exception {
        // Arrange
        List<Employee> softwareEngineers = Arrays.asList(employee1, employee3);
        when(employeeService.getEmployeesByPosition("Software Engineer")).thenReturn(softwareEngineers);

        // Act & Assert
        mockMvc.perform(get("/api/employees/position/Software Engineer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].position").value("Software Engineer"))
                .andExpect(jsonPath("$[1].position").value("Software Engineer"));

        verify(employeeService).getEmployeesByPosition("Software Engineer");
    }

    @Test
    void testSearchEmployeesByName() throws Exception {
        // Arrange
        List<Employee> searchResults = Arrays.asList(employee1);
        when(employeeService.searchEmployeesByName("john")).thenReturn(searchResults);

        // Act & Assert
        mockMvc.perform(get("/api/employees/search")
                .param("name", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("John"));

        verify(employeeService).searchEmployeesByName("john");
    }

    @Test
    void testGetEmployeesWithSalaryGreaterThan() throws Exception {
        // Arrange
        List<Employee> highSalaryEmployees = Arrays.asList(employee2, employee3);
        when(employeeService.getEmployeesWithSalaryGreaterThan(75000.0)).thenReturn(highSalaryEmployees);

        // Act & Assert
        mockMvc.perform(get("/api/employees/salary")
                .param("min", "75000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(employeeService).getEmployeesWithSalaryGreaterThan(75000.0);
    }

    @Test
    void testCreateEmployeeWithInvalidEmail() throws Exception {
        // Arrange
        Employee invalidEmployee = new Employee("John", "Doe", "invalid-email");
        invalidEmployee.setPosition("Engineer");
        invalidEmployee.setDepartment("Engineering");

        // Act & Assert
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest());

        verify(employeeService, never()).createEmployee(any(Employee.class));
    }

    @Test
    void testCreateEmployeeWithFutureDateOfBirth() throws Exception {
        // Arrange
        Employee invalidEmployee = new Employee("John", "Doe", "john.doe@example.com");
        invalidEmployee.setDateOfBirth(LocalDate.now().plusDays(1)); // Future date

        // Act & Assert
        mockMvc.perform(post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmployee)))
                .andExpect(status().isBadRequest());

        verify(employeeService, never()).createEmployee(any(Employee.class));
    }
} 