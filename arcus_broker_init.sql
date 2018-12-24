create database arcus;
use arcus;

CREATE TABLE IF NOT EXISTS service_instances (
    guid      VARCHAR(50) NOT NULL PRIMARY KEY,
    plan_id   VARCHAR(50) NOT NULL,
    zk_host  VARCHAR(50) NOT NULL,
    service_code   VARCHAR(20) NOT NULL UNIQUE,
    INDEX index_svc_name(service_code desc)
);

CREATE TABLE IF NOT EXISTS service_instance_bindings (
    guid      VARCHAR(50) NOT NULL PRIMARY KEY,
    service_instance_id   VARCHAR(50) NOT NULL,
    cluster_user_name VARCHAR(20) NOT NULL UNIQUE,
    INDEX index_cluster_user_name(cluster_user_name desc)
);

CREATE TABLE IF NOT EXISTS usable_port_info (
	port_num INT NOT NULL PRIMARY KEY,
	vm_ip VARCHAR(20) NOT NULL,
    is_used BOOL NOT NULL,
    INDEX index_usage(is_used asc)
);