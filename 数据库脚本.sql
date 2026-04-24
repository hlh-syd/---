CREATE DATABASE IF NOT EXISTS research_workbench
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE research_workbench;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(128) DEFAULT NULL,
  phone VARCHAR(32) DEFAULT NULL,
  nickname VARCHAR(64) NOT NULL,
  avatar_url VARCHAR(255) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1=active,0=disabled',
  role_code VARCHAR(32) NOT NULL DEFAULT 'USER',
  last_login_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_user_username (username),
  UNIQUE KEY uk_sys_user_email (email),
  KEY idx_sys_user_role (role_code)
) ENGINE=InnoDB COMMENT='用户表';

CREATE TABLE IF NOT EXISTS user_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  real_name VARCHAR(64) DEFAULT NULL,
  gender TINYINT DEFAULT NULL COMMENT '0=unknown,1=male,2=female',
  bio VARCHAR(500) DEFAULT NULL,
  institution VARCHAR(128) DEFAULT NULL,
  department VARCHAR(128) DEFAULT NULL,
  research_direction VARCHAR(255) DEFAULT NULL,
  degree_level VARCHAR(32) DEFAULT NULL,
  interests JSON DEFAULT NULL,
  tags JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_profile_user (user_id),
  CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='用户扩展资料';

CREATE TABLE IF NOT EXISTS user_social_binding (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  platform VARCHAR(32) NOT NULL COMMENT 'wechat/feishu/github',
  open_id VARCHAR(128) NOT NULL,
  union_id VARCHAR(128) DEFAULT NULL,
  meta_json JSON DEFAULT NULL,
  bound_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_social_platform_openid (platform, open_id),
  KEY idx_social_user (user_id),
  CONSTRAINT fk_social_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='第三方绑定';

CREATE TABLE IF NOT EXISTS workspace (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(500) DEFAULT NULL,
  cover_url VARCHAR(255) DEFAULT NULL,
  visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE/TEAM/PUBLIC',
  system_prompt TEXT DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_workspace_owner (owner_user_id),
  KEY idx_workspace_visibility (visibility),
  CONSTRAINT fk_workspace_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='项目空间';

CREATE TABLE IF NOT EXISTS workspace_member (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  workspace_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  member_role VARCHAR(16) NOT NULL DEFAULT 'EDITOR' COMMENT 'OWNER/ADMIN/EDITOR/VIEWER',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_workspace_user (workspace_id, user_id),
  KEY idx_workspace_member_user (user_id),
  CONSTRAINT fk_workspace_member_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
  CONSTRAINT fk_workspace_member_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='项目空间成员';

CREATE TABLE IF NOT EXISTS research_project (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  workspace_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  topic VARCHAR(255) DEFAULT NULL,
  summary TEXT DEFAULT NULL,
  color_token VARCHAR(32) DEFAULT NULL,
  stage_code VARCHAR(32) NOT NULL DEFAULT 'IDEA' COMMENT 'IDEA/SEARCH/READING/WRITING/DONE',
  due_date DATE DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_project_workspace (workspace_id),
  KEY idx_project_owner (owner_user_id),
  KEY idx_project_stage (stage_code),
  CONSTRAINT fk_project_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
  CONSTRAINT fk_project_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='研究项目';

CREATE TABLE IF NOT EXISTS research_note (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  note_type VARCHAR(32) NOT NULL DEFAULT 'RICH_TEXT' COMMENT 'RICH_TEXT/MARKDOWN/QUOTE',
  content LONGTEXT NOT NULL,
  source_refs JSON DEFAULT NULL,
  pinned TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_note_project (project_id),
  KEY idx_note_user (user_id),
  CONSTRAINT fk_note_project FOREIGN KEY (project_id) REFERENCES research_project(id),
  CONSTRAINT fk_note_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='研究笔记';

CREATE TABLE IF NOT EXISTS paper_search_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  workspace_id BIGINT DEFAULT NULL,
  project_id BIGINT DEFAULT NULL,
  query_text VARCHAR(500) NOT NULL,
  source_scope VARCHAR(64) NOT NULL DEFAULT 'ACADEMIC',
  filters_json JSON DEFAULT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
  result_count INT NOT NULL DEFAULT 0,
  started_at DATETIME DEFAULT NULL,
  finished_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_paper_search_user (user_id),
  KEY idx_paper_search_workspace (workspace_id),
  KEY idx_paper_search_project (project_id),
  CONSTRAINT fk_paper_search_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_paper_search_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
  CONSTRAINT fk_paper_search_project FOREIGN KEY (project_id) REFERENCES research_project(id)
) ENGINE=InnoDB COMMENT='学术检索任务';

CREATE TABLE IF NOT EXISTS paper_search_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  source_name VARCHAR(64) NOT NULL,
  external_id VARCHAR(128) DEFAULT NULL,
  title VARCHAR(500) NOT NULL,
  authors VARCHAR(500) DEFAULT NULL,
  abstract_text LONGTEXT DEFAULT NULL,
  publish_year INT DEFAULT NULL,
  journal_name VARCHAR(255) DEFAULT NULL,
  doi VARCHAR(128) DEFAULT NULL,
  paper_url VARCHAR(500) DEFAULT NULL,
  pdf_url VARCHAR(500) DEFAULT NULL,
  citation_count INT DEFAULT NULL,
  keyword_json JSON DEFAULT NULL,
  score DECIMAL(10,4) DEFAULT NULL,
  raw_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_paper_result_task (task_id),
  KEY idx_paper_result_year (publish_year),
  KEY idx_paper_result_doi (doi),
  CONSTRAINT fk_paper_result_task FOREIGN KEY (task_id) REFERENCES paper_search_task(id)
) ENGINE=InnoDB COMMENT='学术检索结果';

CREATE TABLE IF NOT EXISTS knowledge_base (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  workspace_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(500) DEFAULT NULL,
  visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
  doc_count INT NOT NULL DEFAULT 0,
  total_chunk_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_kb_workspace (workspace_id),
  KEY idx_kb_owner (owner_user_id),
  CONSTRAINT fk_kb_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
  CONSTRAINT fk_kb_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='知识库';

CREATE TABLE IF NOT EXISTS knowledge_folder (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  kb_id BIGINT NOT NULL,
  parent_id BIGINT DEFAULT NULL,
  name VARCHAR(128) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_kb_folder_kb (kb_id),
  KEY idx_kb_folder_parent (parent_id),
  CONSTRAINT fk_kb_folder_kb FOREIGN KEY (kb_id) REFERENCES knowledge_base(id),
  CONSTRAINT fk_kb_folder_parent FOREIGN KEY (parent_id) REFERENCES knowledge_folder(id)
) ENGINE=InnoDB COMMENT='知识库文件夹';

CREATE TABLE IF NOT EXISTS knowledge_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  kb_id BIGINT NOT NULL,
  folder_id BIGINT DEFAULT NULL,
  source_type VARCHAR(32) NOT NULL DEFAULT 'UPLOAD' COMMENT 'UPLOAD/PAPER/WEB/AI',
  source_ref_id BIGINT DEFAULT NULL,
  title VARCHAR(255) NOT NULL,
  file_name VARCHAR(255) DEFAULT NULL,
  file_ext VARCHAR(16) DEFAULT NULL,
  file_size BIGINT DEFAULT NULL,
  storage_path VARCHAR(500) DEFAULT NULL,
  source_url VARCHAR(500) DEFAULT NULL,
  summary_text TEXT DEFAULT NULL,
  tag_json JSON DEFAULT NULL,
  parse_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  chunk_count INT NOT NULL DEFAULT 0,
  uploaded_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_kb_doc_kb (kb_id),
  KEY idx_kb_doc_folder (folder_id),
  KEY idx_kb_doc_source_type (source_type),
  KEY idx_kb_doc_uploaded_by (uploaded_by),
  CONSTRAINT fk_kb_doc_kb FOREIGN KEY (kb_id) REFERENCES knowledge_base(id),
  CONSTRAINT fk_kb_doc_folder FOREIGN KEY (folder_id) REFERENCES knowledge_folder(id),
  CONSTRAINT fk_kb_doc_user FOREIGN KEY (uploaded_by) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='知识库文档';

CREATE TABLE IF NOT EXISTS knowledge_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  chunk_no INT NOT NULL,
  content_text LONGTEXT NOT NULL,
  token_count INT DEFAULT NULL,
  keyword_json JSON DEFAULT NULL,
  embedding_status VARCHAR(16) NOT NULL DEFAULT 'NONE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_chunk_document_no (document_id, chunk_no),
  KEY idx_chunk_document (document_id),
  CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES knowledge_document(id)
) ENGINE=InnoDB COMMENT='知识库分段';

CREATE TABLE IF NOT EXISTS web_search_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  workspace_id BIGINT DEFAULT NULL,
  project_id BIGINT DEFAULT NULL,
  query_text VARCHAR(500) NOT NULL,
  platform_scope VARCHAR(128) NOT NULL DEFAULT 'webpage',
  filters_json JSON DEFAULT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
  result_count INT NOT NULL DEFAULT 0,
  markdown_summary MEDIUMTEXT DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_web_search_user (user_id),
  KEY idx_web_search_workspace (workspace_id),
  KEY idx_web_search_project (project_id),
  CONSTRAINT fk_web_search_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_web_search_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
  CONSTRAINT fk_web_search_project FOREIGN KEY (project_id) REFERENCES research_project(id)
) ENGINE=InnoDB COMMENT='网页搜索任务';

CREATE TABLE IF NOT EXISTS web_search_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  platform_name VARCHAR(64) NOT NULL,
  title VARCHAR(500) NOT NULL,
  author_name VARCHAR(128) DEFAULT NULL,
  publish_at DATETIME DEFAULT NULL,
  url VARCHAR(500) DEFAULT NULL,
  cover_url VARCHAR(500) DEFAULT NULL,
  snippet_text TEXT DEFAULT NULL,
  markdown_content MEDIUMTEXT DEFAULT NULL,
  score DECIMAL(10,4) DEFAULT NULL,
  raw_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_web_result_task (task_id),
  KEY idx_web_result_platform (platform_name),
  CONSTRAINT fk_web_result_task FOREIGN KEY (task_id) REFERENCES web_search_task(id)
) ENGINE=InnoDB COMMENT='网页搜索结果';

CREATE TABLE IF NOT EXISTS saved_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  workspace_id BIGINT DEFAULT NULL,
  project_id BIGINT DEFAULT NULL,
  material_type VARCHAR(32) NOT NULL COMMENT 'PAPER/WEB/DOCUMENT/NOTE/ARTIFACT',
  source_ref_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  tag_json JSON DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_saved_material_user (user_id),
  KEY idx_saved_material_workspace (workspace_id),
  KEY idx_saved_material_project (project_id),
  KEY idx_saved_material_type (material_type),
  CONSTRAINT fk_saved_material_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_saved_material_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
  CONSTRAINT fk_saved_material_project FOREIGN KEY (project_id) REFERENCES research_project(id)
) ENGINE=InnoDB COMMENT='收藏资料';

