/*
 Setting up the supervision tables
 */
DROP SEQUENCE if exists supervision.source_seq;
DROP SEQUENCE if exists supervision.source_detail_seq;
DROP SEQUENCE if exists supervision.source_header_seq;
DROP SEQUENCE if exists supervision.source_response_seq;

DROP TABLE if exists supervision.detail_header;
DROP TABLE if exists supervision.source_header;
DROP TABLE if exists supervision.source_detail;
DROP TABLE if exists supervision.source_response;
DROP TABLE if exists supervision.source;

CREATE SEQUENCE supervision.source_seq START 1;
CREATE SEQUENCE supervision.source_detail_seq START 1;
CREATE SEQUENCE supervision.source_header_seq START 1;
CREATE SEQUENCE supervision.source_response_seq START 1;

CREATE TABLE supervision.source (
                                    id SERIAL PRIMARY KEY,
                                    version int4 NOT NULL default 0,
                                    domain varchar(100) NOT NULL,
                                    department varchar(100) NOT NULL,
                                    process_name varchar(300) NOT NULL,
                                    access_url varchar(255) NOT NULL UNIQUE,
                                    access_key UUID NOT NULL UNIQUE,
                                    touched TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    disabled bool NOT NULL DEFAULT false,
                                    UNIQUE (department, domain, process_name)
);

CREATE TABLE supervision.source_detail (
                                           id SERIAL PRIMARY KEY,
                                           source_id int4 not null,
                                           version int4 NOT NULL default 0,
                                           proxy_host varchar(70) NULL,
                                           proxy_port int,
                                           basic_auth_user varchar(100) NULL,
                                           basic_auth_pwd varchar(100) NULL,
                                           CONSTRAINT fk_source_detail
                                               FOREIGN KEY(source_id)
                                                   REFERENCES supervision.source(id)
);

CREATE TABLE supervision.source_header (
                                           id SERIAL PRIMARY KEY,
                                           version int4 NOT NULL default 0,
                                           name varchar(50) NULL,
                                           value varchar(200) NULL
);

CREATE TABLE supervision.detail_header
(
    detail_id int4 NOT NULL,
    header_id int4 NOT NULL,
    CONSTRAINT fk_detail_header_source_header
        FOREIGN KEY (header_id)
            REFERENCES supervision.source_header (id),
    CONSTRAINT fk_detail_header_source_detail
        FOREIGN KEY (detail_id)
            REFERENCES supervision.source_detail (id),
    UNIQUE (detail_id, header_id)
);

CREATE TABLE supervision.source_response (
                                           id SERIAL PRIMARY KEY,
                                           version int4 NOT NULL default 0,
                                           last_succ TIMESTAMP NOT NULL,
                                           touched TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           last_exec_time varchar(255) NOT NULL,
                                           access_key UUID NOT NULL UNIQUE,
                                           state varchar(255) NULL,
                                           histogram text NULL,
                                           CONSTRAINT fk_source_access_key_source_response
                                               FOREIGN KEY(access_key)
                                                   REFERENCES supervision.source(access_key)
);
