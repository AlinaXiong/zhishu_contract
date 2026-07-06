-- 创建数据库
CREATE DATABASE IF NOT EXISTS `hero_middleware` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `hero_middleware`;

-- 合同表
CREATE TABLE IF NOT EXISTS `t_contract` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_id` VARCHAR(64) NOT NULL COMMENT '合同ID',
  `contract_name` VARCHAR(255) NOT NULL COMMENT '合同名称',
  `contract_type` VARCHAR(64) DEFAULT NULL COMMENT '合同类型',
  `contract_status` VARCHAR(32) DEFAULT NULL COMMENT '合同状态',
  `source_type` VARCHAR(32) DEFAULT NULL COMMENT '来源单据类型:PURCHASE_APPLY-采购申请,ORDER_INFO-订单信息,ANCHOR_CARD-主播卡片',
  `source_id` VARCHAR(64) DEFAULT NULL COMMENT '来源单据ID',
  `source_no` VARCHAR(64) DEFAULT NULL COMMENT '来源单据编号',
  `party_a` VARCHAR(255) DEFAULT NULL COMMENT '甲方编码',
  `party_a_name` VARCHAR(255) DEFAULT NULL COMMENT '甲方名称',
  `party_b` VARCHAR(255) DEFAULT NULL COMMENT '乙方编码',
  `party_b_name` VARCHAR(255) DEFAULT NULL COMMENT '乙方名称',
  `contract_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '合同金额',
  `currency` VARCHAR(16) DEFAULT 'CNY' COMMENT '币种',
  `start_date` VARCHAR(32) DEFAULT NULL COMMENT '开始日期',
  `end_date` VARCHAR(32) DEFAULT NULL COMMENT '结束日期',
  `sign_date` VARCHAR(32) DEFAULT NULL COMMENT '签订日期',
  `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '经办人ID',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '经办人姓名',
  `dept_id` VARCHAR(64) DEFAULT NULL COMMENT '部门ID',
  `dept_name` VARCHAR(128) DEFAULT NULL COMMENT '部门名称',
  `zhishu_contract_id` VARCHAR(64) DEFAULT NULL COMMENT '智书合同ID',
  `yuecai_contract_id` VARCHAR(64) DEFAULT NULL COMMENT '业财合同ID',
  `draft_url` VARCHAR(512) DEFAULT NULL COMMENT '草稿页链接',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `form_data` TEXT COMMENT '表单数据JSON',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_id` (`contract_id`),
  KEY `idx_zhishu_contract_id` (`zhishu_contract_id`),
  KEY `idx_yuecai_contract_id` (`yuecai_contract_id`),
  KEY `idx_contract_status` (`contract_status`),
  KEY `idx_source_type` (`source_type`),
  KEY `idx_source_id` (`source_id`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同表';

-- 合同同步日志表
CREATE TABLE IF NOT EXISTS `t_contract_sync_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_id` VARCHAR(64) NOT NULL COMMENT '合同ID',
  `sync_type` VARCHAR(32) NOT NULL COMMENT '同步类型:CREATE-创建,UPDATE-更新,SYNC-同步',
  `sync_direction` VARCHAR(32) NOT NULL COMMENT '同步方向:YUECAI_TO_ZHISHU,ZHISHU_TO_YUECAI',
  `sync_status` VARCHAR(32) NOT NULL COMMENT '同步状态:SUCCESS-成功,FAIL-失败',
  `request_param` TEXT COMMENT '请求参数JSON',
  `response_data` TEXT COMMENT '响应数据JSON',
  `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_sync_type` (`sync_type`),
  KEY `idx_sync_status` (`sync_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同同步日志表';

-- 审批记录表
CREATE TABLE IF NOT EXISTS `t_approval_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `approval_id` VARCHAR(64) NOT NULL COMMENT '审批ID',
  `contract_id` VARCHAR(64) NOT NULL COMMENT '合同ID',
  `approver_id` VARCHAR(64) DEFAULT NULL COMMENT '审批人ID',
  `approver_name` VARCHAR(64) DEFAULT NULL COMMENT '审批人姓名',
  `approval_status` VARCHAR(32) DEFAULT NULL COMMENT '审批状态',
  `approval_opinion` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `approval_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
--   UNIQUE KEY `uk_approval_id` (`approval_id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_approver_id` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- 付款记录表
CREATE TABLE IF NOT EXISTS `t_payment_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `payment_id` VARCHAR(64) NOT NULL COMMENT '付款ID',
  `contract_id` VARCHAR(64) NOT NULL COMMENT '合同ID',
  `zhishu_payment_id` VARCHAR(64) DEFAULT NULL COMMENT '智书付款ID',
  `yuecai_payment_id` VARCHAR(64) DEFAULT NULL COMMENT '业财付款ID',
  `payment_type` VARCHAR(32) DEFAULT NULL COMMENT '付款类型',
  `amount` DECIMAL(18,2) DEFAULT NULL COMMENT '付款金额',
  `currency` VARCHAR(16) DEFAULT NULL COMMENT '币种',
  `payment_method` VARCHAR(32) DEFAULT NULL COMMENT '付款方式',
  `payment_status` VARCHAR(32) DEFAULT NULL COMMENT '付款状态',
  `payment_time` VARCHAR(32) DEFAULT NULL COMMENT '付款时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_id` (`payment_id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_yuecai_payment_id` (`yuecai_payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='付款记录表';

-- 收款记录表
CREATE TABLE IF NOT EXISTS `t_receipt_record` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `receipt_id` VARCHAR(64) NOT NULL COMMENT '收款ID',
  `contract_id` VARCHAR(64) NOT NULL COMMENT '合同ID',
  `zhishu_receipt_id` VARCHAR(64) DEFAULT NULL COMMENT '智书收款ID',
  `yuecai_receipt_id` VARCHAR(64) DEFAULT NULL COMMENT '业财收款ID',
  `receipt_type` VARCHAR(32) DEFAULT NULL COMMENT '收款类型',
  `amount` DECIMAL(18,2) DEFAULT NULL COMMENT '收款金额',
  `currency` VARCHAR(16) DEFAULT NULL COMMENT '币种',
  `receipt_method` VARCHAR(32) DEFAULT NULL COMMENT '收款方式',
  `receipt_status` VARCHAR(32) DEFAULT NULL COMMENT '收款状态',
  `receipt_time` VARCHAR(32) DEFAULT NULL COMMENT '收款时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receipt_id` (`receipt_id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_yuecai_receipt_id` (`yuecai_receipt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款记录表';

-- API日志表
CREATE TABLE IF NOT EXISTS `t_api_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '追踪ID',
  `api_name` VARCHAR(128) NOT NULL COMMENT '接口名称',
  `api_description` VARCHAR(255) DEFAULT NULL COMMENT '接口描述',
  `request_method` VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
  `request_url` VARCHAR(512) DEFAULT NULL COMMENT '请求URL',
  `request_params` TEXT COMMENT '请求参数JSON',
  `response_params` TEXT COMMENT '响应参数JSON',
  `request_ip` VARCHAR(64) DEFAULT NULL COMMENT '请求IP',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '用户代理',
  `execute_time` BIGINT(20) DEFAULT NULL COMMENT '执行时间(毫秒)',
  `http_status` INT(11) DEFAULT NULL COMMENT 'HTTP状态码',
  `status` TINYINT(1) DEFAULT 1 COMMENT '执行状态:1-成功,0-失败',
  `error_message` VARCHAR(2000) DEFAULT NULL COMMENT '错误信息',
  `operator_id` VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(64) DEFAULT NULL COMMENT '操作人姓名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_api_name` (`api_name`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API日志表';
