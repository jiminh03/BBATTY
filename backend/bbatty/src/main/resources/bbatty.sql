-- ===================================
-- 야구 직관 커뮤니티 "BBATTY" DB 스키마
-- ===================================
CREATE DATABASE IF NOT EXISTS BBATTY;
USE BBATTY;

-- KBO 팀 정보 테이블
CREATE TABLE `team` (
            `id` BIGINT NOT NULL AUTO_INCREMENT,
            `name` VARCHAR(20) NOT NULL COMMENT '팀명',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (`id`)
) COMMENT='KBO 팀 정보';

INSERT INTO `team` (`id`, `name`) VALUES
                    (1, '한화 이글스'),
                    (2, 'LG 트윈스'),
                    (3, '롯데 자이언츠'),
                    (4, 'KT 위즈'),
                    (5, '삼성 라이온즈'),
                    (6, 'KIA 타이거즈'),
                    (7, 'SSG 랜더스'),
                    (8, 'NC 다이노스'),
                    (9, '두산 베어스'),
                    (10, '키움 히어로즈');

-- KBO 경기 정보 테이블
CREATE TABLE `game` (
            `id` BIGINT NOT NULL AUTO_INCREMENT,
            `away_team_id` BIGINT NOT NULL COMMENT '어웨이팀 ID',
            `home_team_id` BIGINT NOT NULL COMMENT '홈팀 ID',
            `status` ENUM('SCHEDULED', 'FINISHED', 'CANCELLED') NOT NULL DEFAULT 'SCHEDULED' COMMENT '경기 상태',
            `away_score` INT NULL DEFAULT 0 COMMENT '어웨이팀 점수',
            `home_score` INT NULL DEFAULT 0 COMMENT '홈팀 점수',
            `date_time` DATETIME NOT NULL COMMENT '경기 일시',
            `result` ENUM('HOME_WIN', 'AWAY_WIN', 'DRAW', 'CANCELLED') NULL COMMENT '경기 결과',
            `stadium` VARCHAR(50) NOT NULL COMMENT '경기장명',
            `latitude` DECIMAL(10, 7) NULL COMMENT '경기장 위도',
            `longitude` DECIMAL(10, 7) NULL COMMENT '경기장 경도',
            `double_header` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '더블헤더 여부',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (`id`),
            FOREIGN KEY (`away_team_id`) REFERENCES `team`(`id`) ON DELETE RESTRICT,
            FOREIGN KEY (`home_team_id`) REFERENCES `team`(`id`) ON DELETE RESTRICT,
            INDEX `idx_date_time` (`date_time` DESC) COMMENT '직관 기록 날짜순 정렬용'
) COMMENT='KBO 경기 정보';

-- 사용자 직관 로그 테이블 (순수 로그)
CREATE TABLE `user_attended` (
             `user_id` BIGINT NOT NULL COMMENT '사용자 ID',
             `game_id` BIGINT NOT NULL COMMENT '경기 ID',
             PRIMARY KEY (`user_id`, `game_id`) COMMENT '중복 직관 방지',
             FOREIGN KEY (`game_id`) REFERENCES `game`(`id`) ON DELETE CASCADE
) COMMENT='사용자 직관 로그 (원본 이벤트 데이터)';

-- ===================================
-- 2. 사용자 관련 테이블
-- ===================================

-- 사용자 기본 정보 테이블
CREATE TABLE `user` (
            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
            `team_id` BIGINT NOT NULL COMMENT '응원팀 ID',
            `nickname` VARCHAR(20) NOT NULL COMMENT '닉네임',
            `gender` ENUM('MALE', 'FEMALE') NOT NULL COMMENT '성별',
            `birth_year` INT NOT NULL COMMENT '생년',
            `role` ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '사용자 권한',
            `profile_img` VARCHAR(255) NULL COMMENT '프로필 이미지 URL',
            `introduction` TEXT NULL COMMENT '자기소개',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (`id`),
            FOREIGN KEY (`team_id`) REFERENCES `team`(`id`) ON DELETE RESTRICT
) COMMENT='사용자 기본 정보';

