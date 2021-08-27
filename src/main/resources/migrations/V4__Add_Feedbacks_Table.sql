drop table if exists feedbacks cascade;
create table feedbacks
(
    id                  bigint       not null,
    userId              varchar(255) not null,
    userWorkspaceId     varchar(255) not null,
    userConversationId  varchar(255) not null,
    userThreadRootId    varchar(255) not null,
    adminWorkspaceId    varchar(255) not null,
    adminConversationId varchar(255) not null,
    adminThreadRootId   varchar(255) not null,
    primary key (id)
);
