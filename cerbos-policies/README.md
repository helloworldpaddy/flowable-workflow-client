# Cerbos Policy Directory

This directory contains the authorization policies and tests for the CMS Flowable application.

## Directory Structure

```
cerbos-policies/
├── resources/           # Resource-specific policies
│   └── case.yaml       # Case management authorization rules
├── _schemas/           # Schema definitions
│   ├── principal.yaml  # Principal attribute schema
│   └── resource_case.yaml # Case resource schema
├── tests/              # Policy test files
│   └── case_test.yaml  # Comprehensive authorization tests
├── test-runner.sh      # Script to run policy tests
└── README.md          # This file
```

## Running Policy Tests

To validate the authorization policies:

1. **Install Cerbos CLI** (if not already installed):
   ```bash
   # macOS
   brew install cerbos

   # Linux/Windows - see https://docs.cerbos.dev/cerbos/latest/installation.html
   ```

2. **Run the tests**:
   ```bash
   ./test-runner.sh
   ```

3. **Manual test execution**:
   ```bash
   # Validate policies
   cerbos compile .

   # Run specific test file
   cerbos run test --policy-dir . tests/case_test.yaml
   ```

## Authorization Model

The CMS Flowable application uses **Attribute-Based Access Control (ABAC)** with the following components:

### Principals (Users)
- **Attributes**: `userId`, `username`, `roles[]`, `is_manager`
- **Roles**: `DIRECTOR_GROUP`, `AROG_GROUP`, `INTAKE_ANALYST_GROUP`, etc.

### Resources (Cases)
- **Type**: `case`
- **Attributes**: `status`, `currentTaskGroup`, `assignedUsers[]`, `relevantDepartments[]`, etc.

### Actions
- **View Actions**: `view`
- **Workflow Actions**: `intake_initial_review`, `iu_intake_review`, `conduct_investigation`, `arog_review`, etc.

## Key Authorization Rules

1. **Universal View Access**: Directors and AROG members can view all cases
2. **Conditional View Access**: Other roles can view cases if:
   - They are assigned to the case, OR
   - Their role matches the current task group, OR
   - Their department is relevant to the case

3. **Workflow Actions**: Each workflow step has specific role and state requirements:
   - **Intake**: Only `INTAKE_ANALYST_GROUP` when status is `COMPLAINT_RECEIVED`
   - **Investigation**: Only `INVESTIGATOR_GROUP` when IP is approved
   - **AROG Review**: Only `AROG_GROUP` for cases flagged as AROG cases
   - **Final Review**: Only `DIRECTOR_GROUP` for cases in final review

## Integration with Spring Security

The policies are enforced through:
- `@PreAuthorize` annotations on controller methods
- `CasePermissionService` for programmatic checks
- `JwtPrincipalAttributeProvider` for extracting user attributes
- Custom `hasPermission()` expressions that call Cerbos

## Test Coverage

The test suite covers:
- ✅ View permissions for all user roles
- ✅ Workflow action permissions at each stage
- ✅ State-dependent authorization (status, approvals, flags)
- ✅ Negative test cases (unauthorized actions)
- ✅ Role hierarchy and escalation scenarios