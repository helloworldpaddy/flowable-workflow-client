# CMS Flowable End-to-End Testing Instructions

## Overview

This guide provides step-by-step instructions for running comprehensive end-to-end tests for the CMS Flowable workflow system. The test suite validates the complete workflow from case creation to closure, covering all user roles and process transitions.

## Prerequisites

### 1. System Setup

- **Application Running**: Ensure your CMS Flowable Spring Boot application is running on `localhost:8080`
- **Database**: Verify database is initialized with proper schema and sample data
- **Postman**: Install Postman application or use Postman web version

### 2. Test Data Requirements

Create the following test users in your database before running tests:

```sql
-- Test users for E2E testing
INSERT INTO users (user_id, username, email, password_hash, first_name, last_name, user_status) VALUES
(1001, 'intake.officer', 'intake.officer@company.com', '[HASHED_PASSWORD]', 'Intake', 'Officer', 'ACTIVE'),
(1002, 'hr.specialist', 'hr.specialist@company.com', '[HASHED_PASSWORD]', 'HR', 'Specialist', 'ACTIVE'),
(1003, 'legal.counsel', 'legal.counsel@company.com', '[HASHED_PASSWORD]', 'Legal', 'Counsel', 'ACTIVE'),
(1004, 'csis.analyst', 'csis.analyst@company.com', '[HASHED_PASSWORD]', 'CSIS', 'Analyst', 'ACTIVE'),
(1005, 'investigator', 'investigator@company.com', '[HASHED_PASSWORD]', 'Lead', 'Investigator', 'ACTIVE'),
(1006, 'director', 'director@company.com', '[HASHED_PASSWORD]', 'Ethics', 'Director', 'ACTIVE');

-- Test roles
INSERT INTO roles (role_id, role_code, role_name, role_description, access_level) VALUES
(101, 'INTAKE_OFFICER', 'Intake Officer', 'Handles initial case intake and processing', 'STANDARD'),
(102, 'HR_SPECIALIST', 'HR Specialist', 'Handles HR-related allegations', 'DEPARTMENTAL'),
(103, 'LEGAL_COUNSEL', 'Legal Counsel', 'Handles legal matters and compliance', 'DEPARTMENTAL'),
(104, 'SECURITY_ANALYST', 'Security Analyst', 'Handles security and CSIS investigations', 'DEPARTMENTAL'),
(105, 'INVESTIGATOR', 'Investigator', 'Conducts detailed investigations', 'SENIOR'),
(106, 'DIRECTOR', 'Director', 'Approves case closures and final decisions', 'EXECUTIVE');

-- User-role assignments
INSERT INTO user_roles (user_id, role_id) VALUES
(1001, 101), (1002, 102), (1003, 103), (1004, 104), (1005, 105), (1006, 106);
```

### 3. Flowable Groups Configuration

Ensure your Flowable engine has the following candidate groups configured:

- `INTAKE_ANALYST_GROUP`
- `HR_GROUP` / `HR_SPECIALIST`
- `LEGAL_GROUP` / `LEGAL_COUNSEL`
- `CSIS_GROUP` / `SECURITY_ANALYST`
- `INVESTIGATOR_GROUP`
- `DIRECTOR_GROUP`

## Test Execution Steps

### Step 1: Import Test Collection

1. Open Postman
2. Click **Import** button
3. Select the file: `CMS_Flowable_E2E_Testing.postman_collection.json`
4. Verify the collection is imported with 7 main folders

### Step 2: Configure Environment Variables

1. In Postman, ensure the collection variables are set:
   - `base_url`: `http://localhost:8080/api`
   - All other variables will be automatically populated during test execution

### Step 3: Execute Tests Sequentially

#### Phase 1: Authentication Setup (Folder 01)

**Purpose**: Authenticate as Intake Officer to start the workflow

1. **01.1 Login as Intake Officer**
   - Authenticates the intake officer
   - Sets JWT token for subsequent requests
   - ✅ **Expected**: 200 OK, JWT token stored

