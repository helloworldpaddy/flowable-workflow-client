# CMS Flowable Workflow Testing Guide

## **Prerequisites**

1. **Application Running**: Ensure the application is started with `mvn spring-boot:run`
2. **Database Connected**: PostgreSQL database should be accessible
3. **Ports Available**: Port 8080 should be free for the application

## **Access Points**

### **Swagger UI**
- **URL**: `http://localhost:8080/api/swagger-ui/index.html`
- **API Docs**: `http://localhost:8080/api/v3/api-docs`

### **Health Endpoints**
- **Health**: `http://localhost:8080/api/actuator/health`
- **Info**: `http://localhost:8080/api/actuator/info`
- **Flowable**: `http://localhost:8080/api/actuator/flowable`

## **Postman Setup**

### **1. Import Collection**
1. Open Postman
2. Click **Import** 
3. Select `CMS_Flowable_API_Tests.postman_collection.json`
4. Import `CMS_Flowable_Environment.postman_environment.json`

### **2. Set Environment**
1. Select "CMS Flowable Development" environment
2. Verify base_url is set to `http://localhost:8080/api`

## **Testing Scenarios**

### **Scenario 1: Complete Workflow Test**

#### **Step 1: Create Case with Multiple Allegations**
```json
POST /api/v1/cases
{
  "title": "Workplace Harassment Investigation",
  "description": "Multiple allegations requiring HR, Legal, and CSIS review",
  "complainantName": "Sarah Johnson",
  "complainantEmail": "sarah.johnson@company.com", 
  "priority": "HIGH",
  "departmentId": 1,
  "caseTypeId": 2,
  "allegations": [
    {
      "allegationType": "HARASSMENT",
      "severity": "HIGH",
      "description": "Verbal harassment and inappropriate comments",
      "involvedPersons": ["Manager Name", "Witness Name"],
      "incidentDate": "2025-06-10"
    },
    {
      "allegationType": "DISCRIMINATION", 
      "severity": "MEDIUM",
      "description": "Gender-based discrimination in promotions",
      "involvedPersons": ["Manager Name", "HR Director"],
      "incidentDate": "2025-06-05"
    },
    {
      "allegationType": "RETALIATION",
      "severity": "HIGH", 
      "description": "Punitive actions after filing complaint",
      "involvedPersons": ["Manager Name"],
      "incidentDate": "2025-06-12"
    }
  ]
}
```

**Expected Response:**
- Status: `201 Created`
- Case Number: `CMS001` (or similar)
- Workflow Instance created
- Initial task assigned to HR department

#### **Step 2: Verify Case Creation**
```http
GET /api/v1/cases/{caseNumber}
```

**Expected Response:**
- Status: `200 OK`
- Complete case details with 3 allegations
- Workflow status: `ACTIVE`

#### **Step 3: Complete HR Review Task**
```json
POST /api/v1/cases/tasks/complete
{
  "taskId": "hr-review-task-id",
  "userId": 1,
  "decision": "APPROVE",
  "comments": "HR review completed. Case requires legal review.",
  "variables": {
    "hrReviewComments": "Serious harassment allegations confirmed",
    "nextDepartment": "LEGAL",
    "urgencyLevel": "HIGH"
  }
}
```

#### **Step 4: Complete Legal Review Task**
```json
POST /api/v1/cases/tasks/complete
{
  "taskId": "legal-review-task-id", 
  "userId": 2,
  "decision": "APPROVE",
  "comments": "Legal liability confirmed. Recommend CSIS investigation.",
  "variables": {
    "legalRisk": "HIGH",
    "nextDepartment": "CSIS",
    "disciplinaryActionRecommended": true
  }
}
```

#### **Step 5: Complete CSIS Investigation**
```json
POST /api/v1/cases/tasks/complete
{
  "taskId": "csis-investigation-task-id",
  "userId": 3,
  "decision": "APPROVE", 
  "comments": "Investigation completed. Evidence preserved.",
  "variables": {
    "evidencePreserved": true,
    "recommendedAction": "TERMINATION",
    "caseStatus": "READY_FOR_CLOSURE"
  }
}
```

### **Scenario 2: Different Case Types**

#### **Legal Department Case**
```json
POST /api/v1/cases
{
  "title": "Contract Violation Investigation",
  "description": "Breach of employment contract and confidentiality",
  "complainantName": "David Brown",
  "complainantEmail": "david.brown@company.com",
  "priority": "MEDIUM",
  "departmentId": 3,
  "caseTypeId": 1,
  "allegations": [
    {
      "allegationType": "CONTRACT_VIOLATION",
      "severity": "MEDIUM",
      "description": "Non-compete clause violation",
      "involvedPersons": ["Employee Name"],
      "incidentDate": "2025-06-08"
    }
  ]
}
```

