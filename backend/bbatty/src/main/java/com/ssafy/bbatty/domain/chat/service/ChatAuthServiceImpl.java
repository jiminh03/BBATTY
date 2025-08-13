package com.ssafy.bbatty.domain.chat.service;

import com.ssafy.bbatty.domain.chat.dto.request.ChatAuthRequest;
import com.ssafy.bbatty.domain.chat.dto.response.ChatAuthResponse;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 채팅 인증/인가 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatAuthServiceImpl implements ChatAuthService {
    
    private final ChatPermissionValidatorService permissionValidatorService;
    private final GameInfoService gameInfoService;
    private final ChatMessageSenderService messageSenderService;
    private final JwtProvider jwtProvider;
    
    @Override
    public ApiResponse<ChatAuthResponse> authorizeChatAccess(Long userId, Long userTeamId, String userGender, 
                                                           int userAge, String userNickname, ChatAuthRequest request) {
        try {
            // 1. 채팅 유형별 권한 검증 (분리된 서비스 사용)
            permissionValidatorService.validateChatPermission(userId, userTeamId, userGender, userAge, request);
            
            // 2. 채팅방 정보 생성
            ChatAuthResponse.ChatRoomInfo chatRoomInfo = createChatRoomInfo(request);
            
            // 3. 사용자 정보 생성 (JWT는 userId,teamId,gender,age만, 나머지는 클라이언트 정보 사용)
            String clientNickname = userNickname; // JWT 기본값
            String profileImgUrl = "";
            
            if (request.getRoomInfo() != null) {
                // 클라이언트가 보낸 닉네임과 프로필 이미지 사용
                String roomNickname = (String) request.getRoomInfo().get("nickname");
                String roomProfileImgUrl = (String) request.getRoomInfo().get("profileImgUrl");
                
                if (roomNickname != null) {
                    clientNickname = roomNickname;
                }
                if (roomProfileImgUrl != null) {
                    profileImgUrl = roomProfileImgUrl;
                }
            }
            
            ChatAuthResponse.UserInfo userInfo = createUserInfo(userId, userTeamId, userGender, userAge, clientNickname, profileImgUrl);
            
            // 4. 게임 정보 생성 (매칭 채팅인 경우) - 분리된 서비스 사용
            Map<String, Object> gameInfo = null;
            if ("MATCH".equals(request.getChatType()) && request.getGameId() != null) {
                gameInfo = gameInfoService.createGameInfo(request.getGameId());
            }
            
            // 5. Kafka로 인증 성공 결과 전송 (분리된 서비스 사용)
            messageSenderService.sendAuthSuccessToKafka(request.getRequestId(), userInfo, chatRoomInfo, gameInfo);
            
            log.info("채팅 인증 성공: userId={}, chatType={}, action={}", 
                    userId, request.getChatType(), request.getAction());
            
            return ChatAuthResponse.success(request.getRequestId(), userInfo, chatRoomInfo);
            
        } catch (ApiException e) {
            log.warn("채팅 인증 실패: userId={}, errorCode={}, error={}", userId, e.getErrorCode().name(), e.getMessage());
            
            // Kafka로 인증 실패 결과 전송 (분리된 서비스 사용)
            messageSenderService.sendAuthFailureToKafka(request.getRequestId(), e.getMessage());
            
            return ChatAuthResponse.failure(request.getRequestId(), e.getMessage());
        }
    }
    
    /**
     * 현재 JWT 토큰 가져오기
     */
    private String getCurrentJwtToken() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7); // "Bearer " 제거
            }
            
            log.warn("Authorization 헤더가 없거나 Bearer 토큰이 아닙니다");
            throw new ApiException(ErrorCode.UNAUTHORIZED);
            
        } catch (IllegalStateException e) {
            // Kafka 요청인 경우 HTTP 컨텍스트가 없을 수 있음
            log.warn("HTTP 요청 컨텍스트를 가져올 수 없습니다 - Kafka 요청일 가능성");
            return null; // Kafka Consumer에서 직접 토큰을 처리하므로 null 반환
        }
    }
    

    /**
     * 채팅방 정보 생성
     */
    private ChatAuthResponse.ChatRoomInfo createChatRoomInfo(ChatAuthRequest request) {
        // roomId는 chat 서버에서 생성하므로 여기서는 기본 정보만 설정
        return ChatAuthResponse.ChatRoomInfo.builder()
                .roomId(request.getRoomId())         // roomId 사용 (최상위 개념)
                .chatType(request.getChatType())
                .gameId(request.getGameId())         // matchId → gameId로 변경
                .roomName("채팅방")
                .isNewRoom("CREATE".equals(request.getAction()))
                .build();
    }

    /**
     * 전달받은 정보로 UserInfo 생성
     */
    private ChatAuthResponse.UserInfo createUserInfo(Long userId, Long teamId, String gender, int age, String nickname, String profileImgUrl) {
        return ChatAuthResponse.UserInfo.builder()
                .userId(userId)
                .nickname(nickname)
                .profileImgUrl(profileImgUrl != null ? profileImgUrl : "") // 클라이언트에서 받은 프로필 이미지 사용
                .teamId(teamId)
                .teamName("") // 팀명은 별도 조회 필요
                .age(age)
                .gender(gender)
                .build();
    }
    
}