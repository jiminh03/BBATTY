● BBATTY 프로젝트 포팅 메뉴얼

  1. 프로젝트 개요

  - 프로젝트명: BBATTY
  - 언어: Java
  - 프레임워크: Spring Boot 3.5.3

  2. JVM 및 WAS 환경

  JVM 버전

  - Java: OpenJDK 21
  - Gradle: 8.5.0

  WAS 제품

  - Embedded Tomcat (Spring Boot 내장)
  - 포트 구성:
    - bbatty-service: 8080
    - chat-service-1: 8084
    - chat-service-2: 8085 (외부) → 8084 (내부)
    - schedule-service: 8082
    - nginx 로드밸런서: 8083

  3. 서비스 구성

  3.1 bbatty (메인 서비스)

  - 포트: 8080
  - 기능: 사용자 인증, 게시판, 출석, 랭킹, 알림

  3.2 chat (채팅 서비스)

  - 포트: 8084 (2개 인스턴스)
  - 기능: WebSocket 채팅, 실시간 통신
  - 로드밸런싱: Nginx (IP Hash 방식)

  3.3 schedule (스케줄링 서비스)

  - 포트: 8082
  - 기능: 게임 데이터 크롤링, 뉴스 요약, 배치 작업

  4. Docker 환경 설정

  4.1 Dockerfile 구성

  각 서비스별 멀티스테이지 빌드:
  FROM gradle:8.5.0-jdk21 AS builder
  FROM openjdk:21-jdk-slim

  4.2 docker-compose.yml 주요 서비스

  - MySQL 8.0 (3306)
  - Redis (6379, 6380 - chat용 별도)
  - Kafka (9092, KRaft 모드)
  - Nginx (로드밸런서)

  5. 환경변수 설정

  5.1 데이터베이스

  MYSQL_ROOT_PASSWORD: [DB 루트 패스워드]
  MYSQL_DATABASE: [DB명]
  MYSQL_USER: [DB 사용자]
  MYSQL_PASSWORD: [DB 패스워드]
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql-db:3306/${MYSQL_DATABASE}                                                                                                               

  5.2 Redis

  REDIS_PASSWORD: [Redis 패스워드]
  SPRING_DATA_REDIS_HOST: redis-cache / chat-redis                                                                                                                                  
  SPRING_DATA_REDIS_PORT: 6379                                                                                                                                                      

  5.3 Kafka

  KAFKA_BOOTSTRAP_SERVERS: kafka:9092                                                                                                                                               

  6. 외부 API 연동 설정

  6.1 카카오 소셜 로그인

  KAKAO_CLIENT_ID: [카카오 REST API 키]
  KAKAO_CLIENT_SECRET: [카카오 시크릿 키]
  KAKAO_REDIRECT_URI: [리다이렉트 URI]
  - 사용 스코프: account_email, birthday, gender
  - API URL: https://kapi.kakao.com/v2/user/me                                                                                                                                      

  6.2 AWS S3

  CLOUD_AWS_CREDENTIALS_ACCESS_KEY: [AWS 액세스 키]
  CLOUD_AWS_CREDENTIALS_SECRET_KEY: [AWS 시크릿 키]
  CLOUD_AWS_S3_BUCKET: [S3 버킷명]
  - 리전: ap-northeast-2 (서울)
  - SDK: AWS Java SDK v1.12.765

  6.3 OpenAI ChatGPT API

  OPENAI_GPT_KEY: [OpenAI API 키]
  - 모델: gpt-4o-mini
  - 사용 목적: 뉴스 요약 서비스

  6.4 네이버 뉴스 API

  NAVER_CLIENT_ID: [네이버 클라이언트 ID]
  NAVER_CLIENT_SECRET: [네이버 클라이언트 시크릿]

  6.5 Firebase FCM

  FIREBASE_CREDENTIALS_PATH: [Firebase 서비스 계정 키 파일 경로]
  FIREBASE_PROJECT_ID: [Firebase 프로젝트 ID]

  6.6 JWT 토큰

  JWT_SECRET: [JWT 시크릿 키]

  7. 배포 환경

  7.1 Jenkins CI/CD

  - Docker 기반 자동 배포
  - GitLab 연동
  - 자동 빌드 및 테스트

  7.2 Nginx 로드밸런싱

  - 업스트림: chat-service-1, chat-service-2
  - 방식: IP Hash (WebSocket 세션 고정)
  - WebSocket 지원: Upgrade 헤더 처리

  8. 화면별 주요 기능

  8.1 인증 화면

  - 카카오 소셜 로그인
  - 회원가입 (닉네임, 팀 선택)

  8.2 메인 화면

  - 게시글 목록
  - 팀별 뉴스 요약 (ChatGPT)
  - 출석체크

  8.3 채팅 화면

  - 실시간 WebSocket 채팅
  - 매치 채팅룸 생성/참여
  - 관전 채팅

  8.4 마이페이지

  - 프로필 수정 (S3 이미지 업로드)
  - 출석 기록
  - 배지 시스템

  8.5 랭킹 화면

  - 전체/팀별 랭킹
  - 통계 데이터 시각화

  9. 실행 방법

  9.1 환경변수 파일 설정

  프로젝트 루트에 .env 파일 생성 후 위 환경변수들 설정

  9.2 Docker Compose 실행

  docker-compose up -d