CREATE TABLE IF NOT EXISTS ai_tool_directory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  category VARCHAR(64) NOT NULL,
  icon_url VARCHAR(255) DEFAULT NULL,
  official_url VARCHAR(500) DEFAULT NULL,
  description VARCHAR(500) DEFAULT NULL,
  recommend_index INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_ai_tool_category (category),
  KEY idx_ai_tool_status (status)
) ENGINE=InnoDB COMMENT='AI 工具目录';

CREATE TABLE IF NOT EXISTS prompt_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_user_id BIGINT DEFAULT NULL,
  template_name VARCHAR(128) NOT NULL,
  scene_code VARCHAR(64) NOT NULL COMMENT 'PROMPT_OPT/BIO/WEB_SUMMARY/PAPER_BRIEF',
  system_prompt TEXT NOT NULL,
  user_prompt TEXT NOT NULL,
  variables_json JSON DEFAULT NULL,
  is_public TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_prompt_owner (owner_user_id),
  KEY idx_prompt_scene (scene_code),
  CONSTRAINT fk_prompt_owner FOREIGN KEY (owner_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='提示词模板';

CREATE TABLE IF NOT EXISTS ai_chat_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  workspace_id BIGINT DEFAULT NULL,
  project_id BIGINT DEFAULT NULL,
  title VARCHAR(255) NOT NULL,
  assistant_type VARCHAR(32) NOT NULL COMMENT 'GENERAL/BIO/PROMPT/KB/PAPER/WEB',
  source_doc_ids JSON DEFAULT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_chat_session_user (user_id),
  KEY idx_chat_session_workspace (workspace_id),
  KEY idx_chat_session_project (project_id),
  CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_chat_session_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
  CONSTRAINT fk_chat_session_project FOREIGN KEY (project_id) REFERENCES research_project(id)
) ENGINE=InnoDB COMMENT='AI 会话';

