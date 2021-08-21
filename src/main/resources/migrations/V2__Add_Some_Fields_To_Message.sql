alter table messages add column if not exists workspaceId varchar(255);
alter table messages add column if not exists conversationId varchar(255);
alter table messages add column if not exists threadRootId varchar(255);
