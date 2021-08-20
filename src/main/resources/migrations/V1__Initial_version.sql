drop table if exists messages cascade;
create table messages
(
    id   varchar(255) not null,
    text text,
    primary key (id)
);

drop table if exists file_infos cascade;
create table file_infos
(
    hash varchar(255) not null,
    name varchar(255),
    size bigint,
    primary key (hash)
);

drop table if exists message_file_infos cascade;
create table message_file_infos
(
    messageId varchar(255) not null,
    fileHash  varchar(255) not null,
    primary key (messageId, fileHash),
    foreign key (messageId) references messages (id),
    foreign key (fileHash) references file_infos (hash)
);

drop table if exists users cascade;
create table users
(
    id varchar(255) not null,
    primary key (id)
);

drop table if exists user_messages cascade;
create table user_messages
(
    id        bigint       not null,
    name      varchar(255) not null,
    userId    varchar(255) not null,
    messageId varchar(255) not null,
    primary key (id),
    unique (name, userId),
    foreign key (userId) references users (id),
    foreign key (messageId) references messages (id)
);

drop table if exists workspaces cascade;
create table workspaces
(
    id varchar(255) not null,
    primary key (id)
);

drop table if exists workspace_messages cascade;
create table workspace_messages
(
    id          bigint       not null,
    name        varchar(255) not null,
    workspaceId varchar(255) not null,
    messageId   varchar(255) not null,
    primary key (id),
    unique (name, workspaceId),
    foreign key (workspaceId) references workspaces (id),
    foreign key (messageId) references messages (id)
);
