# Employee Management System

A Spring Boot learning project to understand Java and Spring framework concepts.

## What We're Building

A simple employee management system that handles:
- Employee information
- Department details  
- Organization structure
- Basic CRUD operations

## Technologies Used

- **Java 17** - Core programming language
- **Spring Boot 3.2.0** - Framework for building Java applications
- **Spring Data JPA** - Database operations
- **MySQL** - Database to store data
- **Maven** - Build tool and dependency management
- **RESTful APIs** - Web services for frontend communication

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/employeemanagement/
│   │       ├── controllers/     # REST API endpoints
│   │       ├── models/          # Data entities
│   │       ├── repositories/    # Database access layer
│   │       └── services/        # Business logic
│   └── resources/
│       └── application.properties  # Database configuration
└── test/                        # Unit tests
```

## Learning Goals

- Understand Java object-oriented programming
- Learn Spring Boot framework basics
- Practice database operations with JPA
- Build RESTful web services
- Handle HTTP requests and responses

## Getting Started

1. Ensure Java 17 is installed
2. Install and start MySQL server
3. Create database: `employee_db`
4. Run the application
5. Test APIs using Postman or browser

## Next Steps

- Add more entities (Salary, Projects, etc.)
- Implement user authentication
- Add input validation
- Create unit tests
- Build a simple frontend 