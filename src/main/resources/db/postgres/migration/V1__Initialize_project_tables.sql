create table favorite
(
    id                bigserial not null,
    created_date      timestamp,
    modified_date     timestamp,
    member_id         bigint,
    source_station_id bigint,
    target_station_id bigint,
    primary key (id)
);

create table line
(
    id            bigserial not null,
    created_date  timestamp,
    modified_date timestamp,
    color         varchar(255),
    name          varchar(255),
    primary key (id)
);

create table member
(
    id            bigserial not null,
    created_date  timestamp,
    modified_date timestamp,
    age           integer,
    email         varchar(255),
    password      varchar(255),
    primary key (id)
);

create table section
(
    id              bigserial not null,
    distance        integer not null,
    down_station_id bigint,
    line_id         bigint,
    up_station_id   bigint,
    primary key (id)
);

create table station
(
    id            bigserial not null,
    created_date  timestamp,
    modified_date timestamp,
    name          varchar(255),
    primary key (id)
);

alter table line
    add constraint uk_line_name unique (name);

alter table station
    add constraint uk_station_name unique (name);

alter table section
    add constraint fk_section_down_station_id
        foreign key (down_station_id)
            references station (id);

alter table section
    add constraint fk_section_line_id
        foreign key (line_id)
            references line (id);

alter table section
    add constraint fk_section_up_station_id
        foreign key (up_station_id)
            references station (id);