CREATE TABLE IF NOT EXISTS ai_chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  role_code VARCHAR(16) NOT NULL COMMENT 'USER/ASSISTANT/SYSTEM',
  content_text LONGTEXT NOT NULL,
  model_name VARCHAR(64) DEFAULT NULL,
  prompt_tokens INT DEFAULT NULL,
  completion_tokens INT DEFAULT NULL,
  source_refs JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_chat_message_session (session_id),
  KEY idx_chat_message_role (role_code),
  CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES ai_chat_session(id)
) ENGINE=InnoDB COMMENT='AI 消息';

CREATE TABLE IF NOT EXISTS search_history_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  user_name VARCHAR(64) NOT NULL,
  biz_type VARCHAR(32) NOT NULL COMMENT 'RESEARCH/WEB/GENERAL_CHAT/KB_CHAT',
  title VARCHAR(255) DEFAULT NULL,
  item_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_search_history_session_id (session_id),
  KEY idx_search_history_session_user (user_id),
  KEY idx_search_history_session_type (biz_type),
  CONSTRAINT fk_search_history_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='统一搜索会话历史';

CREATE TABLE IF NOT EXISTS search_history_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  user_name VARCHAR(64) NOT NULL,
  detail_no INT NOT NULL DEFAULT 1,
  query_text LONGTEXT DEFAULT NULL,
  answer_text LONGTEXT DEFAULT NULL,
  extra_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_search_history_detail_session (session_id),
  KEY idx_search_history_detail_user (user_id),
  CONSTRAINT fk_search_history_detail_session FOREIGN KEY (session_id) REFERENCES search_history_session(session_id),
  CONSTRAINT fk_search_history_detail_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='统一搜索明细历史';

