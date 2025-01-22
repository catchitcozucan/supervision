--
--  To create the database, schema and stuff, check out
--  create_db.sql - something like this needs to be
--  executed. Then flyway will manage the table setup.
--  However, if you want to 'clean up all' and reset,
--  it would be something like below
--
delete from supervision.source_response where id >0;
delete from supervision.detail_header where detail_id > 0;
delete from supervision.source_header where id > 0;
delete from supervision.source_detail  where id >0;
delete from supervision."source" where id > 0;
delete from supervision.flyway_schema_history where installed_rank > 1;
delete from supervision.login where id > 0;

-- or/and what a flyway:clean would do

drop table if exists supervision.login cascade;
drop table if exists supervision.source_response cascade;
drop table if exists supervision.detail_header cascade;
drop table if exists supervision.source_header  cascade;
drop table if exists supervision.source_detail  cascade;
drop table if exists supervision.source cascade;
delete from supervision.flyway_schema_history where installed_rank > 0;