-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: i13a403.p.ssafy.io    Database: BBATTY
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `comment`
--

DROP TABLE IF EXISTS `comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL COMMENT '게시글 ID',
  `user_id` bigint NOT NULL COMMENT '작성자 ID',
  `content` text NOT NULL COMMENT '댓글 내용',
  `depth` int NOT NULL DEFAULT '0' COMMENT '댓글 깊이 (0: 댓글, 1: 대댓글)',
  `parent_id` bigint DEFAULT NULL COMMENT '부모 댓글 ID',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '삭제 여부',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `parent_id` (`parent_id`),
  KEY `idx_post_created` (`post_id`,`created_at`) COMMENT '게시글별 댓글 조회용',
  CONSTRAINT `comment_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `comment_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `comment_ibfk_3` FOREIGN KEY (`parent_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=90 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='댓글';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `game`
--

DROP TABLE IF EXISTS `game`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `game` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `away_team_id` bigint NOT NULL COMMENT '어웨이팀 ID',
  `home_team_id` bigint NOT NULL COMMENT '홈팀 ID',
  `status` enum('SCHEDULED','FINISHED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED' COMMENT '경기 상태',
  `away_score` int DEFAULT '0' COMMENT '어웨이팀 점수',
  `home_score` int DEFAULT '0' COMMENT '홈팀 점수',
  `date_time` datetime NOT NULL COMMENT '경기 일시',
  `result` enum('HOME_WIN','AWAY_WIN','DRAW','CANCELLED') DEFAULT NULL COMMENT '경기 결과',
  `stadium` varchar(50) NOT NULL COMMENT '경기장명',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '경기장 위도',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '경기장 경도',
  `double_header` tinyint(1) NOT NULL DEFAULT '0' COMMENT '더블헤더 여부',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `away_team_id` (`away_team_id`),
  KEY `home_team_id` (`home_team_id`),
  KEY `idx_date_time` (`date_time` DESC) COMMENT '직관 기록 날짜순 정렬용',
  CONSTRAINT `game_ibfk_1` FOREIGN KEY (`away_team_id`) REFERENCES `team` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `game_ibfk_2` FOREIGN KEY (`home_team_id`) REFERENCES `team` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1355 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='KBO 경기 정보';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_setting`
--

DROP TABLE IF EXISTS `notification_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  `updated_at` datetime(6) DEFAULT NULL,
  `device_id` varchar(100) DEFAULT NULL,
  `device_type` varchar(20) DEFAULT NULL,
  `fcm_token` varchar(4096) NOT NULL,
  `traffic_spike_alert_enabled` bit(1) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKprsli08qedapfuoqx92jd8o7x` (`user_id`),
  CONSTRAINT `FKbwsuroqorxx1boup2snb1t1u9` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post`
--

DROP TABLE IF EXISTS `post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '게시글 ID',
  `user_id` bigint NOT NULL COMMENT '작성자 ID',
  `team_id` bigint NOT NULL COMMENT '팀 ID',
  `title` varchar(100) NOT NULL COMMENT '제목',
  `content` text NOT NULL COMMENT '내용',
  `is_same_team` tinyint(1) NOT NULL DEFAULT '0' COMMENT '같은 팀만 댓글 허용',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '삭제 여부',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_team_latest` (`team_id`,`created_at` DESC) COMMENT '팀별 최신글 조회용',
  KEY `idx_user_posts` (`user_id`,`created_at` DESC) COMMENT '사용자별 게시글 조회용',
  KEY `idx_posts_team_deleted_title` (`team_id`,`is_deleted`,`title`),
  FULLTEXT KEY `ft_posts_title` (`title`) /*!50100 WITH PARSER `ngram` */ ,
  CONSTRAINT `post_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `post_ibfk_2` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=99 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='게시글';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_image`
--

DROP TABLE IF EXISTS `post_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '이미지 ID',
  `post_id` bigint NOT NULL COMMENT '게시글 ID',
  `image_url` varchar(255) NOT NULL COMMENT '이미지 URL',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '삭제 여부',
  PRIMARY KEY (`id`),
  KEY `post_id` (`post_id`),
  CONSTRAINT `post_image_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='게시글 이미지';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_like`
--

DROP TABLE IF EXISTS `post_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '좋아요 ID',
  `user_id` bigint NOT NULL COMMENT '사용자 ID',
  `post_id` bigint NOT NULL COMMENT '게시글 ID',
  `like_action` enum('LIKE','UNLIKE') NOT NULL COMMENT 'LIKE 또는 UNLIKE',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '조회 시각',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `post_id` (`post_id`),
  CONSTRAINT `post_like_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `post_like_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=977 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='게시글 좋아요';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_view`
--

DROP TABLE IF EXISTS `post_view`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_view` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '조회 로그 ID',
  `user_id` bigint NOT NULL COMMENT '조회한 사용자 ID',
  `post_id` bigint NOT NULL COMMENT '조회한 게시글 ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '조회 시각',
  PRIMARY KEY (`id`),
  KEY `idx_post_view_count` (`post_id`,`created_at` DESC) COMMENT '게시글별 조회수 집계용',
  KEY `idx_user_view_history` (`user_id`,`created_at` DESC) COMMENT '사용자 조회 기록용',
  CONSTRAINT `post_view_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `post_view_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1477 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='게시글 조회 로그';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `team`
--

DROP TABLE IF EXISTS `team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(20) NOT NULL COMMENT '팀명',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='KBO 팀 정보';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '사용자 ID',
  `team_id` bigint NOT NULL COMMENT '응원팀 ID',
  `nickname` varchar(20) NOT NULL COMMENT '닉네임',
  `gender` enum('MALE','FEMALE') NOT NULL COMMENT '성별',
  `birth_year` int NOT NULL COMMENT '생년',
  `role` enum('USER','ADMIN') NOT NULL DEFAULT 'USER' COMMENT '사용자 권한',
  `profile_img` varchar(255) DEFAULT NULL COMMENT '프로필 이미지 URL',
  `introduction` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  `attendance_records_public` bit(1) NOT NULL,
  `posts_public` bit(1) NOT NULL,
  `stats_public` bit(1) NOT NULL,
  `traffic_spike_alert_enabled` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `team_id` (`team_id`),
  CONSTRAINT `user_ibfk_1` FOREIGN KEY (`team_id`) REFERENCES `team` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=163 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='사용자 기본 정보';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_attended`
--

DROP TABLE IF EXISTS `user_attended`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_attended` (
  `user_id` bigint NOT NULL COMMENT '사용자 ID',
  `game_id` bigint NOT NULL COMMENT '경기 ID',
  PRIMARY KEY (`user_id`,`game_id`) COMMENT '중복 직관 방지',
  KEY `game_id` (`game_id`),
  CONSTRAINT `FKsdomy6uy1yd72lrguqwpp7029` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `user_attended_ibfk_1` FOREIGN KEY (`game_id`) REFERENCES `game` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='사용자 직관 로그 (원본 이벤트 데이터)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_info`
--

DROP TABLE IF EXISTS `user_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '사용자 ID 참조',
  `kakao_id` varchar(50) DEFAULT NULL COMMENT '카카오 ID',
  `email` varchar(100) NOT NULL COMMENT '이메일',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` bit(1) NOT NULL DEFAULT b'0',
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `user_info_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=270 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='사용자 상세 정보 (민감정보)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'BBATTY'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-08-18  9:58:13
