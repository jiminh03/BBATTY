package com.ssafy.bbatty.domain.notification.repository;

import com.ssafy.bbatty.domain.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 알림 설정 Repository
 */
@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    /**
     * 사용자 ID로 알림 설정 조회
     */
    Optional<NotificationSetting> findByUserId(Long userId);

    /**
     * FCM 토큰으로 알림 설정 조회
     */
    Optional<NotificationSetting> findByFcmToken(String fcmToken);

    /**
     * 특정 팀의 트래픽 급증 알림 활성화된 사용자들 조회
     */
    @Query("SELECT ns FROM NotificationSetting ns " +
           "WHERE ns.user.team.id = :teamId " +
           "AND ns.trafficSpikeAlertEnabled = true " +
           "AND ns.isDeleted = false " +
           "AND ns.user.isDeleted = false")
    List<NotificationSetting> findTrafficSpikeAlertEnabledUsers(@Param("teamId") Long teamId);

    /**
     * 특정 팀의 모든 알림 설정 조회
     */
    @Query("SELECT ns FROM NotificationSetting ns " +
           "WHERE ns.user.team.id = :teamId " +
           "AND ns.isDeleted = false " +
           "AND ns.user.isDeleted = false")
    List<NotificationSetting> findByTeamId(@Param("teamId") Long teamId);

    /**
     * 만료된 토큰 정리용 - 특정 날짜 이전에 업데이트되지 않은 설정들 조회
     */
    @Query("SELECT ns FROM NotificationSetting ns " +
           "WHERE ns.updatedAt < :cutoffDate " +
           "AND ns.isDeleted = false")
    List<NotificationSetting> findExpiredTokens(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /**
     * 사용자가 존재하는지 확인
     */
    boolean existsByUserId(Long userId);

    /**
     * 사용자 ID로 알림 설정 삭제
     */
    void deleteByUserId(Long userId);

}