CREATE TABLE IF NOT EXISTS workspace_artifact (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  workspace_id BIGINT NOT NULL,
  project_id BIGINT DEFAULT NULL,
  creator_user_id BIGINT NOT NULL,
  artifact_type VARCHAR(32) NOT NULL COMMENT 'REPORT/MIND_MAP/BRIEFING/FLASHCARD/QUIZ/MARKDOWN',
  title VARCHAR(255) NOT NULL,
  source_type VARCHAR(32) NOT NULL COMMENT 'PAPER/WEB/KB/CHAT/MIXED',
  source_refs JSON DEFAULT NULL,
  content_markdown LONGTEXT DEFAULT NULL,
  extra_json JSON DEFAULT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'READY',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_artifact_workspace (workspace_id),
  KEY idx_artifact_project (project_id),
  KEY idx_artifact_creator (creator_user_id),
  KEY idx_artifact_type (artifact_type),
  CONSTRAINT fk_artifact_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
  CONSTRAINT fk_artifact_project FOREIGN KEY (project_id) REFERENCES research_project(id),
  CONSTRAINT fk_artifact_creator FOREIGN KEY (creator_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='生成产物';

CREATE TABLE IF NOT EXISTS study_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  workspace_id BIGINT DEFAULT NULL,
  project_id BIGINT DEFAULT NULL,
  title VARCHAR(255) NOT NULL,
  description VARCHAR(1000) DEFAULT NULL,
  task_status VARCHAR(16) NOT NULL DEFAULT 'TODO' COMMENT 'TODO/DOING/DONE/CANCELED',
  priority_level TINYINT NOT NULL DEFAULT 2 COMMENT '1高 2中 3低',
  source_type VARCHAR(32) DEFAULT NULL COMMENT 'SEARCH_RESULT/ARTIFACT/MANUAL',
  source_ref_id BIGINT DEFAULT NULL,
  due_time DATETIME DEFAULT NULL,
  finished_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_study_task_user (user_id),
  KEY idx_study_task_workspace (workspace_id),
  KEY idx_study_task_project (project_id),
  KEY idx_study_task_status (task_status),
  CONSTRAINT fk_study_task_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_study_task_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
  CONSTRAINT fk_study_task_project FOREIGN KEY (project_id) REFERENCES research_project(id)
) ENGINE=InnoDB COMMENT='研究计划任务';

CREATE TABLE IF NOT EXISTS pomodoro_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  task_id BIGINT DEFAULT NULL,
  focus_minutes INT NOT NULL DEFAULT 25,
  break_minutes INT NOT NULL DEFAULT 5,
  cycle_index INT NOT NULL DEFAULT 1,
  started_at DATETIME NOT NULL,
  finished_at DATETIME DEFAULT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DONE' COMMENT 'DONE/ABORTED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_pomodoro_user (user_id),
  KEY idx_pomodoro_task (task_id),
  CONSTRAINT fk_pomodoro_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_pomodoro_task FOREIGN KEY (task_id) REFERENCES study_task(id)
) ENGINE=InnoDB COMMENT='番茄钟记录';

