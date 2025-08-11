package com.ssafy.bbatty.domain.chat.dto.response;

import com.ssafy.bbatty.global.response.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 인증/인가 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAuthResponse {
    
    private String requestId;       // 요청 추적용 고유 ID
    
    // 성공 시 포함되는 사용자 정보
    private UserInfo userInfo;
    
    // 채팅방 정보
    private ChatRoomInfo chatRoomInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String nickname;
        private String profileImgUrl;
        private Long teamId;
        private String teamName;
        private int age;
        private String gender;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRoomInfo {
        private String roomId;          // 통합 채팅방 ID (최상위)
        private String chatType;        // "MATCH" or "WATCH"
        private Long gameId;           // 게임 ID (비즈니스 참조)
        private String roomName;        // 방 제목
        private boolean isNewRoom;      // 새로 생성된 방인지 여부
    }
    
    public static ApiResponse<ChatAuthResponse> success(String requestId, UserInfo userInfo, ChatRoomInfo chatRoomInfo) {
        ChatAuthResponse data = ChatAuthResponse.builder()
                .requestId(requestId)
                .userInfo(userInfo)
                .chatRoomInfo(chatRoomInfo)
                .build();
        return ApiResponse.success(data);
    }
    
    public static ApiResponse<ChatAuthResponse> failure(String requestId, String errorMessage) {
        ChatAuthResponse data = ChatAuthResponse.builder()
                .requestId(requestId)
                .build();
        return ApiResponse.fail(com.ssafy.bbatty.global.constants.ErrorCode.UNAUTHORIZED, data);
    }
}