#### **HR Policy Case**
```json
POST /api/v1/cases
{
  "title": "Policy Violation Review",
  "description": "Multiple policy violations requiring disciplinary action",
  "complainantName": "Lisa Chen", 
  "complainantEmail": "lisa.chen@company.com",
  "priority": "LOW",
  "departmentId": 2,
  "caseTypeId": 3,
  "allegations": [
    {
      "allegationType": "POLICY_VIOLATION",
      "severity": "LOW",
      "description": "Attendance and tardiness issues",
      "involvedPersons": ["Employee Name"],
      "incidentDate": "2025-06-01"
    },
    {
      "allegationType": "MISCONDUCT",
      "severity": "MEDIUM",
      "description": "Misuse of company resources",
      "involvedPersons": ["Employee Name"],
      "incidentDate": "2025-06-03"
    }
  ]
}
```

## **Validation Checklist**

### **Case Creation Validation**
- [ ] Case number generated automatically (CMS prefix)
- [ ] All 3 allegations created and linked to case
- [ ] Workflow instance started
- [ ] Initial task created and assigned
- [ ] Case status set to ACTIVE

### **Workflow Progression Validation**
- [ ] HR task completion routes to Legal department
- [ ] Legal task completion routes to CSIS department  
- [ ] CSIS task completion closes workflow
- [ ] Task variables properly stored
- [ ] Comments recorded for audit trail

### **Data Integrity Validation**
- [ ] Case-Allegation relationships maintained
- [ ] User assignments tracked
- [ ] Timestamps recorded accurately
- [ ] Priority levels respected in routing

### **API Response Validation**
- [ ] Proper HTTP status codes returned
- [ ] Complete JSON responses with all required fields
- [ ] Error handling for invalid requests
- [ ] Authentication/authorization working

## **DMN Decision Testing**

The application uses DMN for allegation classification. Test these scenarios:

### **High Priority Routing**
- Harassment + High Severity → HR → Legal → CSIS
- Discrimination + High Severity → HR → Legal → CSIS

### **Medium Priority Routing**  
- Contract Violations → Legal → HR
- Policy Violations + Medium Severity → HR → Legal

### **Low Priority Routing**
- Minor Policy Violations → HR only
- Administrative Issues → HR only

## **Monitoring and Logs**

### **Application Logs**
Monitor console output for:
- Workflow instance creation
- Task assignments
- Decision evaluation results
- Database operations

### **Database Verification**
Check these tables for data:
- `cms_flowable_workflow.cases`
- `cms_flowable_workflow.allegations` 
- Flowable tables (ACT_RU_TASK, ACT_HI_PROCINST)

### **Health Monitoring**
Regular checks:
- `GET /actuator/health` - Overall application health
- `GET /actuator/flowable` - Flowable engine status
- `GET /actuator/metrics` - Performance metrics

## **Troubleshooting**

### **Common Issues**

1. **Application Not Starting**
   - Check port 8080 availability
   - Verify database connection
   - Review application logs

2. **Tasks Not Creating**
   - Verify BPMN process deployed
   - Check DMN rules evaluation
   - Confirm user/role assignments

3. **Workflow Not Progressing**
   - Verify task completion API calls
   - Check decision variables
   - Review workflow definition

### **Debug Commands**

```bash
# Check application status
curl http://localhost:8080/api/actuator/health

# Check Flowable deployment
curl http://localhost:8080/api/actuator/flowable

# View application logs
mvn spring-boot:run > application.log 2>&1
```

## **Performance Testing**

### **Load Testing Scenarios**
1. Create 10 cases simultaneously
2. Complete 5 tasks in parallel
3. Query case details for 20 cases
4. Monitor response times and memory usage

### **Expected Performance**
- Case creation: < 2 seconds
- Task completion: < 1 second
- Case retrieval: < 500ms
- Workflow progression: < 3 seconds

## **Success Criteria**

**Functional Requirements**
- Cases created with multiple allegations
- Workflow routing follows DMN rules
- All 3 departments (HR, Legal, CSIS) process tasks
- Audit trail maintained throughout

**Technical Requirements**  
- API responses within performance thresholds
- Database integrity maintained
- Error handling works correctly
- Authentication/authorization enforced

**Business Requirements**
- Case numbering system works
- Priority levels affect routing
- Comments and decisions recorded
- Workflow completes successfully