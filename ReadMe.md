### Development Commands
- **Start application**: `mvn spring-boot:run`
- **Build project**: `mvn clean compile`
- **Package application**: `mvn clean package`
- **Run tests**: `mvn test`
- **Run specific test**: `mvn test -Dtest=ClassNameTest`
- **Clean build artifacts**: `mvn clean`

### Development Server
- **Application URL**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/api/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/api/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/api/v3/api-docs.yaml
- **Actuator Health**: http://localhost:8080/api/actuator/health
- **Flowable Metrics**: http://localhost:8080/api/actuator/flowable
- **Application Info**: http://localhost:8080/api/actuator/info

### Test User Authentication
- **Username**: jow (or admin for Postman collection)
- **Password**: demo123
- **Login endpoint**: POST `/auth/login`
- Use returned JWT token in Authorization header: `Bearer <token>`

## API Documentation & Testing

### OpenAPI 3.0 Specification
The application provides comprehensive OpenAPI 3.0 documentation configured in `OpenApiConfig.java`:

#### API Information
- **Title**: "CMS Flowable Workflow Management API"
- **Version**: "v1.0.0"
- **Description**: Complete case management with Flowable workflow integration
- **Contact**: cms-dev@company.com
- **License**: MIT License

#### Security Configuration
- **Authentication**: JWT Bearer Token
- **Format**: `Authorization: Bearer <jwt_token>`
- **Global Security**: Applied to all endpoints except public ones
- **Public Endpoints**: `/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/**`

#### Environment Servers
- **Development**: `http://localhost:8080/api`
- **Staging**: `https://staging-api.cms-flowable.com/api`
- **Production**: `https://api.cms-flowable.com/api`

### Postman Collections
The repository includes comprehensive Postman testing collections:

#### Primary Collection: `CMS_Flowable_API_Tests.postman_collection.json`
- **Version**: 1.0.0
- **Description**: Comprehensive API testing with automated JWT token management
- **Environment**: Uses `CMS_Flowable_Environment.postman_environment.json`

#### Collection Structure:

**1. Authentication Group**
- `POST /auth/login` - User authentication with auto-token extraction
- Test credentials: `admin` / `password123`
- Auto-sets JWT token for subsequent requests

**2. Cases Management Group**
- `POST /v1/cases` - Create case with multiple allegations
- `GET /v1/cases/{case_number}` - Retrieve case details
- Three sample case types:
  - **Workplace Harassment** (HIGH priority, multi-allegation)
  - **Contract Violation** (MEDIUM priority, Legal department)
  - **Policy Violation** (LOW priority, HR department)

**3. Workflow Management Group**
- `GET /v1/workflow/tasks/active` - Get active workflow tasks
- `GET /v1/workflow/tasks/user/{userId}` - Get user-specific tasks
- `POST /v1/cases/tasks/complete` - Complete workflow tasks with variables
- Three department completion examples:
  - **HR Review** - Initial case evaluation
  - **Legal Review** - Legal risk assessment
  - **CSIS Investigation** - Security investigation completion

**4. System Health Group**
- `GET /actuator/health` - Application health status
- `GET /actuator/info` - Application metadata
- `GET /actuator/flowable` - Flowable engine metrics

