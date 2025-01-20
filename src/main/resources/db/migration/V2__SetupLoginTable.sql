/*
 Setting up the Login table
 */

DROP SEQUENCE if exists supervision.login_seq;
DROP TABLE if exists supervision.login;
CREATE SEQUENCE supervision.login_seq START 1;

CREATE TABLE supervision.login (
                                   id SERIAL PRIMARY KEY,
                                   version int4 NOT NULL default 0,
                                   pwd_hash varchar(128) NOT NULL,
                                   username varchar(100) NOT NULL UNIQUE
);
insert into supervision.login(username, pwd_hash) values ('admin', '$argon2id$v=19$m=60000,t=10,p=1$QXwkO6sLPqnnHyEh4SCB6Q$W/FYb9KpX86+8y4HnsBSzwtaZX71YDnCEVg0pac7Ia8');
