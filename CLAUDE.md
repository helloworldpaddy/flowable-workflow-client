# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

- **Start application**: `mvn spring-boot:run`
- **Build project**: `mvn clean compile`
- **Package application**: `mvn clean package`
- **Run tests**: `mvn test`
- **Run specific test**: `mvn test -Dtest=ClassNameTest`
- **Clean build artifacts**: `mvn clean`

## Database Migration Strategy

**IMPORTANT:** The Liquibase migrations are designed to preserve existing transaction data.

- **Transaction Tables**: `cases`, `work_items`, `allegations` - Data is NEVER deleted during migrations
- **Reference Tables**: `roles`, `users`, `user_roles`, `departments`, `case_types` - Uses `ON CONFLICT DO NOTHING` to avoid duplicates
- **Migration Order**: Core tables → Indexes/Constraints → Seed data insertion
- **Data Safety**: All seed data insertions check for existing records before inserting

## Development Server URLs

- **Application URL**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/api/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/api/v3/api-docs
- **Actuator Health**: http://localhost:8080/api/actuator/health
- **Flowable Metrics**: http://localhost:8080/api/actuator/flowable

## Architecture Overview

This is a **Spring Boot 3.3.4** application with **Java 21** that integrates **Flowable 7.1.0** workflow engine for case management with **advanced queue-based task distribution**. The application uses **PostgreSQL** as the database and **JWT authentication** with role-based security.

### Core Technology Stack
- **Spring Boot 3.3.4** with Spring Security
- **Flowable 7.1.0** - BPMN workflow engine with DMN decision support
- **PostgreSQL** - Database with schema `cms_flowable_workflow`
- **JWT Authentication** - Stateless security with role-based access
- **Cerbos** - External authorization service (optional)
- **Queue Management System** - Advanced task distribution with priority-based routing
- **Hypersistence Utils** - JSONB support for PostgreSQL

### Key Service Architecture

**CaseWorkflowService** - Central orchestrator for case management and workflow integration
- `createCaseWithWorkflow()` - Creates case + work_items + starts BPMN process
- `completeTask()` - Advances workflow tasks with variables
- `getTasksForCase()` - Retrieves active tasks for case number

**FlowableDeploymentService** - Manages BPMN and DMN deployments
- Auto-deploys from `/processes/` and `/dmn/` classpath
- Supports runtime deployment via REST API

**AuthServiceImpl** - Handles JWT authentication with BCrypt password validation

**QueueTaskService** - Advanced task distribution and queue management
- `populateQueueTasksForProcessInstance()` - Automatically creates queue tasks when workflows start
- `claimTask()` - Allows users to claim tasks from queues
- `getTasksByQueue()` - Retrieves tasks from specific queues with priority ordering
- `getNextTaskFromQueue()` - Gets highest priority, oldest task from queue

**WorkflowMetadataService** - Dynamic workflow registration and deployment
- `registerWorkflowMetadata()` - Register workflow with candidate group to queue mappings
- `deployWorkflow()` - Deploy BPMN processes and build task-to-queue mappings
- `buildTaskQueueMappings()` - Automatically map BPMN tasks to queues based on candidate groups

**QueueAnalyticsService** - Comprehensive queue monitoring and analytics
- `getQueueDashboard()` - Overall queue statistics and health metrics
- `getQueueAnalytics()` - Detailed analytics for specific queues
- `getPerformanceMetrics()` - Task completion rates and backlog analysis

### Database Architecture

**Single Datasource** for both business and Flowable tables:
- **Business Tables**: `cases`, `allegations`, `work_items`, `users`, `roles`, `departments`, `case_types`
- **Queue Tables**: `queue_tasks`, `workflow_metadata` - Advanced task distribution system
- **Flowable Tables**: Auto-managed ACT_* tables for workflow engine
- **Schema**: All tables use `cms_flowable_workflow` schema
- **Connection Pool**: HikariCP with 20 max connections

#### Key Relationships
```
cases (1) → (n) allegations
cases (1) → (n) work_items  
cases (n) → (1) users (created_by, assigned_to)
cases (n) → (1) departments
cases (n) → (1) case_types
```

### Workflow Engine Configuration

- **BPMN Process**: `Process_CMS_Workflow_Updated` - Main case management workflow
- **DMN Decision**: `allegation-classification` - Routes cases to departments  
- **Process Variables**: Cases carry metadata through workflow lifecycle
- **User Tasks**: EO Intake → Classification → Department Processing → Investigation → Closure

## Important Configuration

### Application Context
- **Context Path**: `/api` - All endpoints prefixed with `/api`
- **Controller Mappings**: Use `/v1/cases` (not `/api/v1/cases`) due to context path
- **Full URL Example**: `http://localhost:8080/api/v1/cases`

### Environment Variables
Copy `.env.example` to `.env` and configure:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - PostgreSQL connection
- `JWT_SECRET` - JWT signing key
- `CERBOS_HOST`, `CERBOS_PORT` - Cerbos authorization service
- `DB_SCHEMA` - Database schema name (default: `cms_flowable_workflow`)

### Flowable Configuration
- **BPMN Engine**: Enabled - processes BPMN workflows
- **DMN Engine**: Enabled - evaluates decision tables with KIE/Drools
- **CMMN/Form/Content**: Disabled for performance
- **History Level**: Full audit trail retention
- **Auto-deployment**: From `src/main/resources/processes/` and `src/main/resources/dmn/`

