--
-- postgresql
--
--drop sequence if exists HIBERNATE_SEQUENCES;
--create sequence HIBERNATE_SEQUENCES start with 1024 increment by 1;

drop table big_history;
create table big_history (
id			serial not null,

field_start	double precision not null DEFAULT 0,
field_end	double precision not null DEFAULT 0,
title		varchar(1024) not null DEFAULT '',
description	varchar(1048576) not null DEFAULT '',
category	varchar(1024) not null DEFAULT '',

created		timestamp not null DEFAULT CURRENT_TIMESTAMP,
updated		timestamp not null DEFAULT CURRENT_TIMESTAMP,
primary key (id));
alter SEQUENCE big_history_id_seq restart WITH 1024;
