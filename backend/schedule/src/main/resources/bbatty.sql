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