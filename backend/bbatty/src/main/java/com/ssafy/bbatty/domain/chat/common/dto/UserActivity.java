package com.ssafy.bbatty.domain.chat.common.dto;

import com.ssafy.bbatty.domain.chat.common.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 활동 정보 DTO (간소화 버전)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivity {

    /** 사용자 ID */
    private String userId;

    /** 사용자 이름 */
    private String userName;

    /** 방 ID */
    private String roomId;

    /** 입장 시간 */
    @Builder.Default
    private LocalDateTime joinTime = LocalDateTime.now();

    /** 활동 상태 */
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * 사용자가 활성 상태인지 확인
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * 메시지 전송 가능한지 확인
     */
    public boolean canSendMessage() {
        return status != null && status.canSendMessage();
    }

}