#### Environment Variables
The collection uses these dynamic variables:
- `{{base_url}}` - API base URL (http://localhost:8080/api)
- `{{jwt_token}}` - Auto-extracted JWT token from login
- `{{case_number}}` - Auto-extracted case number from case creation
- `{{task_id}}` - Auto-extracted task ID for workflow operations
- `{{user_id}}` - User ID for task assignments (default: 1)

#### Sample Payloads

**Complex Case Creation (Workplace Harassment)**:
```json
{
  "title": "Workplace Harassment Investigation",
  "description": "Multiple allegations requiring immediate attention and legal review",
  "complainantName": "Sarah Johnson",
  "complainantEmail": "sarah.johnson@company.com",
  "priority": "HIGH",
  "departmentId": 1,
  "caseTypeId": 2,
  "allegations": [
    {
      "allegationType": "HARASSMENT",
      "severity": "HIGH",
      "description": "Verbal harassment including inappropriate comments",
      "involvedPersons": ["Mike Wilson (Supervisor)", "Jenny Adams (Witness)"],
      "incidentDate": "2025-06-10"
    },
    {
      "allegationType": "DISCRIMINATION", 
      "severity": "MEDIUM",
      "description": "Gender-based discrimination in promotions",
      "involvedPersons": ["Mike Wilson (Supervisor)", "Tom Smith (HR)"],
      "incidentDate": "2025-06-05"
    },
    {
      "allegationType": "RETALIATION",
      "severity": "HIGH", 
      "description": "Punitive schedule changes after HR complaint",
      "involvedPersons": ["Mike Wilson (Supervisor)"],
      "incidentDate": "2025-06-12"
    }
  ]
}
```

**Task Completion with Workflow Variables**:
```json
{
  "taskId": "{{task_id}}",
  "userId": 1,
  "decision": "APPROVE",
  "comments": "HR initial review completed. Case severity confirms need for legal department involvement.",
  "variables": {
    "hrReviewComments": "Case involves serious harassment allegations requiring legal review",
    "nextDepartment": "LEGAL",
    "urgencyLevel": "HIGH",
    "witnessStatementsCollected": true,
    "documentationComplete": true
  }
}
```

#### Automated Testing Features
- **JWT Token Management**: Auto-extracts and sets tokens from login response
- **Response Validation**: Automated tests verify response structure and data
- **Variable Chaining**: Case numbers and task IDs automatically flow between requests
- **Status Code Verification**: Ensures proper HTTP response codes
- **Data Validation**: Verifies allegation counts, case structure, and workflow progression

## Architecture Overview

### Core Technology Stack
- **Spring Boot 3.2.2** with Java 17
- **Flowable 7.0.0** - BPMN workflow engine with DMN decision support
- **PostgreSQL** - Database on Neon cloud (schema: `cms_flowable_workflow`)
- **JWT Authentication** - Stateless security with role-based access
- **Spring Security** - Authorization with method-level security

### Database Architecture
- **Business Tables**: `cases`, `allegations`, `work_items`, `users`, `roles`, `departments`, `case_types`, `case_transitions`
- **Flowable Tables**: Auto-managed ACT_* tables for workflow engine
- **Schema**: All tables use `cms_flowable_workflow` schema
- **Connection**: Single datasource with HikariCP connection pooling (max 20 connections)
- **Database URL**: PostgreSQL on Neon cloud with SSL required
- **Management**: Flowable tables auto-create/update, business tables managed manually

#### Key Table Relationships
```
cases (1) → (n) allegations
cases (1) → (n) work_items  
cases (n) → (1) users (created_by, assigned_to)
cases (n) → (1) departments
cases (n) → (1) case_types
users (n) → (n) roles (user_roles junction table)
case_transitions (audit trail for case status changes)
```

#### Critical Database Fields
- **Case Number Format**: `CMS-YYYY-XXX` (e.g., CMS-2025-001)
- **Work Item Number Format**: `{CaseNumber}-WI-{SequentialNumber}` (e.g., CMS-2025-001-WI-01)
- **UUID Handling**: Flowable process instance IDs are UUIDs, converted to hashCode() for Long fields
- **Audit Fields**: All entities have `created_at`, `updated_at`, `created_by`, `updated_by`

### Workflow Engine Setup
- **BPMN Process**: `Process_CMS_Workflow` - Main case management workflow
- **DMN Decision**: `allegation-classification` - Routes cases to departments
- **Process Variables**: Cases carry metadata through workflow lifecycle
- **User Tasks**: EO Intake → Classification → Department Processing → Investigation → Closure
- **Parallel Processing**: Multi-department cases use parallel gateways

### Key Business Flow
1. **Case Creation**: POST `/v1/cases` creates case + allegations as work_items
2. **Workflow Start**: Automatically starts BPMN process with case variables
3. **DMN Classification**: Evaluates allegation types and routes to departments
4. **Task Assignment**: Creates user tasks for appropriate roles (HR_SPECIALIST, LEGAL_COUNSEL, etc.)
5. **Work Item Tracking**: Allegations become trackable work_items in workflow
6. **Process Completion**: Tasks progress through investigation to case closure

### Security Model
- **Stateless JWT**: 24-hour tokens, 7-day refresh tokens
- **Role-based Access**: HR_SPECIALIST, LEGAL_COUNSEL, SECURITY_ANALYST, DIRECTOR
- **Protected Endpoints**: All `/v1/*` routes require authentication
- **Candidate Groups**: Flowable tasks assigned to role-based groups

## Key Service Classes

### CaseWorkflowService
Central orchestrator for case management and workflow integration. Key methods:
- `createCaseWithWorkflow()` - Creates case, work_items, starts BPMN process
- `completeTask()` - Advances workflow tasks with variables
- `getTasksForCase()` - Retrieves active tasks for case number

### FlowableDeploymentService  
Manages BPMN and DMN deployments:
- Auto-deploys from `/processes/` and `/dmn/` classpath
- Supports runtime deployment via REST API

### AuthServiceImpl
Handles JWT authentication:
- User login/logout with token generation
- Password validation with BCrypt
- Role-based authorization support

## Important Configuration

### Application Context
- **Context Path**: `/api` - All endpoints prefixed with `/api`
- **Controller Mappings**: Use `/v1/cases` (not `/api/v1/cases`) due to context path

### Database Connection
- **URL**: PostgreSQL on Neon cloud with connection pooling
- **Schema Management**: Flowable auto-creates workflow tables, business tables managed manually
- **Connection Pool**: HikariCP with 20 max connections

### Flowable Configuration
- **BPMN Engine**: Enabled - processes BPMN workflows
- **DMN Engine**: Enabled - evaluates decision tables  
- **CMMN/Form/Content**: Disabled for performance
- **History Level**: Full audit trail retention
- **Async Executor**: 8-16 thread pool for background processing

## Complete API Endpoint Reference

### Authentication Endpoints (`/auth`)
- `POST /auth/login` - User authentication
  - **Request**: `LoginRequest` (username, password)
  - **Response**: `LoginResponse` (token, refreshToken, userDetails)
  - **Public**: No authentication required
- `POST /auth/logout` - Invalidate user session
- `POST /auth/register` - Create new user (admin only)
- `GET /auth/validate` - Validate JWT token

### Case Management Endpoints (`/v1/cases`)
- `POST /v1/cases` - Create case with allegations
  - **Request**: `CreateCaseWithAllegationsRequest`
  - **Response**: `CaseWithAllegationsResponse` (201 Created)
  - **Process**: Creates case → work_items → starts BPMN workflow
- `GET /v1/cases/{caseNumber}` - Get case details
- `GET /v1/cases` - List all cases (paginated)
- `PUT /v1/cases/{caseNumber}` - Update case
- `DELETE /v1/cases/{caseNumber}` - Soft delete case

### Allegation Management Endpoints (`/api/v1/allegations`)
- `GET /api/v1/allegations/{allegationId}` - Get allegation by ID
- `GET /api/v1/allegations/case/{caseId}` - Get allegations by case
- `GET /api/v1/allegations/type/{allegationType}` - Filter by type
- `GET /api/v1/allegations/severity/{severity}` - Filter by severity
- `GET /api/v1/allegations/department/{department}` - Filter by department
- `PUT /api/v1/allegations/{allegationId}/classification` - Update classification

### Workflow Management Endpoints (`/workflow`)
- `POST /workflow/start/{processKey}` - Start workflow process
- `GET /workflow/tasks` - Get all active tasks
- `GET /workflow/tasks/user/{userId}` - Get user-specific tasks
- `GET /workflow/tasks/case/{caseNumber}` - Get case-specific tasks
- `POST /workflow/tasks/{taskId}/complete` - Complete workflow task
- `GET /workflow/process/{processInstanceId}` - Get process instance details
- `GET /workflow/processes` - List active processes

### Flowable Deployment Endpoints (`/v1/deploy`)
- `POST /v1/deploy/all` - Deploy all BPMN, DMN, CMMN definitions
  - **Response**: Deployment status and IDs
- `GET /v1/deploy/status` - Get deployment status
- `POST /v1/deploy/bpmn` - Deploy specific BPMN file
- `POST /v1/deploy/dmn` - Deploy specific DMN file

### System Monitoring Endpoints (`/actuator`)
- `GET /actuator/health` - Application health status
- `GET /actuator/info` - Application metadata
- `GET /actuator/metrics` - Performance metrics
- `GET /actuator/flowable` - Flowable engine metrics

## Data Models & DTOs

### Core Request DTOs
```java
CreateCaseWithAllegationsRequest {
  String title;
  String description;
  String complainantName;
  String complainantEmail;
  Priority priority;
  Long departmentId;
  Long caseTypeId;
  List<AllegationRequest> allegations;
}

AllegationRequest {
  String allegationType;
  Severity severity;
  String description;
  List<String> involvedPersons;
  LocalDate incidentDate;
}
```

### Core Response DTOs
```java
CaseWithAllegationsResponse {
  String caseId;
  String caseNumber;
  String title;
  String description;
  Priority priority;
  CaseStatus status;
  String complainantName;
  String complainantEmail;
  Long workflowInstanceKey;
  LocalDateTime createdAt;
  LocalDateTime updatedAt;
  String createdBy;
  String assignedTo;
  List<AllegationResponse> allegations;
}

WorkflowTaskResponse {
  String taskId;
  String taskName;
  String description;
  String processInstanceId;
  String processDefinitionId;
  String assignee;
  String candidateGroups;
  LocalDateTime createTime;
  LocalDateTime dueDate;
  Integer priority;
  String formKey;
  String category;
  Map<String, Object> variables;
  String caseId;
}
```

### Core Enums
```java
public enum Severity {
    LOW("Low"),
    MEDIUM("Medium"), 
    HIGH("High"),
    CRITICAL("Critical");
}

public enum Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}

public enum CaseStatus {
    OPEN, IN_PROGRESS, UNDER_REVIEW, 
    PENDING_CLOSURE, CLOSED, ARCHIVED
}
```

## Development Patterns

### Entity Relationships
- `Case` → Multiple `Allegation` entities (OneToMany)
- `WorkItemEntity` stores workflow-trackable allegations
- `User` with `Set<Role>` for security integration (ManyToMany)
- All entities use `@CreatedDate`/`@LastModifiedDate` for audit trails
- JPA entities use `@Table(schema = "cms_flowable_workflow")`

### Error Handling
- `GlobalExceptionHandler` provides consistent API responses
- Custom exceptions: `CaseNotFoundException`, `AllegationNotFoundException`, `WorkflowException`
- Validation errors return field-level details with `@Valid` and `BindingResult`
- Standard error response format with timestamp, status, error, message, path

### API Response Structure
- Standardized response DTOs separate from entities
- `CaseWithAllegationsResponse` includes nested allegation data
- `WorkflowTaskResponse` provides Flowable task details with case context
- Pagination support with `PageRequest` and `Page<T>` responses
- HTTP status codes: 200 (OK), 201 (Created), 400 (Bad Request), 401 (Unauthorized), 404 (Not Found)

### Workflow Variable Pattern
- Cases pass business data as process variables (`Map<String, Object>`)
- DMN decisions consume case variables for routing (`allegationType`, `severity`, `classification`)
- Task completion updates variables for audit trail and next step routing
- Common variables: `caseId`, `caseTitle`, `priority`, `complainantName`, `allegations[]`

### Security Patterns
- Method-level security with `@PreAuthorize("hasRole('ROLE_NAME')")`
- JWT token extraction in `JwtAuthenticationFilter`
- Role-based access: `HR_SPECIALIST`, `LEGAL_COUNSEL`, `SECURITY_ANALYST`, `DIRECTOR`
- Candidate group assignment in Flowable tasks matches Spring Security roles

## Testing Approach

### Manual Testing with Postman
- **Primary Collection**: `CMS_Flowable_API_Tests.postman_collection.json`
- **Environment**: `CMS_Flowable_Environment.postman_environment.json`
- **Test Scenarios**: Full workflow lifecycle with automated validations
- **Setup**: Import both files into Postman, run "Login User" first to set JWT token

### Comprehensive Test Workflow
1. **Authentication**: Login as admin/jow to get JWT token
2. **Case Creation**: Create complex case with 3 allegations (harassment, discrimination, retaliation)
3. **Workflow Verification**: Check that BPMN process started and tasks created
4. **Task Progression**: Complete HR → Legal → CSIS department tasks
5. **System Health**: Verify application and Flowable engine health

### Key Test Cases
- **Multi-allegation Cases**: Test classification of different allegation types
- **DMN Decision Routing**: Verify allegations route to correct departments (HR/Legal/CSIS)
- **Parallel Processing**: Test multi-department cases with parallel gateways
- **Task Variables**: Ensure workflow variables pass correctly between tasks
- **Authentication Flow**: Test JWT token generation, validation, and expiration
- **Error Handling**: Test invalid requests, missing fields, unauthorized access

### Unit Testing Framework
```bash
mvn test                              # Run all tests
mvn test -Dtest=CaseWorkflowServiceTest   # Run specific test class
mvn test -Dtest=*Service*             # Run all service tests
```

### Integration Testing
- Test database transactions with `@Transactional`
- Test Flowable engine integration with `@SpringBootTest`
- Test security with `@WithMockUser` and `@WithMockJwt`

## Performance & Monitoring

### Application Metrics
- **Health Check**: `GET /actuator/health` - Database, Flowable engine status
- **Metrics**: `GET /actuator/metrics` - JVM, HTTP, database pool metrics
- **Flowable Metrics**: `GET /actuator/flowable` - Process instances, tasks, deployments

### Performance Expectations
- **Case Creation**: < 2 seconds (includes DB save + workflow start)
- **Task Completion**: < 1 second
- **Case Retrieval**: < 500ms
- **DMN Evaluation**: < 100ms
- **JWT Token Validation**: < 50ms

### Database Connection Monitoring
- **HikariCP Pool**: Max 20 connections, min 5 idle
- **Connection Timeout**: 30 seconds
- **Query Logging**: Set `spring.jpa.show-sql: true` for debugging

## Troubleshooting Guide

### Common Issues

#### 1. Path Mapping Conflicts
**Problem**: 404 Not Found for API endpoints
**Cause**: Context path `/api` conflicts with controller mappings
**Solution**: 
- Controllers use `/v1/cases` (not `/api/v1/cases`) due to context path
- Security config paths must match: `"/v1/cases/**"` not `"/api/v1/cases/**"`
- Full URL: `http://localhost:8080/api/v1/cases`

#### 2. UUID Conversion Errors
**Problem**: Cannot convert UUID to Long
**Cause**: Flowable process instance IDs are UUIDs, database fields are Long
**Solution**:
```java
// Convert UUID to Long for database storage
Long workflowInstanceKey = (long) processInstance.getId().hashCode();
// Store full UUID in varchar field for reference
String flowableProcessInstanceId = processInstance.getId();
```

#### 3. Enum Handling Issues
**Problem**: Enum conversion errors (e.g., "Critical" vs "CRITICAL")
**Cause**: Severity enum toString() returns display name, not constant name
**Solution**: Use custom conversion methods
```java
private Severity convertStringToSeverity(String severityString) {
    switch (severityString.toUpperCase()) {
        case "LOW": case "Low": return Severity.LOW;
        case "MEDIUM": case "Medium": return Severity.MEDIUM;
        case "HIGH": case "High": return Severity.HIGH;
        case "CRITICAL": case "Critical": return Severity.CRITICAL;
        default: return Severity.MEDIUM;
    }
}
```

#### 4. JWT Authentication Failures
**Problem**: 401 Unauthorized responses
**Causes & Solutions**:
- **Missing Token**: Include `Authorization: Bearer <token>` header
- **Expired Token**: Login again to get new token (24-hour expiry)
- **Invalid Format**: Ensure "Bearer " prefix (with space)
- **Wrong Endpoint**: Check if endpoint requires authentication

#### 5. Flowable Process Start Failures
**Problem**: Process definition not found
**Causes & Solutions**:
- **Missing BPMN**: Ensure `Process_CMS_Workflow.bpmn20.xml` in `/resources/processes/`
- **Deployment Issue**: Check deployment logs, use `/v1/deploy/all` endpoint
- **Process Key**: Verify process key matches `Process_CMS_Workflow`

#### 6. Database Connection Issues
**Problem**: Connection timeout or pool exhaustion
**Causes & Solutions**:
- **Neon Cloud**: Check SSL connection and credentials
- **Pool Settings**: Increase `hikari.maximum-pool-size` if needed
- **Long Transactions**: Use `@Transactional` properly, avoid long-running transactions

#### 7. DMN Decision Evaluation Errors
**Problem**: DMN decision table not evaluating correctly
**Causes & Solutions**:
- **Missing DMN**: Ensure `allegation-classification.dmn` in `/resources/dmn/`
- **Variable Names**: Check DMN input variable names match process variables
- **Data Types**: Ensure process variables match DMN expected types (String, Integer, Boolean)

### Debug Mode
Enable comprehensive logging for troubleshooting:
```yaml
logging:
  level:
    org.flowable: DEBUG
    com.workflow.cmsflowable: DEBUG
    org.springframework.security: DEBUG
```

### Database Debugging
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

### Common Error Codes
- **400 Bad Request**: Invalid JSON, missing required fields, validation errors
- **401 Unauthorized**: Missing/invalid JWT token
- **403 Forbidden**: Valid token but insufficient permissions
- **404 Not Found**: Invalid endpoint, missing resource
- **500 Internal Server Error**: Database issues, Flowable engine errors

### Useful Development Commands
```bash
# Check application logs
tail -f logs/cms-flowable.log

# Database connection test
mvn spring-boot:run -Dspring.jpa.show-sql=true

# Skip tests during build
mvn clean package -DskipTests

# Run with specific profile
mvn spring-boot:run -Dspring.profiles.active=dev

# Generate dependency tree
mvn dependency:tree
```
