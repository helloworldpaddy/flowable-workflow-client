# CMS Flowable Database Migration Strategy

## Overview

This comprehensive Liquibase migration strategy provides complete database lifecycle management for the CMS Flowable application across different environments (development, testing, staging, production).

## Migration Structure

```
db/changelog/
├── db.changelog-master.xml           # Master changelog file
├── setup/                           # Environment setup
│   └── 001-environment-setup.xml    # Schema, extensions, enums
├── schema/                          # Database schema
│   ├── 001-create-core-tables.xml   # Users, roles, departments
│   ├── 002-create-workflow-tables.xml # Cases, allegations, work items
│   ├── 003-create-audit-tables.xml  # Audit and monitoring tables
│   ├── 004-create-indexes.xml       # Performance indexes
│   └── 005-create-constraints.xml   # Foreign keys and constraints
├── data/                           # Reference and seed data
│   ├── 001-insert-system-roles.xml  # System roles and groups
│   ├── 002-insert-case-types.xml    # Case type definitions
│   ├── 003-insert-departments.xml   # Organizational departments
│   ├── 004-insert-admin-users.xml   # Administrative users
│   ├── 005-insert-test-data.xml     # Test data (test context)
│   └── 006-insert-demo-data.xml     # Demo data (demo context)
├── migration/                      # Legacy data migration
│   └── 001-legacy-data-migration.xml # Templates for legacy data
├── performance/                    # Performance optimizations
│   ├── 001-create-performance-indexes.xml # Advanced indexes
│   └── 002-create-partitions.xml    # Table partitioning
└── security/                      # Security and compliance
    ├── 001-create-security-policies.xml # RLS and database roles
    └── 002-create-audit-triggers.xml    # Audit triggers and functions
```

## Environment Configuration

### 1. Development Environment

```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    default-schema: cms_flowable_workflow
    enabled: true
    contexts: dev,test
```

### 2. Testing Environment

```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    default-schema: cms_flowable_workflow
    enabled: true
    contexts: test
```

### 3. Staging Environment

```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    default-schema: cms_flowable_workflow
    enabled: true
    contexts: staging
```

### 4. Production Environment

```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    default-schema: cms_flowable_workflow
    enabled: true
    contexts: production
```

## Database Setup Commands

### Fresh Installation

```bash
# 1. Create database and user (run as database admin)
CREATE DATABASE workflow;
CREATE USER cms_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE workflow TO cms_user;

# 2. Update application.yml with database connection details
spring:
  datasource:
    url: jdbc:postgresql://your-host:5432/workflow
    username: cms_user
    password: secure_password

# 3. Run the application - Liquibase will handle the rest
mvn spring-boot:run
```

### Migrating from Existing Database

```bash
# 1. Backup existing database
pg_dump existing_db > backup.sql

# 2. Create new database with Liquibase structure
# Follow fresh installation steps

# 3. Migrate data using legacy migration templates
# Customize migration/001-legacy-data-migration.xml as needed

# 4. Run migration
mvn spring-boot:run
```

## Migration Features

### 1. Schema Management
- **Complete schema creation** with all tables, indexes, and constraints
- **Environment-specific configurations** using Liquibase contexts
- **Rollback support** for all changes
- **Incremental updates** for schema evolution

### 2. Reference Data Management
- **System roles and permissions** for RBAC
- **Case types and departments** for business logic
- **Administrative users** with proper role assignments
- **Test and demo data** for development environments

### 3. Performance Optimizations
- **Comprehensive indexing strategy** for all query patterns
- **Composite and partial indexes** for complex queries
- **GIN indexes** for full-text search and JSON queries
- **Table partitioning** for audit logs by month
- **Automated partition management** functions

### 4. Security and Compliance
- **Row-Level Security (RLS)** policies for data access control
- **Database roles** with principle of least privilege
- **Audit triggers** for automatic change tracking
- **Comprehensive audit trail** with user context

### 5. Legacy Migration Support
- **Baseline support** for existing installations
- **Template migration scripts** for common scenarios
- **Safe migration practices** with backup strategies

## User Accounts Created

### Administrative Users
| Username | Password | Role | Description |
|----------|----------|------|-------------|
| `admin` | `password123` | ADMIN | System administrator |
| `ethics_director` | `password123` | DIRECTOR, DIRECTOR_GROUP | Ethics office director |

