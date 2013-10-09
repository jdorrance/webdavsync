create table file
(
  filename	character varying(2048) not null,
  last_modified	bigint not null,
  local		character varying(2048) not null,
  remote	character varying(2048) not null,
  primary key (local, remote, filename)
);

create table remote_path
(
  etag		character varying(2048) not null,
  last_modified	bigint not null,
  local		character varying(2048) not null,
  path		character varying(2048) not null,
  remote	character varying(2048) not null,
  primary key (local, remote, path)
);

create table synchronization
(
  local		character varying(2048) not null,
  occurred	bigint not null,
  remote	character varying(2048) not null,
  primary key (local, remote)
);
