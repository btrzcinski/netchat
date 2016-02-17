CREATE DATABASE IF NOT EXISTS netchat;

GRANT ALL ON netchat.* to 'netchat'@'localhost' IDENTIFIED BY 'tahcten';

USE netchat;

DROP TABLE chat_backlog;

CREATE TABLE chat_backlog (
    id int auto_increment primary key,
    sent datetime,
    source varchar(255),
    destination varchar(255),
    message blob
);

DROP TABLE chat_blocks;

CREATE TABLE chat_blocks (
    id int auto_increment primary key,
    username varchar(255),
    block varchar(255)
);

DROP TABLE chat_friends;

CREATE TABLE chat_friends (
    id int auto_increment primary key,
    username varchar(255),
    buddy varchar(255)
);

DROP TABLE login_users;

CREATE TABLE login_users (
    id int auto_increment primary key,
    username varchar(255),
    password varchar(255)
);


