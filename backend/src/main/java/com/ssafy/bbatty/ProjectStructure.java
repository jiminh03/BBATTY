package com.ssafy.bbatty;

/**
 * BBATTY 프로젝트 도메인 구조 문서
 * 
 * 이 파일은 프로젝트의 도메인 구조를 정의하고 빈 폴더들을 Git에서 추적하기 위해 생성되었습니다.
 * 
 * @author SSAFY 11기 대전 1반 A605팀
 * @since 2025.01
 */
public class ProjectStructure {
    
    /**
     * 도메인 구조 정의
     * 
     * domain/
     * ├── board/          # 피드/게시글 기능
     * ├── chat/           # 실시간 채팅 기능  
     * ├── crawler/        # 데이터 크롤링 기능
     * ├── game/           # 경기 정보 관리
     * ├── statistics/     # 통계/랭킹 시스템
     * └── user/           # 사용자 관리
     * 
     * global/
     * ├── ai/             # AI 이미지 필터링
     * ├── api/            # 외부 API 연동
     * ├── config/         # 설정 클래스
     * ├── exception/      # 예외 처리
     * └── security/       # 보안/인증
     */
    
    // 각 도메인별 표준 패키지 구조
    public static final String[] STANDARD_PACKAGES = {
        "controller",  // REST API 엔드포인트
        "service",     // 비즈니스 로직
        "repository",  // 데이터 접근 계층
        "entity",      // JPA 엔티티
        "dto.request", // 요청 DTO
        "dto.response" // 응답 DTO
    };
}