#### Phase 2: Case Creation & Process Start (Folder 02)

**Purpose**: Create a multi-department case and start the BPMN workflow

2. **02.1 Create Multi-Department Case**
   - Creates case with HR, Legal, and CSIS allegations
   - ✅ **Expected**: 201 Created, case number generated

3. **02.2 Start Workflow Process**
   - Initiates the `Process_CMS_Workflow` BPMN process
   - ✅ **Expected**: 200 OK, process instance ID generated

#### Phase 3: EO Intake Process (Folder 03)

**Purpose**: Complete the initial intake task

4. **03.1 Get Intake Tasks**
   - Retrieves tasks assigned to intake officer
   - ✅ **Expected**: 200 OK, `Task_EO_Intake` found

5. **03.2 Complete EO Intake Task**
   - Completes the intake process
   - Triggers classification business rule
   - ✅ **Expected**: 200 OK, task completed

#### Phase 4: Department Assignment Process (Folder 04)

**Purpose**: Process department assignments (HR, Legal, CSIS)

6. **04.1 Login as HR Specialist**
   - Switch to HR specialist authentication
   - ✅ **Expected**: 200 OK, HR specialist JWT token

7. **04.2 Get HR Assignment Tasks**
   - Retrieve HR assignment tasks
   - ✅ **Expected**: 200 OK, `Task_HR_Assignment` found

8. **04.3 Complete HR Assignment Task**
   - Complete HR department assignment
   - ✅ **Expected**: 200 OK, HR assignment completed

9. **04.4 Login as Legal Counsel**
   - Switch to legal counsel authentication
   - ✅ **Expected**: 200 OK, legal counsel JWT token

10. **04.5 Get Legal Assignment Tasks**
    - Retrieve legal assignment tasks
    - ✅ **Expected**: 200 OK, `Task_Legal_Assignment` found

11. **04.6 Complete Legal Assignment Task**
    - Complete legal department assignment
    - ✅ **Expected**: 200 OK, legal assignment completed

12. **04.7 Login as CSIS Analyst**
    - Switch to CSIS analyst authentication
    - ✅ **Expected**: 200 OK, CSIS analyst JWT token

13. **04.8 Get CSIS Assignment Tasks**
    - Retrieve CSIS assignment tasks
    - ✅ **Expected**: 200 OK, `Task_CSIS_Assignment` found

14. **04.9 Complete CSIS Assignment Task**
    - Complete CSIS department assignment
    - ✅ **Expected**: 200 OK, CSIS assignment completed

#### Phase 5: Investigation Process (Folder 05)

**Purpose**: Conduct the investigation after all departments are assigned

15. **05.1 Login as Investigator**
    - Switch to investigator authentication
    - ✅ **Expected**: 200 OK, investigator JWT token

16. **05.2 Get Investigation Tasks**
    - Retrieve investigation tasks
    - ✅ **Expected**: 200 OK, `Task_Investigation` found

17. **05.3 Complete Investigation Task**
    - Complete the investigation process
    - ✅ **Expected**: 200 OK, investigation completed

#### Phase 6: Case Closure Process (Folder 06)

**Purpose**: Close the case with director approval

18. **06.1 Login as Director**
    - Switch to director authentication
    - ✅ **Expected**: 200 OK, director JWT token

19. **06.2 Get Case Closure Tasks**
    - Retrieve case closure tasks
    - ✅ **Expected**: 200 OK, `Task_Case_Closure` found

20. **06.3 Complete Case Closure Task**
    - Complete the case closure process
    - ✅ **Expected**: 200 OK, case closure completed

#### Phase 7: Verification & Cleanup (Folder 07)

**Purpose**: Verify the complete workflow execution

21. **07.1 Verify Process Completion**
    - Check that BPMN process instance is completed
    - ✅ **Expected**: 200 OK, process ended successfully

22. **07.2 Verify Case Status**
    - Verify final case status
    - ✅ **Expected**: 200 OK, case status updated

## Test Execution Methods

