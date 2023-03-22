create table `EL_TABLE`
(
    `id`               integer NOT NULL,
    `application_name` text    NOT NULL,
    `chain_name`       text    NOT NULL,
    `el_data`          text    NOT NULL,
    PRIMARY KEY (`id`)
);

create table `script_node_table`
(
    `id`               integer NOT NULL,
    `application_name` text    NOT NULL,
    `script_node_id`   text    NOT NULL,
    `script_node_name` text    NOT NULL,
    `script_node_type` text    NOT NULL,
    `script_node_data` text    NOT NULL,
    `script_language`     varchar(1024) NOT NULL,
    PRIMARY KEY (`id`)
);