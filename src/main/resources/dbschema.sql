DROP TABLE commit_files;
DROP TABLE commits;

CREATE TABLE commits
(
  id serial PRIMARY KEY,
  commitId character varying(255),
  author character varying(255),
  reviewer character varying(255),
  parent character varying(1024),
  message character varying(4096),
  authTime timestamp,
  mergeTime timestamp,
  authEmail character varying(255),
  mergeEmail character varying(255),
  tag character varying(255),
  ticketNum integer,
  project character varying(255)
)
WITH (
  OIDS=FALSE
);


CREATE TABLE commit_files
(
  id serial PRIMARY KEY,
  cid integer REFERENCES commits (id),
  fileName character varying(255),
  changeType character varying(255)
)
WITH (
  OIDS=FALSE
);