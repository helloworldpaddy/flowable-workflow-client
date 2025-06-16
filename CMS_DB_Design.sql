
CREATE TABLE cms_flowable_workflow.users (
	id bigserial NOT NULL,
	user_id int4 DEFAULT nextval('cms_flowable_workflow.users_id_seq'::regclass) NOT NULL,
	username varchar(50) NOT NULL,
	email varchar(255) NOT NULL,
	password_hash varchar(255) NOT NULL,
	first_name varchar(100) NOT NULL,
	last_name varchar(100) NOT NULL,
	user_status varchar(20) DEFAULT 'ACTIVE'::character varying NULL,
	created_at timestamp DEFAULT now() NULL,
	updated_at timestamp DEFAULT now() NULL,
	CONSTRAINT users_email_key UNIQUE (email),
	CONSTRAINT users_pkey PRIMARY KEY (id),
	CONSTRAINT users_user_id_key UNIQUE (user_id),
	CONSTRAINT users_username_key UNIQUE (username)
);
CREATE INDEX idx_users_email ON cms_flowable_workflow.users USING btree (email);
CREATE INDEX idx_users_user_id ON cms_flowable_workflow.users USING btree (user_id);
CREATE INDEX idx_users_username ON cms_flowable_workflow.users USING btree (username);

CREATE TABLE cms_flowable_workflow.roles (
	id bigserial NOT NULL,
	role_id int4 DEFAULT nextval('cms_workflow.roles_id_seq'::regclass) NOT NULL,
	role_code varchar(50) NOT NULL,
	role_name varchar(100) NOT NULL,
	role_description text NULL,
	access_level varchar(20) DEFAULT 'USER'::character varying NULL,
	is_active bool DEFAULT true NULL,
	created_at timestamp DEFAULT now() NULL,
	updated_at timestamp DEFAULT now() NULL,
	CONSTRAINT roles_pkey PRIMARY KEY (id),
	CONSTRAINT roles_role_code_key UNIQUE (role_code),
	CONSTRAINT roles_role_id_key UNIQUE (role_id)
);
CREATE INDEX idx_roles_role_id ON cms_flowable_workflow.roles USING btree (role_id);

CREATE TABLE cms_flowable_workflow.user_roles (
	id bigserial NOT NULL,
	user_role_id int4 DEFAULT nextval('cms_workflow.user_roles_id_seq'::regclass) NOT NULL,
	user_id int4 NOT NULL,
	role_id int4 NOT NULL,
	assigned_date date DEFAULT CURRENT_DATE NULL,
	is_active bool DEFAULT true NULL,
	created_at timestamp DEFAULT now() NULL,
	updated_at timestamp DEFAULT now() NULL,
	CONSTRAINT user_roles_pkey PRIMARY KEY (id),
	CONSTRAINT user_roles_user_id_role_id_key UNIQUE (user_id, role_id),
	CONSTRAINT user_roles_user_role_id_key UNIQUE (user_role_id),
	CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES cms_flowable_workflow.roles(role_id) ON DELETE CASCADE,
	CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES cms_flowable_workflow.users(user_id) ON DELETE CASCADE
);


CREATE TABLE cms_flowable_workflow.departments (
	id bigserial NOT NULL,
	department_id int4 DEFAULT nextval('cms_workflow.departments_id_seq'::regclass) NOT NULL,
	department_code varchar(50) NOT NULL,
	department_name varchar(100) NOT NULL,
	department_description text NULL,
	manager_user_id int4 NULL,
	is_active bool DEFAULT true NULL,
	created_at timestamp DEFAULT now() NULL,
	updated_at timestamp DEFAULT now() NULL,
	CONSTRAINT departments_department_code_key UNIQUE (department_code),
	CONSTRAINT departments_department_id_key UNIQUE (department_id),
	CONSTRAINT departments_pkey PRIMARY KEY (id),
	CONSTRAINT fk_departments_manager_user_id FOREIGN KEY (manager_user_id) REFERENCES cms_flowable_workflow.users(user_id) ON DELETE SET NULL
);

CREATE TABLE cms_flowable_workflow.cases (
	case_id varchar(50) NOT NULL,
	case_number varchar(100) NOT NULL,
	case_title varchar(255) NULL,
	description text NULL,
	status varchar(50) NULL,
	priority varchar(20) NULL,
	created_by varchar(50) NULL,
	assigned_to varchar(50) NULL,
	flowable_case_instance_id varchar(100) NULL,
	created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	CONSTRAINT cases_case_number_key UNIQUE (case_number),
	CONSTRAINT cases_pkey PRIMARY KEY (case_id)
);


CREATE TABLE cms_flowable_workflow.work_items (
	work_item_id varchar(50) NOT NULL,
	work_item_number varchar(100) NOT NULL,
	"type" varchar(100) NOT NULL,
	severity varchar(50) NOT NULL,
	description text NULL,
	status varchar(50) NOT NULL,
	classification varchar(100) NULL,
	assigned_group varchar(100) NULL,
	priority varchar(50) NULL,
	flowable_process_instance_id varchar(255) NULL,
	created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	CONSTRAINT work_items_flowable_process_instance_id_key UNIQUE (flowable_process_instance_id),
	CONSTRAINT work_items_pkey PRIMARY KEY (work_item_id),
	CONSTRAINT work_items_work_item_number_key UNIQUE (work_item_number)
);

CREATE TABLE cms_flowable_workflow.allegations (
	allegation_id varchar(50) NOT NULL,
	case_id varchar(50) NOT NULL,
	allegation_type varchar(100) NOT NULL,
	severity varchar(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
	description text NULL,
	department_classification varchar(50) NULL,
	assigned_group varchar(100) NULL,
	flowable_plan_item_id varchar(255) NULL,
	created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	CONSTRAINT allegations_pkey PRIMARY KEY (allegation_id),
	CONSTRAINT fk_allegations_case_id FOREIGN KEY (case_id) REFERENCES cms_flowable_workflow.cases(case_id) ON DELETE CASCADE
);
CREATE INDEX idx_allegations_case_id ON cms_flowable_workflow.allegations USING btree (case_id);
CREATE INDEX idx_allegations_type ON cms_flowable_workflow.allegations USING btree (allegation_type);
CREATE INDEX idx_allegations_severity ON cms_flowable_workflow.allegations USING btree (severity);