-- ===================================
-- 야구 직관 커뮤니티 "BBATTY" 최종 DB 스키마
-- ===================================

CREATE DATABASE IF NOT EXISTS BBATTY;
USE BBATTY;

-- ===================================
-- 1. 마스터 데이터 테이블
-- ===================================

-- KBO 팀 정보 테이블
CREATE TABLE `team` (
                        `id` BIGINT NOT NULL AUTO_INCREMENT,
                        `name` VARCHAR(20) NOT NULL COMMENT '팀명',
                        `wins` INT NOT NULL DEFAULT 0 COMMENT '승수',
                        `draws` INT NOT NULL DEFAULT 0 COMMENT '무승부수',
                        `loses` INT NOT NULL DEFAULT 0 COMMENT '패수',
                        `team_rank` INT NULL COMMENT '현재 순위',
                        `win_rate` DECIMAL(5, 3) NULL COMMENT '승률',
                        `gb` DECIMAL(3, 1) NULL COMMENT '게임차',
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (`id`)
) COMMENT='KBO 팀 정보';

INSERT INTO `team` (`id`, `name`, `wins`, `draws`, `loses`, `team_rank`, `win_rate`, `gb`) VALUES
                                                                                               (1, '한화 이글스', 0, 0, 0, NULL, NULL, NULL),
                                                                                               (2, 'LG 트윈스', 0, 0, 0, NULL, NULL, NULL),
                                                                                               (3, '롯데 자이언츠', 0, 0, 0, NULL, NULL, NULL),
                                                                                               (4, 'KT 위즈', 0, 0, 0, NULL, NULL, NULL),
                                                                                               (5, '삼성 라이온즈', 0, 0, 0, NULL, NULL, NULL),
                                                                                               (6, 'KIA 타이거즈', 0, 0, 0, NULL, NULL, NULL),
                                                                                               (7, 'SSG 랜더스', 0, 0, 0, NULL, NULL, NULL),
                                                                                               (8, 'NC 다이노스', 0, 0, 0, NULL, NULL, NULL),
                                                                                               (9, '두산 베어스', 0, 0, 0, NULL, NULL, NULL),
                                                                                               (10, '키움 히어로즈', 0, 0, 0, NULL, NULL, NULL);

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

-- ===================================
-- 2. 사용자 관련 테이블
-- ===================================

-- 사용자 기본 정보 테이블
CREATE TABLE `user` (
                        `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
                        `team_id` BIGINT NOT NULL COMMENT '응원팀 ID',
                        `nickname` VARCHAR(20) NOT NULL COMMENT '닉네임',
                        `gender` ENUM('MALE', 'FEMALE') NOT NULL COMMENT '성별',
                        `age` INT NOT NULL COMMENT '나이',
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
                             `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             PRIMARY KEY (`id`),
                             FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) COMMENT='사용자 상세 정보 (민감정보)';

-- ===================================
-- 3. 직관 로그 테이블 (원본 데이터)
-- ===================================

-- 사용자 직관 로그 테이블 (순수 로그)
CREATE TABLE `user_attended` (
                                 `user_id` BIGINT NOT NULL COMMENT '사용자 ID',
                                 `game_id` BIGINT NOT NULL COMMENT '경기 ID',
                                 PRIMARY KEY (`user_id`, `game_id`) COMMENT '중복 직관 방지',
                                 FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                                 FOREIGN KEY (`game_id`) REFERENCES `game`(`id`) ON DELETE CASCADE
) COMMENT='사용자 직관 로그 (원본 이벤트 데이터)';

-- ===================================
-- 4. 통계 테이블들 (집계 데이터)
-- ===================================

-- 사용자 기본 통계 테이블
CREATE TABLE `user_stats` (
                              `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
                              `user_id` BIGINT NOT NULL COMMENT '사용자 ID',
                              `season` INT NOT NULL COMMENT '시즌 (0: 통산)',
                              `total_games` INT NOT NULL DEFAULT 0 COMMENT '총 직관 경기수',
                              `wins` INT NOT NULL DEFAULT 0 COMMENT '승리 경기수',
                              `draws` INT NOT NULL DEFAULT 0 COMMENT '무승부 경기수',
                              `loses` INT NOT NULL DEFAULT 0 COMMENT '패배 경기수',
                              `win_rate` DECIMAL(5,3) NULL COMMENT '승률',
                              `current_streak` INT NOT NULL DEFAULT 0 COMMENT '현재 연승/연패 (양수: 연승, 음수: 연패)',
                              `max_win_streak` INT NOT NULL DEFAULT 0 COMMENT '최대 연승',
                              `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
                              `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_user_season` (`user_id`, `season`) COMMENT '사용자별 시즌 통계 중복 방지',
                              FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) COMMENT='사용자 기본 통계';

