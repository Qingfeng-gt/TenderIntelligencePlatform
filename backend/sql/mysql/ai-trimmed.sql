-- ============================================================
-- AI 模块精简版表结构(与 tender-module-ai 保留功能一致)
-- 保留功能: 模型管理 / API Key / 聊天(会话/消息/角色) / 知识库RAG / AI 写作 / 工具
-- 已裁剪功能: 绘图(image) / 音乐(music) / 思维导图(mindmap) / 工作流(workflow)
-- 适用于 MySQL 8,库: tender
-- 幂等: 重复执行安全(CREATE TABLE IF NOT EXISTS)
-- 字段分析对照实体: tender-module-ai/src/main/java/.../dal/dataobject
-- ============================================================

SET NAMES utf8mb4;

-- 1) AI API 密钥表
CREATE TABLE IF NOT EXISTS `ai_api_key` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`        varchar(100) NOT NULL DEFAULT '' COMMENT '名称',
  `api_key`     varchar(255) NOT NULL DEFAULT '' COMMENT '密钥',
  `platform`    varchar(32)  NOT NULL DEFAULT '' COMMENT '平台:OpenAI/Anthropic/DeepSeek/通义千问/…(AiPlatformEnum)',
  `url`         varchar(512) NOT NULL DEFAULT '' COMMENT 'API 地址',
  `status`      int          NOT NULL DEFAULT 0 COMMENT '状态:0 开启/1 禁用(CommonStatusEnum)',
  `creator`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`   bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = 'AI API 密钥表';

