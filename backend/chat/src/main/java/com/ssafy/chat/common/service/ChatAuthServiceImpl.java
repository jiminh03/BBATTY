package com.ssafy.chat.common.service;

import com.ssafy.chat.common.dto.ChatSession;
import com.ssafy.chat.common.dto.ChatSessionResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 채팅 인증 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatAuthServiceImpl implements ChatAuthService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${chat.session.ttl:300}") // 기본 5분
    private int sessionTtlSeconds;
    
    @Value("${chat.websocket.url}")
    private String websocketUrl;

    @Override
    public ChatSessionResponse createChatSession(String jwtToken, String nickname, String profileImageUrl, 
                                               Integer winRate, String chatRoomId, Boolean attendanceAuth) {
        try {
            // JWT 토큰 검증 및 클레임 추출
            Claims claims = parseJwtToken(jwtToken);
            
            String userId = claims.getSubject();
            String teamId = claims.get("teamId", String.class);
            String gender = claims.get("gender", String.class);
            Integer age = claims.get("age", Integer.class);
            
            log.info("JWT 검증 완료 - userId: {}, teamId: {}, gender: {}, age: {}", 
                    userId, teamId, gender, age);
            
            // 채팅 타입별 입장 조건 검증
            boolean canJoin = false;
            if (attendanceAuth != null) {
                // 직관 채팅
                canJoin = canJoinWatchChat(teamId, chatRoomId);
            } else {
                // 매칭 채팅
                canJoin = canJoinMatchChat(userId, teamId, gender, age, winRate, chatRoomId);
            }
            
            if (!canJoin) {
                log.warn("채팅방 입장 조건 불만족 - userId: {}, chatRoomId: {}", userId, chatRoomId);
                throw new RuntimeException("채팅방 입장 조건을 만족하지 않습니다.");
            }
            
            // 세션 토큰 생성
            String sessionToken = UUID.randomUUID().toString();
            
            // Redis에 세션 정보 저장
            ChatSession session = ChatSession.builder()
                    .sessionToken(sessionToken)
                    .userId(Long.valueOf(userId))
                    .userNickname(nickname)
                    .teamId(teamId)
                    .roomId(chatRoomId)
                    .expiresAt(System.currentTimeMillis() + (sessionTtlSeconds * 1000L))
                    .build();
                    
            String redisKey = "chat_session:" + sessionToken;
            redisTemplate.opsForValue().set(redisKey, session, Duration.ofSeconds(sessionTtlSeconds));
            
            // 클라이언트 응답 생성
            ChatSessionResponse response = ChatSessionResponse.builder()
                    .sessionToken(sessionToken)
                    .wsUrl(websocketUrl)
                    .userNickname(nickname)
                    .winRate(winRate)
                    .attendanceAuth(attendanceAuth)
                    .build();
                    
            log.info("채팅 세션 생성 완료 - userId: {}, sessionToken: {}", userId, sessionToken);
            return response;
            
        } catch (Exception e) {
            log.error("채팅 세션 생성 실패", e);
            throw new RuntimeException("채팅 세션 생성에 실패했습니다.", e);
        }
    }

    @Override
    public ChatSession validateSessionToken(String sessionToken) {
        try {
            String redisKey = "chat_session:" + sessionToken;
            ChatSession session = (ChatSession) redisTemplate.opsForValue().get(redisKey);
            
            if (session == null) {
                log.warn("존재하지 않는 세션 토큰: {}", sessionToken);
                return null;
            }
            
            if (session.isExpired()) {
                log.warn("만료된 세션 토큰: {}", sessionToken);
                redisTemplate.delete(redisKey);
                return null;
            }
            
            return session;
            
        } catch (Exception e) {
            log.error("세션 토큰 검증 실패 - sessionToken: {}", sessionToken, e);
            return null;
        }
    }

    @Override
    public boolean canJoinWatchChat(String jwtTeamId, String chatRoomId) {
        try {
            // 직관 채팅: 같은 팀만 입장 가능
            // chatRoomId 형식: "watch_game_001_teamA"
            if (chatRoomId.contains("_" + jwtTeamId)) {
                log.info("직관 채팅 입장 허용 - teamId: {}, chatRoomId: {}", jwtTeamId, chatRoomId);
                return true;
            }
            
            log.warn("직관 채팅 입장 거부 - 다른 팀 - teamId: {}, chatRoomId: {}", jwtTeamId, chatRoomId);
            return false;
            
        } catch (Exception e) {
            log.error("직관 채팅 입장 조건 검증 실패", e);
            return false;
        }
    }

    @Override
    public boolean canJoinMatchChat(String jwtUserId, String jwtTeamId, String jwtGender, 
                                  Integer jwtAge, Integer winRate, String chatRoomId) {
        try {
            // 매칭 채팅방 조건을 Redis에서 조회
            String roomConditionKey = "match_room_condition:" + chatRoomId;
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> conditions = 
                (java.util.Map<String, Object>) redisTemplate.opsForHash().entries(roomConditionKey);
            
            if (conditions.isEmpty()) {
                log.warn("매칭 채팅방 조건 없음 - chatRoomId: {}", chatRoomId);
                return false;
            }
            
            // 조건 검증
            return validateMatchConditions(jwtUserId, jwtTeamId, jwtGender, jwtAge, winRate, conditions);
            
        } catch (Exception e) {
            log.error("매칭 채팅 입장 조건 검증 실패", e);
            return false;
        }
    }
    
    /**
     * JWT 토큰 파싱
     */
    private Claims parseJwtToken(String token) {
        return Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 매칭 채팅 조건 상세 검증
     */
    private boolean validateMatchConditions(String userId, String teamId, String gender, 
                                          Integer age, Integer winRate, 
                                          java.util.Map<String, Object> conditions) {
        
        // 같은 팀만 허용 조건
        Boolean sameTeamOnly = (Boolean) conditions.get("sameTeamOnly");
        if (Boolean.TRUE.equals(sameTeamOnly)) {
            String roomTeamId = (String) conditions.get("teamId");
            if (!teamId.equals(roomTeamId)) {
                log.info("매칭 채팅 입장 거부 - 다른 팀 - userTeam: {}, roomTeam: {}", teamId, roomTeamId);
                return false;
            }
        }
        
        // 성별 조건
        String preferredGender = (String) conditions.get("preferredGender");
        if (!"ALL".equals(preferredGender) && !gender.equals(preferredGender)) {
            log.info("매칭 채팅 입장 거부 - 성별 불일치 - userGender: {}, preferred: {}", gender, preferredGender);
            return false;
        }
        
        // 나이 조건
        String preferredAgeRange = (String) conditions.get("preferredAgeRange");
        if (!"ALL".equals(preferredAgeRange) && !isAgeInRange(age, preferredAgeRange)) {
            log.info("매칭 채팅 입장 거부 - 나이 불일치 - userAge: {}, preferred: {}", age, preferredAgeRange);
            return false;
        }
        
        // 승률 조건
        Integer minWinRate = (Integer) conditions.get("minWinRate");
        Integer maxWinRate = (Integer) conditions.get("maxWinRate");
        if (minWinRate != null && winRate < minWinRate) {
            log.info("매칭 채팅 입장 거부 - 승률 부족 - userWinRate: {}, min: {}", winRate, minWinRate);
            return false;
        }
        if (maxWinRate != null && winRate > maxWinRate) {
            log.info("매칭 채팅 입장 거부 - 승률 초과 - userWinRate: {}, max: {}", winRate, maxWinRate);
            return false;
        }
        
        log.info("매칭 채팅 입장 허용 - userId: {}", userId);
        return true;
    }
    
    /**
     * 나이 범위 검증
     */
    private boolean isAgeInRange(Integer age, String ageRange) {
        if (age == null || ageRange == null) return false;
        
        switch (ageRange) {
            case "20s": return age >= 20 && age < 30;
            case "30s": return age >= 30 && age < 40;
            case "40s": return age >= 40 && age < 50;
            case "50s": return age >= 50 && age < 60;
            default: return false;
        }
    }
}