-- 사용자 상세 정보 테이블 (민감정보)
CREATE TABLE `user_info` (
             `id` BIGINT NOT NULL AUTO_INCREMENT,
             `user_id` BIGINT NOT NULL COMMENT '사용자 ID 참조',
             `kakao_id` VARCHAR(50) NULL COMMENT '카카오 ID',
             `email` VARCHAR(100) NOT NULL COMMENT '이메일',
             `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
             PRIMARY KEY (`id`),
             FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) COMMENT='사용자 상세 정보 (민감정보)';

-- ===================================
-- 커뮤니티 테이블
-- ===================================

-- 게시글 테이블
CREATE TABLE `post` (
            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '게시글 ID',
            `user_id` BIGINT NOT NULL COMMENT '작성자 ID',
            `team_id` BIGINT NOT NULL COMMENT '팀 ID',
            `title` VARCHAR(100) NOT NULL COMMENT '제목',
            `content` TEXT NOT NULL COMMENT '내용',
            `is_same_team` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '같은 팀만 댓글 허용',
            `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (`id`),
            FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
            FOREIGN KEY (`team_id`) REFERENCES `team`(`id`) ON DELETE RESTRICT,
            INDEX `idx_team_latest` (`team_id`, `created_at` DESC) COMMENT '팀별 최신글 조회용',
            INDEX `idx_user_posts` (`user_id`, `created_at` DESC) COMMENT '사용자별 게시글 조회용'
) COMMENT='게시글';

-- 게시글 이미지 테이블
CREATE TABLE `post_image` (
              `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '이미지 ID',
              `post_id` BIGINT NOT NULL COMMENT '게시글 ID',
              `image_url` VARCHAR(255) NOT NULL COMMENT '이미지 URL',
              `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부',
              PRIMARY KEY (`id`),
              FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE
) COMMENT='게시글 이미지';

-- 게시글 좋아요 테이블
CREATE TABLE `post_like` (
             `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '좋아요 ID',
             `user_id` BIGINT NOT NULL COMMENT '사용자 ID',
             `post_id` BIGINT NOT NULL COMMENT '게시글 ID',
             `like_action` ENUM('LIKE', 'UNLIKE') NOT NULL COMMENT 'LIKE 또는 UNLIKE',
             `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '조회 시각',
             PRIMARY KEY (`id`),
             FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
             FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE
) COMMENT='게시글 좋아요';


-- 게시글 조회 로그 테이블
CREATE TABLE `post_view` (
            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '조회 로그 ID',
            `user_id` BIGINT NOT NULL COMMENT '조회한 사용자 ID',
            `post_id` BIGINT NOT NULL COMMENT '조회한 게시글 ID',
            `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '조회 시각',
            PRIMARY KEY (`id`),
            FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
            FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE,
            INDEX `idx_post_view_count` (`post_id`, `created_at` DESC) COMMENT '게시글별 조회수 집계용',
            INDEX `idx_user_view_history` (`user_id`, `created_at` DESC) COMMENT '사용자 조회 기록용'
) COMMENT='게시글 조회 로그';

-- 댓글 테이블
CREATE TABLE `comment` (
           `id` BIGINT NOT NULL AUTO_INCREMENT,
           `post_id` BIGINT NOT NULL COMMENT '게시글 ID',
           `user_id` BIGINT NOT NULL COMMENT '작성자 ID',
           `content` TEXT NOT NULL COMMENT '댓글 내용',
           `depth` INT NOT NULL DEFAULT 0 COMMENT '댓글 깊이 (0: 댓글, 1: 대댓글)',
           `parent_id` BIGINT NULL COMMENT '부모 댓글 ID',
           `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부',
           `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
           `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
           PRIMARY KEY (`id`),
           FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE,
           FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
           FOREIGN KEY (`parent_id`) REFERENCES `comment`(`id`) ON DELETE CASCADE,
           INDEX `idx_post_created` (`post_id`, `created_at` ASC) COMMENT '게시글별 댓글 조회용'
) COMMENT='댓글';