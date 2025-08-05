# CMS Flowable Comprehensive Documentation

## Overview

The CMS (Case Management System) Flowable application is a comprehensive workflow-based case management system for handling employee complaints, misconduct investigations, and disciplinary actions. It integrates **Flowable BPMN** for workflow orchestration, **DMN decision tables** for dynamic routing, and **Cerbos authorization** for fine-grained access control.

## Table of Contents

1. [Architecture & Technology Stack](#architecture--technology-stack)
2. [API Endpoints](#api-endpoints)
3. [Case Management Workflow](#case-management-workflow)
4. [Data Models & Entities](#data-models--entities)
5. [Authorization & Security](#authorization--security)
6. [Database Schema](#database-schema)
7. [Testing & Development](#testing--development)
8. [Code Cleanup Recommendations](#code-cleanup-recommendations)
9. [Postman Collection](#postman-collection)
10. [OpenAPI Specification](#openapi-specification)

---

## Architecture & Technology Stack

### Core Technologies
- **Spring Boot 3.2.2** with Java 17
- **Flowable 7.0.0** - BPMN workflow engine with DMN decision support
- **PostgreSQL** - Database with schema `cms_flowable_workflow`
- **JWT Authentication** - Stateless security with role-based access
- **Cerbos** - External authorization service (ABAC)
- **Spring Security** - Method-level security integration

### Key Components
- **BPMN Process**: `Process_CMS_Workflow_Updated` - Main case management workflow
- **DMN Decision**: `allegation-classification` - Routes cases to departments
- **Authorization**: Cerbos policies for fine-grained access control
- **Database**: Single datasource for both business and Flowable tables

### Application URLs
- **Application**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/api/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/api/v3/api-docs
- **Actuator Health**: http://localhost:8080/api/actuator/health
- **Flowable Metrics**: http://localhost:8080/api/actuator/flowable

---

## API Endpoints

### 🔐 Authentication Endpoints (`/auth`)

#### POST `/auth/login`
**Purpose**: User authentication with JWT token generation  
**Public**: No authentication required  
**Request Body**:
```json
{
  "username": "hr.specialist",
  "password": "demo123"
}
```
**Response**:
```json
{
  "success": true,
  "message": "Authentication successful",
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "user": {
    "userId": 40,
    "username": "hr.specialist",
    "email": "hr@cms-flowable.com",
    "roles": ["HR_SPECIALIST"]
  }
}
```

**Test Users**:
- `admin` / `demo123` - System Administrator
- `intake.analyst` / `demo123` - Intake Analyst
- `hr.specialist` / `demo123` - HR Specialist
- `legal.counsel` / `demo123` - Legal Counsel
- `security.analyst` / `demo123` - Security Analyst

### 📁 Case Management Endpoints (`/v1/cases`)

#### POST `/v1/cases`
**Purpose**: Create case with allegations and start BPMN workflow  
**Authorization**: `@PreAuthorize("hasPermission(#request, 'case', 'create')")`  
**Request Body**:
```json
{
  "title": "Workplace Harassment Investigation",
  "description": "Multiple allegations requiring immediate attention",
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
    }
  ]
}
```

**Response**:
```json
{
  "caseId": "CMS-2025-001",
  "caseNumber": "CMS-2025-001",
  "title": "Workplace Harassment Investigation",
  "status": "OPEN",
  "workflowInstanceKey": 123456789,
  "createdAt": "2025-07-22T14:39:36.138Z",
  "allegations": [...]
}
```

#### GET `/v1/cases/{caseNumber}`
**Purpose**: Get case details with allegations  
**Authorization**: `@PreAuthorize("hasPermission(#caseNumber, 'case', 'view')")`  
**Response**: Full case details with nested allegations

#### GET `/v1/cases`
**Purpose**: List all cases (paginated)  
**Authorization**: `@PreAuthorize("hasAnyRole('DIRECTOR_GROUP', 'MANAGER_GROUP', 'ANALYST_GROUP')")`  
**Query Parameters**: `page`, `size`, `status`, `priority`

### 🔄 Workflow Management Endpoints (`/workflow`)

#### GET `/workflow/tasks`
**Purpose**: Get all active workflow tasks  
**Authorization**: JWT required  
**Response**: Array of WorkflowTaskResponse objects

#### GET `/workflow/tasks/user/{userId}`
**Purpose**: Get user-specific tasks  
**Authorization**: JWT required  
**Response**: Tasks assigned to the specific user

#### POST `/workflow/tasks/{taskId}/complete`
**Purpose**: Complete workflow task with variables  
**Authorization**: `@PreAuthorize("hasPermission(#request.taskId, 'task', 'complete')")`  
**Request Body**:
```json
{
  "hrReviewCompleted": true,
  "hrFindings": "Confirmed harassment patterns",
  "hrRecommendation": "PROCEED_TO_FORMAL_INVESTIGATION",
  "riskLevel": "HIGH",
  "reviewDate": "2025-07-03"
}
```

### 🚀 Deployment Endpoints (`/v1/deploy`)

#### POST `/v1/deploy/all`
**Purpose**: Deploy all BPMN and DMN definitions  
**Authorization**: Admin required  
**Response**: Deployment status and IDs

#### GET `/v1/deploy/status`
**Purpose**: Get deployment status  
**Authorization**: Admin required

### 📊 System Monitoring (`/actuator`)

#### GET `/actuator/health`
**Purpose**: Application health status  
**Public**: No authentication required

#### GET `/actuator/flowable`
**Purpose**: Flowable engine metrics  
**Authorization**: Admin required

---

## Case Management Workflow

### Process: `Process_CMS_Workflow_Updated`

The workflow consists of 11 distinct user tasks across multiple departments with dynamic routing based on allegation types.

### Workflow Stages

#### 1. **Complaint Intake** (`Task_EO_Intake`)
- **Assigned To**: `INTAKE_ANALYST_GROUP`
- **Purpose**: Initial complaint reception and documentation
- **Activities**: Record details, collect evidence, assign case number
- **Variables Set**: `status: "COMPLAINT_RECEIVED"`, `caseNumber`, `priority`

#### 2. **Dynamic Department Routing** (DMN Decision)
- **Service**: `DetermineDepartmentRoutingService`
- **DMN Decision**: `allegation-classification`
- **Input Variables**: `allegationType`, `severity`
- **Output Variables**: `classification`, `assignedGroup`, `priority`

**DMN Routing Examples**:
- **HARASSMENT + HIGH** → HR Department, HR_SPECIALIST, CRITICAL Priority
- **FRAUD + HIGH** → LEGAL Department, LEGAL_COUNSEL, HIGH Priority
- **SECURITY_BREACH + CRITICAL** → CSIS Department, CSIS_ANALYST, CRITICAL Priority

#### 3. **Multi-Department Assignment**
Based on DMN decisions, cases route to:
- **HR Department** (`HR_GROUP`) - Harassment, discrimination cases
- **Legal Department** (`LEGAL_GROUP`) - Fraud, compliance violations
- **CSIS Department** (`CSIS_GROUP`) - Security breaches, data incidents

#### 4. **Investigation Process**
- **Draft Investigation Plan** (`INVESTIGATOR_GROUP`)
- **Investigation Plan Approval** (Gateway decision)
- **Conduct Investigation** (`INVESTIGATOR_GROUP`)
- **Draft Report of Investigation** (`INVESTIGATOR_GROUP`)
- **ROI Approval** (Gateway decision)

#### 5. **Findings Assessment & Adjudication**
- **Assess Findings** (`INVESTIGATION_MANAGER_GROUP`)
- **Substantiation Gateway** (Decision on allegations)
- **HR/Legal Adjudication** (`HR_LEGAL_ADJUDICATION_GROUP`)

#### 6. **AROG Review Process** (Conditional)
- **AROG Criteria Gateway** (Automatic evaluation)
- **AROG Review Meeting** (`AROG_GROUP`) - For high-impact cases

#### 7. **Case Closure**
- **EO Final Review and Closure** (`DIRECTOR_GROUP`)
- **Case Closure Gateway** (Final approval)
- **Case Closed** (End Event)

### Process Variables

**Core Case Variables**:
```yaml
caseNumber: "CMS-2025-XXX"
title: "Case title"
priority: "LOW|MEDIUM|HIGH|CRITICAL"
status: "COMPLAINT_RECEIVED|IN_PROGRESS|CLOSED"
relevantDepartments: ["HR", "LEGAL", "CSIS"]
hrNeeded: boolean
legalNeeded: boolean
csisNeeded: boolean
ipApproved: boolean
roiApproved: boolean
allegationsSubstantiated: boolean
caseApprovedForClosure: boolean
```

---

## Data Models & Entities

### Core Entities

#### Case Entity
```java
@Entity
@Table(schema = "cms_flowable_workflow")
public class Case {
    @Id
    private String caseId;  // Primary key (CMS-YYYY-XXX)
    private String caseNumber;
    private String title;
    private String description;
    private Priority priority;
    private CaseStatus status;
    private Long workflowInstanceKey;  // Links to Flowable process
    
    @OneToMany(mappedBy = "caseId", cascade = CascadeType.ALL)
    private List<Allegation> allegations;
    
    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL)
    private List<WorkItem> workItems;
}
```

#### Allegation Entity
```java
@Entity
@Table(schema = "cms_flowable_workflow")
public class Allegation {
    @Id
    private String allegationId;
    private String caseId;  // Foreign key to Case
    private String allegationType;
    private Severity severity;
    private String description;
    private String classification;  // From DMN
    private String assignedGroup;   // From DMN
    private String flowablePlanItemId;  // Links to Flowable
}
```

#### WorkItemEntity
```java
@Entity
@Table(name = "work_items", schema = "cms_flowable_workflow")
public class WorkItemEntity {
    @Id
    private String workItemId;
    private String caseId;  // Foreign key to Case
    private String workItemNumber;  // CMS-YYYY-XXX-WI-XX
    private String allegationType;
    private WorkItemStatus status;
    private String flowableProcessInstanceId;
}
```

### Core Enums

```java
public enum Severity {
    LOW("Low"), MEDIUM("Medium"), HIGH("High"), CRITICAL("Critical");
}

public enum Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}

public enum CaseStatus {
    OPEN, IN_PROGRESS, UNDER_REVIEW, 
    PENDING_CLOSURE, CLOSED, ARCHIVED
}
```

### Request/Response DTOs

#### CreateCaseWithAllegationsRequest
```java
public class CreateCaseWithAllegationsRequest {
    private String title;
    private String description;
    private String complainantName;
    private String complainantEmail;
    private Priority priority;
    private Long departmentId;
    private Long caseTypeId;
    private List<AllegationRequest> allegations;
}
```

#### WorkflowTaskResponse
```java
public class WorkflowTaskResponse {
    private String taskId;
    private String taskName;
    private String description;
    private String processInstanceId;
    private String assignee;
    private String candidateGroups;
    private LocalDateTime createTime;
    private Map<String, Object> variables;
    private String caseId;
}
```

---

## Authorization & Security

### Authentication Model
- **JWT Tokens**: 24-hour expiry, 7-day refresh tokens
- **Stateless**: No server-side session storage
- **Header Format**: `Authorization: Bearer <jwt_token>`

### Authorization Model (Cerbos Integration)

#### Roles & Groups
```yaml
DIRECTOR_GROUP: Full access, case closure authority
AROG_GROUP: Independent review for high-impact cases
IU_MANAGER_GROUP: Investigation oversight and approvals
INVESTIGATOR_GROUP: Investigation planning and execution
HR_GROUP: HR-related allegations and policy violations
LEGAL_GROUP: Legal, compliance, and fraud cases
CSIS_GROUP: Security incidents and data breaches
INTAKE_ANALYST_GROUP: Initial case processing and triage
```

#### Permission Model
- **Universal View**: Directors and AROG can view all cases
- **Conditional View**: Users can view cases if:
  - They are assigned to the case, OR
  - Their role matches current task group, OR
  - Their department is relevant to the case

#### Workflow Actions
Each workflow step has specific role and state requirements:
- **Intake**: Only `INTAKE_ANALYST_GROUP` when status is `COMPLAINT_RECEIVED`
- **Investigation**: Only `INVESTIGATOR_GROUP` when IP is approved
- **AROG Review**: Only `AROG_GROUP` for flagged cases
- **Final Review**: Only `DIRECTOR_GROUP` for final approval

### Cerbos Policy Structure

#### Principal Schema
```yaml
principal:
  attributes:
    userId: string
    username: string
    roles: array<string>
    is_manager: boolean
    departments: array<string>
```

#### Resource Schema (Case)
```yaml
resource:
  kind: case
  attributes:
    status: string
    currentTaskGroup: string
    assignedUsers: array<string>
    relevantDepartments: array<string>
    ipApproved: boolean
    roiApproved: boolean
    allegationsSubstantiated: boolean
    isArogCase: boolean
```

---

## Database Schema

### Business Tables (cms_flowable_workflow schema)
- **cases** - Core case information
- **allegations** - Individual allegations with DMN results
- **work_items** - Workflow-trackable allegations  
- **users** - User accounts and authentication
- **roles** - Role definitions
- **user_roles** - User-role assignments (Many-to-Many)
- **departments** - Department information
- **case_types** - Case type definitions
- **case_transitions** - Audit trail of case changes

### Flowable Tables (ACT_ prefix)
- **ACT_RU_TASK** - Active workflow tasks
- **ACT_HI_TASKINST** - Task history
- **ACT_RU_EXECUTION** - Process instances
- **ACT_HI_PROCINST** - Process history
- **ACT_RU_VARIABLE** - Process variables

### Key Relationships
```
cases (1) → (n) allegations
cases (1) → (n) work_items
cases (n) → (1) users (created_by, assigned_to)
cases (n) → (1) departments
cases (n) → (1) case_types
users (n) → (n) roles (user_roles junction)
```

### Database Connection
- **URL**: PostgreSQL with SSL required
- **Schema**: `cms_flowable_workflow`
- **Connection Pool**: HikariCP with 20 max connections
- **Management**: Liquibase for schema migrations

---

## Testing & Development

### Development Commands
```bash
# Start application
mvn spring-boot:run

# Build project
mvn clean compile

# Package application
mvn clean package

# Run tests
mvn test

# Run specific test
mvn test -Dtest=ClassNameTest
```

### Test Users (All passwords: `demo123`)
- **admin** - System Administrator (IU_MANAGER role)
- **intake.analyst** - Intake Analyst (INTAKE_ANALYST role)
- **hr.specialist** - HR Specialist (HR_SPECIALIST role)
- **legal.counsel** - Legal Counsel (LEGAL_COUNSEL role)
- **security.analyst** - Security Analyst (SECURITY_ANALYST role)

### End-to-End Testing Flow

#### 1. Authentication
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
-H "Content-Type: application/json" \
-d '{"username": "hr.specialist", "password": "demo123"}'
```

#### 2. Case Creation
```bash
curl -X POST "http://localhost:8080/api/v1/cases" \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{
  "title": "Workplace Harassment Investigation",
  "description": "Multiple allegations requiring attention",
  "complainantName": "Sarah Johnson",
  "complainantEmail": "sarah.johnson@company.com",
  "priority": "HIGH",
  "allegations": [
    {
      "allegationType": "HARASSMENT",
      "severity": "HIGH",
      "description": "Verbal harassment"
    }
  ]
}'
```

#### 3. Task Identification
```sql
-- Get active tasks for the process
SELECT id_, name_, task_def_key_, assignee_, create_time_
FROM act_ru_task 
WHERE proc_inst_id_ = '{PROCESS_INSTANCE_ID}'
ORDER BY create_time_;
```

#### 4. Task Completion
```bash
curl -X POST "http://localhost:8080/api/workflow/tasks/{TASK_ID}/complete" \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{
  "hrReviewCompleted": true,
  "hrFindings": "Harassment confirmed",
  "hrRecommendation": "PROCEED_TO_INVESTIGATION"
}'
```

### Debugging & Monitoring

#### Database Queries
```sql
-- Monitor case creation
SELECT case_id, case_number, status, created_at 
FROM cms_flowable_workflow.cases 
ORDER BY created_at DESC LIMIT 5;

-- Monitor active tasks
SELECT id_, name_, task_def_key_, create_time_
FROM act_ru_task 
WHERE create_time_ > NOW() - INTERVAL '1 hour';

-- Track process variables
SELECT name_, text_, long_
FROM act_ru_variable
WHERE proc_inst_id_ = '{PROCESS_INSTANCE_ID}';
```

#### Logging Configuration
```yaml
logging:
  level:
    org.flowable: DEBUG
    com.workflow.cmsflowable: DEBUG
    org.springframework.security: DEBUG
```

---

## Code Cleanup Recommendations

Based on comprehensive code analysis, the following cleanup is recommended:

### 🔴 HIGH PRIORITY - Remove Immediately

#### 1. Delete Development/Test Files with Security Risks
- **`/util/PasswordHashTest.java`** - Contains hardcoded test passwords
- **`/util/PasswordHashGenerator.java`** - Password hash generation utility
- **`/util/PasswordVerificationTest.java`** - Password verification testing
- **`/util/JwtUtil.java`** - Duplicate JWT implementation (unused)

#### 2. Remove Debug Code
- **50+ `System.out.println()` statements** across multiple files:
  - `CaseWorkflowService.java` (27 instances)
  - `CaseManagementController.java` (8 instances)
  - `WorkflowController.java` (6 instances)
  - `DetermineDepartmentRoutingService.java` (8 instances)

### 🟡 MEDIUM PRIORITY - Consolidate/Refactor

#### 1. Security Annotations
- **Commented `@PreAuthorize` annotations** in `CaseWorkflowService.java`
- Either re-enable security or remove commented code

#### 2. Unused/Redundant Code
- **`AllegationController.java`** - Appears unused (no references found)
- **`MyCustomInterceptor.java`** - Empty interceptor with no functionality
- **`WebConfig.java`** - Registers empty interceptor

#### 3. Duplicate Classification Logic
- Consolidate allegation classification methods across multiple services
- Use DMN service as single source of truth

### 🟢 LOW PRIORITY - Code Quality

#### 1. Import Cleanup
- Remove commented import statements
- Remove unused imports

#### 2. Method Cleanup
- **`WorkItemService.java`** - Contains unused methods
- Clean up excessive debug output

### Estimated Impact
- **Files to Delete**: 6 files
- **Lines of Code Reduction**: 400+ lines
- **Debug Code Cleanup**: 50+ System.out.println statements
- **Security Improvements**: Resolve commented security annotations

---

## Postman Collection

### Collection Overview
**Name**: CMS Flowable API - Updated Collection  
**Description**: Complete API testing suite with automated JWT token management  
**Version**: 2.1.0

### Environment Variables
```json
{
  "baseUrl": "http://localhost:8080/api",
  "jwt_token": "{{auto-extracted}}",
  "case_number": "{{auto-extracted}}",
  "task_id": "{{auto-extracted}}",
  "user_id": "1"
}
```

### Collection Structure

#### 1. Health Check
- **Test Endpoint** - `GET /v1/cases/test`
- Basic API connectivity verification

#### 2. Deployment Management
- **Deploy All (BPMN + DMN)** - `POST /v1/deploy/all`
- **Check Deployment Status** - `GET /v1/deploy/status`

#### 3. Authentication
- **Login User** - `POST /auth/login`
- Auto-extracts JWT token for subsequent requests

#### 4. Case Management
- **Create Case with Multiple Allegations** - `POST /v1/cases`
- **Get Case Details** - `GET /v1/cases/{case_number}`
- **List All Cases** - `GET /v1/cases`

#### 5. Workflow Operations
- **Get All Active Tasks** - `GET /workflow/tasks`
- **Get User Tasks** - `GET /workflow/tasks/user/{userId}`
- **Complete Task** - `POST /workflow/tasks/{taskId}/complete`

#### 6. System Health
- **Health Check** - `GET /actuator/health`
- **Application Info** - `GET /actuator/info`
- **Flowable Metrics** - `GET /actuator/flowable`

### Sample Test Case Payloads

#### Complex Case Creation
```json
{
  "title": "Senior Manager Ethics Investigation",
  "description": "Multiple violations requiring multi-department review",
  "priority": "HIGH",
  "complainantName": "Ethics Hotline",
  "complainantEmail": "ethics@company.com",
  "allegations": [
    {
      "allegationType": "HARASSMENT",
      "severity": "HIGH",
      "description": "Workplace harassment creating hostile environment"
    },
    {
      "allegationType": "DISCRIMINATION",
      "severity": "MEDIUM", 
      "description": "Gender-based discrimination in promotions"
    },
    {
      "allegationType": "FINANCIAL_MISCONDUCT",
      "severity": "HIGH",
      "description": "Unauthorized expense claims and fund misuse"
    }
  ]
}
```

#### Task Completion with Variables
```json
{
  "hrReviewCompleted": true,
  "hrFindings": "Systematic violations confirmed across multiple areas",
  "hrRiskLevel": "HIGH",
  "hrRecommendation": "IMMEDIATE_SUSPENSION_PENDING_INVESTIGATION",
  "reviewDate": "2025-07-03",
  "witnessesInterviewed": ["Employee A", "Employee B"],
  "documentationComplete": true
}
```

---

## OpenAPI Specification

### API Information
- **Title**: CMS Flowable API - Updated
- **Version**: 2.0.0
- **Description**: Comprehensive Case Management System with BPMN workflow integration
- **Contact**: cms-dev@company.com
- **License**: MIT

### Servers
- **Development**: http://localhost:8080/api
- **Staging**: https://cms-staging.company.com/api
- **Production**: https://cms.company.com/api

### Security Schemes
```yaml
security:
  - BearerAuth: []  # JWT tokens
  - ApiKeyAuth: []  # API key authentication
```

### Key API Paths

#### Health Check
```yaml
/v1/health:
  get:
    summary: API Health Check
    responses:
      '200':
        description: API is operational
        content:
          text/plain:
            schema:
              type: string
              example: "Case management controller is working!"
```

#### Deployment
```yaml
/v1/deploy/all:
  post:
    summary: Deploy BPMN and DMN Files
    description: Deploy Process_CMS_Workflow_Updated.bpmn20.xml and allegation-classification.dmn
    responses:
      '200':
        description: Deployment successful
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DeploymentResponse'
```

#### Case Management
```yaml
/v1/cases:
  post:
    summary: Create Case with Workflow
    description: Creates case, allegations, work items, and starts BPMN process
    security:
      - BearerAuth: []
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/CreateCaseWithAllegationsRequest'
    responses:
      '201':
        description: Case created successfully
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CaseWithAllegationsResponse'
```

### Component Schemas

#### DeploymentResponse
```yaml
DeploymentResponse:
  type: object
  properties:
    success:
      type: boolean
    deploymentId:
      type: string
    deploymentName:
      type: string
    message:
      type: string
```

#### CreateCaseWithAllegationsRequest
```yaml
CreateCaseWithAllegationsRequest:
  type: object
  required:
    - title
    - complainantName
    - complainantEmail
    - allegations
  properties:
    title:
      type: string
      example: "Workplace Harassment Investigation"
    description:
      type: string
    complainantName:
      type: string
      example: "Sarah Johnson"
    complainantEmail:
      type: string
      format: email
    priority:
      $ref: '#/components/schemas/Priority'
    allegations:
      type: array
      items:
        $ref: '#/components/schemas/AllegationRequest'
```

#### WorkflowTaskResponse
```yaml
WorkflowTaskResponse:
  type: object
  properties:
    taskId:
      type: string
      example: "6faa5865-5809-11f0-90f8-d25cf3d0bf28"
    taskName:
      type: string
      example: "HR Initial Review"
    description:
      type: string
    processInstanceId:
      type: string
    assignee:
      type: string
      nullable: true
    candidateGroups:
      type: string
      example: "HR_SPECIALIST"
    createTime:
      type: string
      format: date-time
    variables:
      type: object
      additionalProperties: true
    caseId:
      type: string
      example: "CMS-2025-001"
```

---

## Environment Configuration

### Required Environment Variables
```bash
# Database Configuration
DB_URL=postgresql://hostname:5432/database
DB_USERNAME=username
DB_PASSWORD=password
DB_SCHEMA=cms_flowable_workflow

# JWT Configuration
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=86400000  # 24 hours

# Cerbos Configuration
CERBOS_HOST=localhost
CERBOS_PORT=3593
CERBOS_FALLBACK_ENABLED=true

# Application Configuration
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8080
```

### Application Properties
```yaml
server:
  servlet:
    context-path: /api
  port: ${SERVER_PORT:8080}

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20

flowable:
  database-schema-update: true
  async-executor-activate: true
  
security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: ${JWT_EXPIRATION:86400000}

cerbos:
  host: ${CERBOS_HOST:localhost}
  port: ${CERBOS_PORT:3593}
  fallback:
    enabled: ${CERBOS_FALLBACK_ENABLED:true}
```

---

## Troubleshooting

### Common Issues

#### 1. Authentication Failures
- **Issue**: 401 Unauthorized responses
- **Solutions**:
  - Verify JWT token in Authorization header: `Bearer <token>`
  - Check token expiration (24-hour expiry)
  - Ensure correct user credentials

#### 2. Task Identification
- **Issue**: Cannot find active tasks
- **Solutions**:
  - Query `act_ru_task` table for active tasks
  - Filter by `create_time` for recent tasks
  - Check `candidate_group` assignments

#### 3. Workflow Process Issues
- **Problem**: Process instance not starting
- **Solutions**:
  - Verify BPMN deployment with `/v1/deploy/all`
  - Check process key: `Process_CMS_Workflow_Updated`
  - Review Flowable deployment logs

#### 4. Database Connection Issues
- **Problem**: Connection timeout or pool exhaustion
- **Solutions**:
  - Verify PostgreSQL connection string
  - Check SSL requirements for cloud databases
  - Monitor connection pool usage

### Debug Queries

#### Monitor Case Creation
```sql
SELECT case_id, case_number, title, status, created_at, workflow_instance_key 
FROM cms_flowable_workflow.cases 
ORDER BY created_at DESC LIMIT 5;
```

#### Track Active Tasks
```sql
SELECT id_, name_, task_def_key_, assignee_, create_time_
FROM act_ru_task 
WHERE create_time_ > NOW() - INTERVAL '1 hour'
ORDER BY create_time_ DESC;
```

#### Process Instance Status
```sql
SELECT id_, proc_def_id_, business_key_, start_time_, end_time_
FROM act_hi_procinst 
WHERE business_key_ LIKE 'CMS-2025-%'
ORDER BY start_time_ DESC;
```

---

## Performance & Monitoring

### Key Metrics
- **Case Creation**: < 2 seconds (includes workflow start)
- **Task Completion**: < 1 second  
- **Case Retrieval**: < 500ms
- **DMN Evaluation**: < 100ms
- **JWT Validation**: < 50ms

### Monitoring Endpoints
- **Health**: `/actuator/health` - Application and database status
- **Metrics**: `/actuator/metrics` - JVM and HTTP metrics
- **Flowable**: `/actuator/flowable` - Workflow engine metrics

### Performance Optimization
- **Connection Pooling**: HikariCP with 20 max connections
- **Async Processing**: Flowable async executor (8-16 threads)
- **Caching**: Process definitions and DMN decisions cached
- **Database Indexing**: Optimized queries for case retrieval

---

*This comprehensive documentation consolidates information from 8 separate .md files and provides a complete reference for the CMS Flowable application. Last updated: 2025-08-01*