### Service Users
| Username | Password | Roles | Description |
|----------|----------|-------|-------------|
| `hr_specialist` | `password123` | HR_SPECIALIST, HR_GROUP | HR department specialist |
| `legal_counsel` | `password123` | LEGAL_COUNSEL, LEGAL_GROUP | Legal department counsel |
| `security_analyst` | `password123` | SECURITY_ANALYST, CSIS_GROUP | Security/CSIS analyst |
| `lead_investigator` | `password123` | INVESTIGATOR, INVESTIGATOR_GROUP | Lead investigator |
| `intake_analyst` | `password123` | INTAKE_ANALYST, INTAKE_ANALYST_GROUP | Intake processing analyst |

### Test Users (test context only)
| Username | Password | Role | Description |
|----------|----------|------|-------------|
| `jow` | `password123` | USER | Basic test user |
| `case_manager` | `password123` | CASE_MANAGER | Case management testing |
| `reviewer` | `password123` | REVIEWER | Quality assurance testing |

## Security Features

### Row-Level Security (RLS)
- **Case access control** based on user roles and assignments
- **Department-based access** for specialized roles
- **Notification privacy** - users see only their own notifications
- **Admin override** for administrative access

### Database Roles
- **cms_app_admin**: Full database access for administrators
- **cms_app_user**: Limited access for application users
- **cms_app_readonly**: Read-only access for reporting
- **cms_app_service**: Application service account

### Audit Trail
- **Automatic audit logging** for all sensitive table changes
- **User context tracking** including IP address and user agent
- **Comprehensive change tracking** with before/after values
- **Partitioned audit logs** for performance and retention

## Maintenance Operations

### Partition Management
```sql
-- Create new monthly partition for audit logs
SELECT cms_flowable_workflow.create_monthly_audit_partition('2025-07-01');

-- Drop old partitions (keeping last 24 months)
SELECT cms_flowable_workflow.drop_old_audit_partitions(24);
```

### Performance Monitoring
```sql
-- Check table sizes
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'cms_flowable_workflow'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check index usage
SELECT schemaname, tablename, indexname, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
WHERE schemaname = 'cms_flowable_workflow'
ORDER BY idx_tup_read DESC;
```

### Backup Strategy
```bash
# Full database backup
pg_dump -h host -U user -d database -f backup_$(date +%Y%m%d).sql

# Schema-only backup
pg_dump -h host -U user -d database -s -f schema_backup_$(date +%Y%m%d).sql

# Data-only backup
pg_dump -h host -U user -d database -a -f data_backup_$(date +%Y%m%d).sql
```

## Troubleshooting

### Common Issues

1. **Permission Denied Errors**
   - Ensure database user has sufficient privileges
   - Check if RLS policies are correctly configured
   - Verify database role assignments

2. **Migration Failures**
   - Check Liquibase lock table: `SELECT * FROM databasechangeloglock`
   - Clear locks if needed: `UPDATE databasechangeloglock SET locked=false`
   - Review failed changeset in logs

3. **Performance Issues**
   - Check if indexes are being used: `EXPLAIN ANALYZE <query>`
   - Verify partition pruning for audit logs
   - Monitor connection pool settings

### Rollback Procedures

```bash
# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Rollback to specific tag
mvn liquibase:rollback -Dliquibase.rollbackTag=baseline

# Rollback to specific date
mvn liquibase:rollback -Dliquibase.rollbackDate=2025-01-01
```

## Best Practices

1. **Always backup before migration** in production environments
2. **Test migrations** thoroughly in development and staging
3. **Use contexts** to control environment-specific data
4. **Monitor performance** after index and partition changes
5. **Regular maintenance** of partitions and audit logs
6. **Security reviews** of RLS policies and database roles
7. **Version control** all migration files
8. **Document custom changes** in migration comments

## Support

For issues related to database migration:
1. Check application logs for Liquibase errors
2. Review database logs for constraint violations
3. Consult Liquibase documentation for advanced features
4. Contact the development team for custom migration needs

---

**Note**: This migration strategy is designed to be environment-agnostic and can be adapted for different database configurations by updating the `application.yml` database connection settings.