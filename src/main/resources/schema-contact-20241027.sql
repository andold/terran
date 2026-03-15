drop sequence if exists HIBERNATE_SEQUENCES;
create sequence HIBERNATE_SEQUENCES start with 1024 increment by 1;

// 연락처
drop table contact if exists;
drop table contact_content if exists;
drop table contact_param if exists;
drop table contact_value if exists;
drop table contact_map if exists;

drop table vcard if exists;
create table vcard (
id			integer AUTO_INCREMENT(1024, 1) not null,
fn			varchar(4096) not null DEFAULT '',
field_value	integer not null DEFAULT 0,
content		varchar(1048576) not null DEFAULT '',

created timestamp not null DEFAULT CURRENT_TIMESTAMP,
updated timestamp not null DEFAULT CURRENT_TIMESTAMP,
primary key (id));

drop table vcard_map if exists;
create table vcard_map (
id			integer AUTO_INCREMENT(1024, 1) not null,
vcard_id	integer DEFAULT 0,
field_key	varchar(4096) not null DEFAULT '',
field_value	integer not null DEFAULT 0,

created timestamp not null DEFAULT CURRENT_TIMESTAMP,
updated timestamp not null DEFAULT CURRENT_TIMESTAMP,
primary key (id));

//	alter table vcard add fn varchar(4096) not null DEFAULT '';
//	alter table vcard add field_value integer not null DEFAULT 0;

--
-- postgresql
--
drop sequence if exists HIBERNATE_SEQUENCES;
create sequence HIBERNATE_SEQUENCES start with 1024 increment by 1;

-- 연락처
/*
drop table contact;
drop table contact_content;
drop table contact_param;
drop table contact_value;
drop table contact_map;
drop table vcard;
drop table vcard_map;
*/

create table vcard (
id			serial not null,
fn			varchar(4096) not null DEFAULT '',
field_value	integer not null DEFAULT 0,
content		varchar(1048576) not null DEFAULT '',

created timestamp not null DEFAULT CURRENT_TIMESTAMP,
updated timestamp not null DEFAULT CURRENT_TIMESTAMP,
primary key (id));
alter sequence vcard_id_seq restart with 1024;

create table vcard_map (
id			serial not null,
vcard_id	integer DEFAULT 0,
field_key	varchar(4096) not null DEFAULT '',
field_value	integer not null DEFAULT 0,

created timestamp not null DEFAULT CURRENT_TIMESTAMP,
updated timestamp not null DEFAULT CURRENT_TIMESTAMP,
primary key (id));
alter sequence vcard_map_id_seq restart with 1024;