ALTER TABLE pomodoro_record
  ADD COLUMN IF NOT EXISTS cycle_index INT NOT NULL DEFAULT 1 AFTER break_minutes;

CREATE TABLE IF NOT EXISTS calendar_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  workspace_id BIGINT DEFAULT NULL,
  project_id BIGINT DEFAULT NULL,
  title VARCHAR(255) NOT NULL,
  description VARCHAR(1000) DEFAULT NULL,
  event_type VARCHAR(32) NOT NULL DEFAULT 'PLAN' COMMENT 'PLAN/MEETING/DEADLINE/REMINDER',
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  all_day TINYINT NOT NULL DEFAULT 0,
  source_ref_id BIGINT DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_calendar_user (user_id),
  KEY idx_calendar_workspace (workspace_id),
  KEY idx_calendar_project (project_id),
  KEY idx_calendar_time (start_time, end_time),
  CONSTRAINT fk_calendar_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_calendar_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id),
  CONSTRAINT fk_calendar_project FOREIGN KEY (project_id) REFERENCES research_project(id)
) ENGINE=InnoDB COMMENT='日历事件';

CREATE TABLE IF NOT EXISTS checkin_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  checkin_date DATE NOT NULL,
  focus_minutes INT NOT NULL DEFAULT 0,
  completed_task_count INT NOT NULL DEFAULT 0,
  summary_text VARCHAR(500) DEFAULT NULL,
  mood_code VARCHAR(16) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_checkin_user_date (user_id, checkin_date),
  CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='每日打卡';

CREATE TABLE IF NOT EXISTS user_daily_usage (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  usage_date DATE NOT NULL,
  today_time INT NOT NULL DEFAULT 0 COMMENT '累计使用分钟数',
  last_increment_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_daily_usage (user_id, usage_date),
  KEY idx_user_daily_usage_user (user_id),
  CONSTRAINT fk_user_daily_usage_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='用户每日使用时长';

CREATE TABLE IF NOT EXISTS async_job (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  job_type VARCHAR(32) NOT NULL COMMENT 'SEARCH/PARSE/SUMMARY/REPORT/IMPORT',
  biz_type VARCHAR(32) NOT NULL,
  biz_ref_id BIGINT DEFAULT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED/CANCELED',
  progress_percent INT NOT NULL DEFAULT 0,
  error_message VARCHAR(1000) DEFAULT NULL,
  started_at DATETIME DEFAULT NULL,
  finished_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_async_job_user (user_id),
  KEY idx_async_job_status (status),
  KEY idx_async_job_type (job_type),
  CONSTRAINT fk_async_job_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='异步任务中心';
