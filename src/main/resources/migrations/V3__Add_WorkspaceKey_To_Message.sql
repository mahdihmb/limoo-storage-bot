alter table messages drop column workspaceId;
alter table messages add column if not exists workspaceKey varchar(255);
