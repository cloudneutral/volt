create table account
(
    id             int            not null default unordered_unique_rowid(),
    type           varchar(32)    not null,
    version        int            not null default 0,
    balance        numeric(19, 2) not null,
    name           varchar(128)   not null,
    allow_negative integer        not null default 0,

    primary key (id, type)
);