-- 사용자 상세 통계 테이블
CREATE TABLE `user_detailed_stats` (
                                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                       `user_id` BIGINT NOT NULL COMMENT '사용자 ID',
                                       `season` INT NOT NULL COMMENT '시즌 (0: 통산)',
                                       `stat_type` ENUM('OPPONENT', 'STADIUM', 'DAY_OF_WEEK', 'HOME_AWAY') NOT NULL COMMENT '통계 타입',
                                       `stat_key` VARCHAR(50) NOT NULL COMMENT '통계 키 (팀명, 경기장명, 요일, HOME/AWAY)',
                                       `total_games` INT NOT NULL DEFAULT 0 COMMENT '총 경기수',
                                       `wins` INT NOT NULL DEFAULT 0 COMMENT '승리 경기수',
                                       `draws` INT NOT NULL DEFAULT 0 COMMENT '무승부 경기수',
                                       `loses` INT NOT NULL DEFAULT 0 COMMENT '패배 경기수',
                                       `win_rate` DECIMAL(5,3) NULL COMMENT '승률',
                                       `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
                                       `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_user_detailed_stats` (`user_id`, `season`, `stat_type`, `stat_key`) COMMENT '상세 통계 중복 방지',
                                       FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                                       INDEX `idx_user_season_type` (`user_id`, `season`, `stat_type`, `win_rate` DESC) COMMENT '상세 통계 드롭다운 조회용'
) COMMENT='사용자 상세 통계 (상대팀별, 구장별, 요일별, 홈원정별)';

-- ===================================
-- 5. 뱃지 시스템 테이블
-- ===================================

-- 뱃지 정보 테이블
CREATE TABLE `badge` (
                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '뱃지 ID',
                         `name` VARCHAR(20) NOT NULL COMMENT '뱃지명',
                         `type` ENUM('PERMANENT', 'SEASONAL', 'SPECIAL') NOT NULL COMMENT '뱃지 타입',
                         `category` ENUM('STADIUM', 'WIN', 'ATTENDANCE', 'WIN_FAIRY') NOT NULL COMMENT '뱃지 종류',
                         PRIMARY KEY (`id`)
) COMMENT='뱃지 정보';

-- 사용자 뱃지 획득 테이블
CREATE TABLE `user_badge` (
                              `id` BIGINT NOT NULL COMMENT '사용자 뱃지 ID',
                              `user_id` BIGINT NOT NULL COMMENT '사용자 ID',
                              `badge_id` BIGINT NOT NULL COMMENT '뱃지 ID',
                              `season` INT NOT NULL COMMENT '획득 시즌',
                              PRIMARY KEY (`id`),
                              FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                              FOREIGN KEY (`badge_id`) REFERENCES `badge`(`id`) ON DELETE RESTRICT
) COMMENT='사용자 뱃지 획득 기록';

-- ===================================
-- 6. 커뮤니티 테이블
-- ===================================

-- 게시글 테이블
CREATE TABLE `post` (
                        `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '게시글 ID',
                        `user_id` BIGINT NOT NULL COMMENT '작성자 ID',
                        `team_id` BIGINT NOT NULL COMMENT '팀 ID',
                        `title` VARCHAR(100) NOT NULL COMMENT '제목',
                        `content` TEXT NOT NULL COMMENT '내용',
                        `view_count` INT NOT NULL DEFAULT 0 COMMENT '조회수',
                        `is_same_team` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '같은 팀만 댓글 허용',
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
                              PRIMARY KEY (`id`),
                              FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE,
) COMMENT='게시글 이미지';

-- 게시글 좋아요 테이블
CREATE TABLE `post_like` (
     `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '좋아요 ID',
     `user_id` BIGINT NOT NULL COMMENT '사용자 ID',
     `post_id` BIGINT NOT NULL COMMENT '게시글 ID',
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
                           `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           PRIMARY KEY (`id`),
                           FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE,
                           FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                           FOREIGN KEY (`parent_id`) REFERENCES `comment`(`id`) ON DELETE CASCADE,
                           INDEX `idx_post_created` (`post_id`, `created_at` ASC) COMMENT '게시글별 댓글 조회용'
) COMMENT='댓글';