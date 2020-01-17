create table users (
    id serial primary key,
    username varchar(16) not null,
    password char(64) not null,
    unique(username)
);

create table purchases (
    id serial primary key,
    buyer varchar(32) not null,
    market varchar(64),
    date_bought date not null,
    product_category varchar(32) not null,
    product_name varchar(128) not null,
    price numeric(8, 2) not null
);

