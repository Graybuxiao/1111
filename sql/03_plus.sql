ALTER TABLE `flw_task` ADD COLUMN `task_node_id` VARCHAR(64) NULL COMMENT '节点id' AFTER `finish_time`;
ALTER TABLE `flw_his_task` ADD COLUMN `task_node_id` VARCHAR(64) NULL COMMENT '节点id' AFTER `task_state`;
ALTER TABLE `process_templates` ADD COLUMN `flow_long_id` BIGINT NULL COMMENT 'flowlongid' AFTER `updated`;

