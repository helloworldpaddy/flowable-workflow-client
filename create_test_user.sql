-- Create test user 'jow' with password 'password123'
INSERT INTO cms_flowable_workflow.users (user_id, username, email, password_hash, first_name, last_name, user_status, created_at, updated_at)
VALUES (1, 'jow', 'jow@company.com', '$2a$10$grHjQhl7qdK4ERNdViMDw.ffg5B8A6x5WRibGStY3nfHlJBVV45LW', 'John', 'Doe', 'ACTIVE', now(), now());

-- Create a basic USER role if it doesn't exist
INSERT INTO cms_flowable_workflow.roles (role_id, role_code, role_name, role_description, access_level, is_active, created_at, updated_at)
VALUES (1, 'USER', 'Basic User', 'Basic user with standard permissions', 'USER', true, now(), now())
ON CONFLICT (role_code) DO NOTHING;

-- Assign the USER role to jow
INSERT INTO cms_flowable_workflow.user_roles (user_role_id, user_id, role_id, assigned_date, is_active, created_at, updated_at)
VALUES (1, 1, 1, CURRENT_DATE, true, now(), now())
ON CONFLICT (user_id, role_id) DO NOTHING;