-- 2) AI 模型表
CREATE TABLE IF NOT EXISTS `ai_model` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `key_id`       bigint       NOT NULL DEFAULT 0 COMMENT 'API 密钥编号:ai_api_key.id',
  `name`         varchar(100) NOT NULL DEFAULT '' COMMENT '模型名称',
  `model`        varchar(100) NOT NULL DEFAULT '' COMMENT '模型标志',
  `platform`     varchar(32)  NOT NULL DEFAULT '' COMMENT '平台(AiPlatformEnum)',
  `type`         int          NOT NULL DEFAULT 1 COMMENT '类型:1 对话/2 图片/5 向量/6 重排序等(AiModelTypeEnum)',
  `sort`         int          NOT NULL DEFAULT 0 COMMENT '排序值',
  `status`       int          NOT NULL DEFAULT 0 COMMENT '状态',
  `temperature`  double       NULL DEFAULT NULL COMMENT '温度参数',
  `max_tokens`   int          NULL DEFAULT NULL COMMENT '单条回复最大 Token 数',
  `max_contexts` int          NULL DEFAULT NULL COMMENT '上下文最大 Message 数量',
  `creator`      varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`      varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`    bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type_status` (`tenant_id`, `type`, `status`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 模型表';

-- 3) AI 聊天角色表
CREATE TABLE IF NOT EXISTS `ai_chat_role` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`            varchar(100) NOT NULL DEFAULT '' COMMENT '角色名称',
  `avatar`          varchar(255) NOT NULL DEFAULT '' COMMENT '角色头像',
  `category`        varchar(32)  NOT NULL DEFAULT '' COMMENT '角色分类',
  `description`     varchar(500) NOT NULL DEFAULT '' COMMENT '角色描述',
  `system_message`  text         NULL COMMENT '角色设定(SysPrompt)',
  `user_id`         bigint       NOT NULL DEFAULT 0 COMMENT '用户编号:system_users.id',
  `model_id`        bigint       NULL DEFAULT NULL COMMENT '模型编号:ai_model.id',
  `knowledge_ids`   json         NULL COMMENT '引用的知识库编号列表(AiKnowledgeDO.id)',
  `tool_ids`        json         NULL COMMENT '引用的工具编号列表(AiToolDO.id)',
  `mcp_client_names` json        NULL COMMENT '引用的 MCP Client 名字列表',
  `public_status`   bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否公开:1 公开(管理员创建)/0 私有',
  `sort`            int          NOT NULL DEFAULT 0 COMMENT '排序值',
  `status`          int          NOT NULL DEFAULT 0 COMMENT '状态',
  `creator`         varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`       bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 聊天角色表';

-- 4) AI 对话会话表
CREATE TABLE IF NOT EXISTS `ai_chat_conversation` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`        bigint       NOT NULL DEFAULT 0 COMMENT '用户编号',
  `title`          varchar(100) NOT NULL DEFAULT '新对话' COMMENT '对话标题',
  `pinned`         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否置顶',
  `pinned_time`    datetime     NULL DEFAULT NULL COMMENT '置顶时间',
  `role_id`        bigint       NULL DEFAULT NULL COMMENT '角色编号:ai_chat_role.id',
  `model_id`       bigint       NULL DEFAULT NULL COMMENT '模型编号:ai_model.id',
  `model`          varchar(100) NOT NULL DEFAULT '' COMMENT '模型标志(冗余)',
  `system_message` text         NULL COMMENT '角色设定',
  `temperature`    double       NULL DEFAULT NULL COMMENT '温度参数',
  `max_tokens`     int          NULL DEFAULT NULL COMMENT '单条回复最大 Token 数',
  `max_contexts`   int          NULL DEFAULT NULL COMMENT '上下文最大 Message 数量',
  `creator`        varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`      bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 对话会话表';

-- 5) AI 对话消息表
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `conversation_id`   bigint       NOT NULL DEFAULT 0 COMMENT '对话编号:ai_chat_conversation.id',
  `reply_id`          bigint       NULL DEFAULT NULL COMMENT '回复消息编号(问答关联)',
  `type`              varchar(32)  NOT NULL DEFAULT '' COMMENT '消息类型:user/assistant/system/tool(MessageType)',
  `user_id`           bigint       NOT NULL DEFAULT 0 COMMENT '用户编号',
  `role_id`           bigint       NULL DEFAULT NULL COMMENT '角色编号:ai_chat_role.id',
  `model`             varchar(100) NOT NULL DEFAULT '' COMMENT '模型标志(冗余)',
  `model_id`          bigint       NULL DEFAULT NULL COMMENT '模型编号:ai_model.id',
  `content`           text         NULL COMMENT '聊天内容',
  `reasoning_content` text         NULL COMMENT '推理内容',
  `use_context`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否携带上下文',
  `segment_ids`       json         NULL COMMENT '知识库段落编号数组(AiKnowledgeSegmentDO.id)',
  `web_search_pages`  json         NULL COMMENT '联网搜索的网页内容数组',
  `attachment_urls`   json         NULL COMMENT '附件 URL 数组',
  `creator`           varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`         bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 对话消息表';

