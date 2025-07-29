package com.ssafy.bbatty.domain.chat.common.service;

import com.ssafy.bbatty.domain.chat.common.dto.ChatTokenClaims;
import com.ssafy.bbatty.domain.chat.common.enums.ChatType;

import java.util.Map;

/**
 * 채팅 토큰 생성 및 검증 서비스 인터페이스
 */
public interface ChatTokenService {

    /**
     * 채팅 토큰 생성
     * @param userId 사용자 ID
     * @param chatType 채팅 타입 (GAME, MATCH)
     * @param claims 추가 클레임 정보
     * @return JWT 토큰
     */
    String generateChatToken(String userId, ChatType chatType, Map<String, Object> claims);

    /**
     * 채팅 토큰 검증 및 클레임 추출
     * @param token JWT 토큰
     * @return 토큰 클레임 정보
     */
    ChatTokenClaims validateChatToken(String token);

    /**
     * 토큰 만료 여부 확인
     * @param token JWT 토큰
     * @return 만료 여부
     */
    boolean isTokenExpired(String token);

    /**
     * 토큰에서 사용자 ID 추출 (검증 없이)
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    String extractUserIdWithoutValidation(String token);
}