## Key API Endpoints

### Authentication (`/auth`)
- `POST /auth/login` - User authentication (returns JWT token)
- Test credentials: `jow` / `demo123` or `admin` / `password123`

### Case Management (`/v1/cases`)
- `POST /v1/cases` - Create case with allegations (starts BPMN workflow)
- `GET /v1/cases/{caseNumber}` - Get case details with allegations
- `GET /v1/cases` - List all cases (paginated)

### Workflow Management (`/workflow`)
- `GET /workflow/tasks` - Get all active tasks
- `GET /workflow/tasks/user/{userId}` - Get user-specific tasks
- `POST /workflow/tasks/{taskId}/complete` - Complete workflow task

### Queue-Based Task Management (`/workflow/queue`)
- `GET /workflow/queue/{queueName}/tasks` - Get tasks from specific queue
- `GET /workflow/queue/{queueName}/next` - Get next available task from queue
- `POST /workflow/queue/tasks/{taskId}/claim` - Claim a task for user
- `POST /workflow/queue/tasks/{taskId}/unclaim` - Release claimed task back to queue
- `GET /workflow/queue/my-tasks` - Get current user's claimed tasks
- `GET /workflow/queue/statistics/{queueName}` - Get queue statistics
- `GET /workflow/queue/names` - Get all available queue names

### Workflow Metadata Management (`/workflow-metadata`)
- `POST /workflow-metadata/register` - Register workflow metadata with queue mappings
- `POST /workflow-metadata/deploy` - Deploy BPMN workflow and build task mappings
- `GET /workflow-metadata/{processDefinitionKey}` - Get workflow metadata
- `GET /workflow-metadata/active` - Get all active workflows
- `GET /workflow-metadata/deployed` - Get all deployed workflows

### Queue Analytics (`/queue/analytics`)
- `GET /queue/analytics/dashboard` - Overall queue analytics dashboard
- `GET /queue/analytics/{queueName}` - Detailed analytics for specific queue
- `GET /queue/analytics/workload-distribution` - Workload distribution across queues
- `GET /queue/analytics/performance` - Performance metrics and completion rates
- `GET /queue/analytics/health` - Queue health check based on task age and volume

### Flowable Deployment (`/v1/deploy`)
- `POST /v1/deploy/all` - Deploy all BPMN, DMN definitions
- `GET /v1/deploy/status` - Get deployment status

## Data Models & Patterns

### Core Entities
- `Case` - Parent entity with OneToMany relationships to `Allegation` and `WorkItem`
- `Allegation` - Child entity linked to cases and Flowable process instances
- `WorkItemEntity` - Workflow-trackable allegations stored in `work_items` table
- `User` with `Set<Role>` for security integration

### Business Flow
1. **Case Creation**: POST `/v1/cases` creates case + allegations + work_items
2. **Workflow Start**: Automatically starts BPMN process with case variables
3. **DMN Classification**: Evaluates allegation types and routes to departments
4. **Task Assignment**: Creates user tasks for appropriate roles (HR_SPECIALIST, LEGAL_COUNSEL, etc.)
5. **Process Completion**: Tasks progress through investigation to case closure

### Security Model
- **Stateless JWT**: 24-hour tokens, 7-day refresh tokens
- **Role-based Access**: HR_SPECIALIST, LEGAL_COUNSEL, SECURITY_ANALYST, DIRECTOR
- **Authorization Header**: `Bearer <jwt_token>`
- **Protected Endpoints**: All `/v1/*` routes require authentication

## Testing

### Postman Collections
- **Primary Collection**: `CMS_Flowable_API_Tests.postman_collection.json`
- **Environment**: `CMS_Flowable_Environment.postman_environment.json`
- **Automated Features**: JWT token management, variable chaining, response validation

### Test Workflow
1. Login to get JWT token
2. Create case with multiple allegations
3. Verify BPMN process started and tasks created
4. Complete HR → Legal → CSIS department tasks
5. Check system health endpoints

## Common Issues & Solutions

### Path Mapping Conflicts
**Problem**: 404 Not Found for API endpoints
**Solution**: Controllers use `/v1/cases` (not `/api/v1/cases`) due to context path `/api`

### UUID Conversion Errors  
**Problem**: Cannot convert UUID to Long
**Solution**: Use `(long) processInstance.getId().hashCode()` for database storage

### Enum Handling Issues
**Problem**: "Critical" vs "CRITICAL" conversion errors
**Solution**: Use custom conversion methods that handle both display names and constants

### JWT Authentication Failures
**Solutions**:
- Include `Authorization: Bearer <token>` header
- Check token expiration (24-hour expiry)
- Ensure "Bearer " prefix with space

### Flowable Process Start Failures
**Solutions**:
- Ensure BPMN files in `/resources/processes/`
- Check deployment logs, use `/v1/deploy/all` endpoint
- Verify process key matches `Process_CMS_Workflow_Updated`

## Debug Mode

Enable comprehensive logging:
```yaml
logging:
  level:
    org.flowable: DEBUG
    com.workflow.cmsflowable: DEBUG
    org.springframework.security: DEBUG
spring:
  jpa:
    show-sql: true
```

## Docker Services

Start Cerbos authorization service:
```bash
docker-compose up cerbos
```