-- Drop table

-- DROP TABLE public.project

CREATE TABLE public.project (
	project_key int8 DEFAULT unique_rowid() NOT NULL,
	project_id text(2147483647) NOT NULL,
	"name" text(2147483647) NOT NULL,
	description text(2147483647),
	workspace_id text(2147483647) NOT NULL,
	"version" int8 DEFAULT 0:::INT
) ;

-- Drop table

-- DROP TABLE public.permission

CREATE TABLE public.permission (
	permission_key int8 DEFAULT unique_rowid() NOT NULL,
	entity_id text(2147483647) NOT NULL,
	username text(2147483647) NOT NULL,
	created_at timestamptz NOT NULL,
	permission_type text(2147483647) NOT NULL,
	access_type text(2147483647) NOT NULL
) ;

-- Drop table

-- DROP TABLE public.workspace

CREATE TABLE public.workspace (
	workspace_key int8 DEFAULT unique_rowid() NOT NULL,
	workspace_id text(2147483647) NOT NULL,
	"name" text(2147483647) NOT NULL,
	description text(2147483647),
	read_only bool DEFAULT false NOT NULL,
	"version" int8 DEFAULT 0:::INT
) ;

-- Drop table

-- DROP TABLE public.asset

CREATE TABLE public.asset (
	asset_key int8 DEFAULT unique_rowid() NOT NULL,
	asset_id text(2147483647) NOT NULL,
	"name" text(2147483647) NOT NULL,
	description text(2147483647),
	asset_type text(2147483647) NOT NULL,
	project_id text(2147483647) NOT NULL,
	frame_type text(2147483647),
	data_object_mapping_key int8,
	note text(2147483647),
	analytic_output_mapping_key int8,
	frame_id text(2147483647),
	data_management_catalog_connection_id text(2147483647),
	base_dimension_mapping_key int8,
	fact_table_type text(2147483647),
	base_fact_mapping_key int8,
	view_frame_logic_key int8,
	mart_database_name text(2147483647),
	publication_name text(2147483647),
	"version" int8 DEFAULT 0:::INT,
	created_at timestamp DEFAULT now() NOT NULL,
	updated_at timestamp DEFAULT now() NOT NULL,
	created_by text(2147483647) DEFAULT 'admin':::STRING NOT NULL,
	updated_by text(2147483647) DEFAULT 'admin':::STRING NOT NULL
) ;