### Method 1: Manual Sequential Execution

1. Execute each request individually in order
2. Verify test results after each step
3. Check console logs for success/failure messages
4. Recommended for debugging and understanding workflow

### Method 2: Collection Runner

1. Click **Runner** in Postman
2. Select the E2E Testing collection
3. Configure:
   - **Iterations**: 1
   - **Delay**: 1000ms between requests
   - **Data File**: None needed
4. Click **Run CMS Flowable E2E Workflow Testing**
5. Monitor execution in real-time

### Method 3: Newman CLI (Automated)

```bash
# Install Newman if not already installed
npm install -g newman

# Run the collection
newman run CMS_Flowable_E2E_Testing.postman_collection.json \
  --delay-request 1000 \
  --timeout-request 30000 \
  --reporters cli,html \
  --reporter-html-export e2e-test-results.html
```

## Expected Workflow Flow

```
Start Event: Case Created
    ↓
EO Intake Task (intake.officer)
    ↓
Classification Business Rule
    ↓
Gateway: Multi-Department Route
    ↓
Parallel Department Processing Subprocess:
    ├── HR Assignment Task (hr.specialist)
    ├── Legal Assignment Task (legal.counsel)
    └── CSIS Assignment Task (csis.analyst)
    ↓
Investigation Task (investigator)
    ↓
Case Closure Task (director)
    ↓
End Event: Case Closed
```

## Success Criteria

### ✅ Test Passes When:

- All 22 test requests return expected HTTP status codes
- JWT tokens are successfully generated and used
- Task IDs are properly captured and used in subsequent requests
- BPMN process completes without errors
- Case status progresses through all stages
- Console shows "🎉 E2E Test completed successfully!"

### ❌ Test Fails When:

- Authentication failures (401 Unauthorized)
- Task not found errors (404 Not Found)
- BPMN process errors (500 Internal Server Error)
- Missing or invalid task IDs
- Process instance doesn't complete

## Troubleshooting

### Common Issues:

1. **Authentication Failures**
   - Verify test users exist in database
   - Check password hashing matches your implementation
   - Ensure JWT secret is consistent

2. **Task Not Found**
   - Verify BPMN process is deployed correctly
   - Check candidate group assignments
   - Ensure process instance is created

3. **Process Not Starting**
   - Verify Flowable engine is running
   - Check BPMN file syntax
   - Ensure process key matches: `Process_CMS_Workflow`

4. **Database Issues**
   - Verify schema is created
   - Check foreign key constraints
   - Ensure test data is properly inserted

### Debug Steps:
1. Check application logs for errors
2. Verify database state after each step
3. Use Flowable Admin UI to monitor process execution
4. Check individual API endpoints manually

## Test Data Cleanup

After testing, you may want to clean up test data:

```sql
-- Remove test case and related data
DELETE FROM allegations WHERE case_number LIKE 'CMS-%';
DELETE FROM cases WHERE case_number LIKE 'CMS-%';

-- Keep test users for future testing or remove them
-- DELETE FROM user_roles WHERE user_id BETWEEN 1001 AND 1006;
-- DELETE FROM users WHERE user_id BETWEEN 1001 AND 1006;
```

## Performance Metrics

Monitor the following during testing:

- **Total Execution Time**: Should complete in < 2 minutes
- **Individual Request Time**: Most requests < 1 second
- **Database Response Time**: Queries should be optimized
- **Memory Usage**: Monitor for memory leaks

## Continuous Integration

To integrate with CI/CD:

```yaml
# Example GitHub Actions workflow
name: E2E Tests
on: [push, pull_request]
jobs:
  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Start Application
        run: ./mvnw spring-boot:run &
      - name: Wait for Application
        run: ./wait-for-it.sh localhost:8080 --timeout=60
      - name: Run E2E Tests
        run: newman run CMS_Flowable_E2E_Testing.postman_collection.json
```

This comprehensive test suite validates your entire CMS Flowable workflow system and ensures all user roles and process transitions work correctly.
