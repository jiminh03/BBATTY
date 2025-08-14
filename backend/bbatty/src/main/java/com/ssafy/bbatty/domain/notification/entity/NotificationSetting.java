package com.ssafy.bbatty.domain.notification.entity;

import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 FCM 토큰 관리 엔티티
 * 사용자별 FCM 토큰과 디바이스 정보를 저장합니다.
 */
@Entity
@Table(name = "notification_setting", schema = "BBATTY")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NotificationSetting extends BaseEntity {

    /**
     * 기본키 (표준)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * User와 1:1 관계 (사용자별 하나의 FCM 토큰)
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * FCM 토큰 (필수, 4096자 - FCM 토큰 최대 길이)
     */
    @Column(name = "fcm_token", nullable = false, length = 4096)
    private String fcmToken;

    /**
     * 디바이스 고유 ID (선택, 토큰 갱신 시 중복 방지)
     */
    @Column(name = "device_id", length = 100)
    private String deviceId;

    /**
     * 디바이스 타입 ("ANDROID"/"IOS", 기본 ANDROID)
     */
    @Column(name = "device_type", length = 20)
    @Builder.Default
    private String deviceType = "ANDROID";



    /**
     * FCM 토큰으로 알림 설정 생성
     */
    public static NotificationSetting createWithFcmToken(User user, String fcmToken, String deviceId, String deviceType) {
        return NotificationSetting.builder()
                .user(user)
                .fcmToken(fcmToken)
                .deviceId(deviceId)
                .deviceType(deviceType != null ? deviceType : "ANDROID")
                .build();
    }

    /**
     * FCM 토큰 업데이트
     */
    public void updateFcmToken(String fcmToken, String deviceId, String deviceType) {
        this.fcmToken = fcmToken;
        if (deviceId != null) {
            this.deviceId = deviceId;
        }
        if (deviceType != null) {
            this.deviceType = deviceType;
        }
    }
}