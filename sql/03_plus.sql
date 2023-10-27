ALTER TABLE `flw_task` ADD COLUMN `task_node_id` VARCHAR(64) NULL COMMENT '节点id' AFTER `finish_time`;
ALTER TABLE `flw_his_task` ADD COLUMN `task_node_id` VARCHAR(64) NULL COMMENT '节点id' AFTER `task_state`;
ALTER TABLE `process_templates` ADD COLUMN `flow_long_id` BIGINT NULL COMMENT 'flowlongid' AFTER `updated`;

CREATE TABLE process_attachments(
                                    id VARCHAR (64) NOT NULL COMMENT '',
                                    user_id VARCHAR (64) COMMENT 'userId',
                                    file_name VARCHAR (999) COMMENT 'fileName',
                                    file_desc VARCHAR (999) COMMENT 'fileDesc',
                                    types VARCHAR (50) COMMENT 'types',
                                    create_time DATETIME COMMENT 'createTime',
                                    task_id BIGINT (15) COMMENT 'taskId',
                                    instance_id BIGINT (15) COMMENT 'instanceId',
                                    url VARCHAR (50) COMMENT 'url',
                                    PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'process_attachments';
CREATE TABLE process_comments(
                                 id VARCHAR (64) NOT NULL COMMENT '',
                                 types VARCHAR (50) COMMENT 'types',
                                 create_time DATETIME COMMENT 'createTime',
                                 user_id VARCHAR (64) COMMENT 'userId',
                                 task_id BIGINT (15) COMMENT 'taskId',
                                 instance_id BIGINT (15) COMMENT 'instanceId',
                                 message LONGTEXT COMMENT 'message',
                                 PRIMARY KEY (id)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'process_comments';