-- 6) 知识库表
CREATE TABLE IF NOT EXISTS `ai_knowledge` (
  `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name`                 varchar(100) NOT NULL DEFAULT '' COMMENT '知识库名称',
  `description`          varchar(500) NOT NULL DEFAULT '' COMMENT '知识库描述',
  `embedding_model_id`   bigint       NOT NULL DEFAULT 0 COMMENT '向量模型编号:ai_model.id',
  `embedding_model`      varchar(100) NOT NULL DEFAULT '' COMMENT '模型标识(冗余)',
  `top_k`                int          NOT NULL DEFAULT 4 COMMENT 'topK',
  `similarity_threshold` double       NULL DEFAULT NULL COMMENT '相似度阈值',
  `status`               int          NOT NULL DEFAULT 0 COMMENT '状态',
  `creator`              varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`              varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`              bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`            bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 知识库表';

-- 7) 知识库-文档表
CREATE TABLE IF NOT EXISTS `ai_knowledge_document` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `knowledge_id`      bigint       NOT NULL DEFAULT 0 COMMENT '知识库编号:ai_knowledge.id',
  `name`              varchar(255) NOT NULL DEFAULT '' COMMENT '文档名称',
  `url`               varchar(512) NOT NULL DEFAULT '' COMMENT '文件 URL',
  `content`           longtext     NULL COMMENT '内容',
  `content_length`    int          NOT NULL DEFAULT 0 COMMENT '文档长度',
  `tokens`            int          NOT NULL DEFAULT 0 COMMENT '文档 token 数量',
  `segment_max_tokens` int         NOT NULL DEFAULT 500 COMMENT '分片最大 Token 数',
  `retrieval_count`   int          NOT NULL DEFAULT 0 COMMENT '召回次数',
  `status`            int          NOT NULL DEFAULT 0 COMMENT '状态',
  `creator`           varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`         bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_id` (`knowledge_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 知识库-文档表';

-- 8) 知识库-文档分段表
CREATE TABLE IF NOT EXISTS `ai_knowledge_segment` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `knowledge_id`    bigint       NOT NULL DEFAULT 0 COMMENT '知识库编号:ai_knowledge.id',
  `document_id`     bigint       NOT NULL DEFAULT 0 COMMENT '文档编号:ai_knowledge_document.id',
  `content`         text         NULL COMMENT '切片内容',
  `content_length`  int          NOT NULL DEFAULT 0 COMMENT '切片内容长度',
  `vector_id`       varchar(255) NOT NULL DEFAULT '' COMMENT '向量库的编号',
  `tokens`          int          NOT NULL DEFAULT 0 COMMENT 'token 数量',
  `retrieval_count` int          NOT NULL DEFAULT 0 COMMENT '召回次数',
  `status`          int          NOT NULL DEFAULT 0 COMMENT '状态(0 成功/1 处理中)',
  `creator`         varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`       bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_id` (`knowledge_id`),
  KEY `idx_document_id` (`document_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 知识库-文档分段表';

-- 9) AI 写作表(写标书入口)
CREATE TABLE IF NOT EXISTS `ai_write` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `user_id`           bigint       NOT NULL DEFAULT 0 COMMENT '用户编号',
  `type`              int          NOT NULL DEFAULT 1 COMMENT '写作类型:1 长文生成/2 章节续写/3 专业排版(AiWriteTypeEnum)',
  `platform`          varchar(32)  NOT NULL DEFAULT '' COMMENT '平台(AiPlatformEnum)',
  `model_id`          bigint       NULL DEFAULT NULL COMMENT '模型编号:ai_model.id',
  `model`             varchar(100) NOT NULL DEFAULT '' COMMENT '模型',
  `prompt`            text         NULL COMMENT '生成内容提示',
  `generated_content` longtext     NULL COMMENT '生成的内容',
  `original_content`  longtext     NULL COMMENT '原文',
  `length`            int          NULL DEFAULT NULL COMMENT '长度提示词(字典 ai_write_length)',
  `format`            int          NULL DEFAULT NULL COMMENT '格式提示词(字典 ai_write_format)',
  `tone`              int          NULL DEFAULT NULL COMMENT '语气提示词(字典 ai_write_tone)',
  `language`          int          NULL DEFAULT NULL COMMENT '语言提示词(字典 ai_write_language)',
  `error_message`     varchar(500) NOT NULL DEFAULT '' COMMENT '错误信息',
  `creator`           varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`         bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 写作表';

-- 10) AI 工具表(模型工具调用:目录查询/天气查询/待扩展)
CREATE TABLE IF NOT EXISTS `ai_tool` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '工具编号',
  `name`        varchar(100) NOT NULL DEFAULT '' COMMENT '工具名称(对应 Bean 名)',
  `description` varchar(500) NOT NULL DEFAULT '' COMMENT '工具描述',
  `status`      int          NOT NULL DEFAULT 0 COMMENT '状态',
  `creator`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`   bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB AUTO_INCREMENT = 100 DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 工具表';
