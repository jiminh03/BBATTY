package com.ssafy.chat.common.service;

import com.ssafy.chat.common.dto.SessionInfo;
import com.ssafy.chat.common.dto.UserInfo;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 세션과 Redis 사용자 정보 동기화 서비스
 * 분산 환경에서 실제 활성 세션과 저장된 사용자 정보를 일치시킴
 */
public interface SessionSyncService {
    
    // ===========================================
    // 실시간 동기화
    // ===========================================
    
    /**
     * 특정 채팅방의 세션-사용자 정보 동기화
     * @param roomId 채팅방 ID
     * @return 동기화 결과 (정리된 세션 수)
     */
    int syncSessionsInRoom(String roomId);
    
    /**
     * 모든 채팅방의 세션-사용자 정보 동기화
     * @return 동기화 결과 맵 (채팅방 ID -> 정리된 세션 수)
     */
    Map<String, Integer> syncAllRoomSessions();
    
    /**
     * 특정 사용자의 중복 세션 정리
     * @param userId 사용자 ID
     * @return 정리된 중복 세션 수
     */
    int cleanupDuplicateUserSessions(Long userId);
    
    // ===========================================
    // 세션 검증 및 정리
    // ===========================================
    
    /**
     * 실제로 연결되지 않은 세션 정보 정리 (좀비 세션 제거)
     * @param roomId 채팅방 ID
     * @return 정리된 좀비 세션 수
     */
    int removeZombieSessions(String roomId);
    
    /**
     * Redis에는 있지만 실제 WebSocket 연결이 없는 사용자 정리
     * @param roomId 채팅방 ID
     * @return 정리된 사용자 수
     */
    int removeOrphanedUsers(String roomId);
    
    /**
     * WebSocket 연결은 있지만 Redis에 정보가 없는 세션 보정
     * @param roomId 채팅방 ID
     * @return 보정된 세션 수
     */
    int repairIncompleteSessionInfo(String roomId);
    
    // ===========================================
    // 통계 및 모니터링
    // ===========================================
    
    /**
     * 채팅방별 세션-사용자 불일치 현황 조회
     * @return 채팅방 ID -> 불일치 정보 맵
     */
    Map<String, SyncStatus> getRoomSyncStatus();
    
    /**
     * 특정 채팅방의 동기화 상태 조회
     * @param roomId 채팅방 ID
     * @return 동기화 상태 정보
     */
    SyncStatus getRoomSyncStatus(String roomId);
    
    /**
     * 전체 시스템의 동기화 통계
     * @return 동기화 통계 정보
     */
    SyncStatistics getGlobalSyncStatistics();
    
    // ===========================================
    // 강제 동기화
    // ===========================================
    
    /**
     * 특정 채팅방을 Redis 정보 기준으로 강제 동기화
     * 실제 WebSocket 연결 상태와 관계없이 Redis 정보를 기준으로 함
     * @param roomId 채팅방 ID
     * @param forceRemoveDisconnected 연결되지 않은 세션 강제 제거 여부
     * @return 동기화 결과
     */
    SyncResult forceSync(String roomId, boolean forceRemoveDisconnected);
    
    /**
     * 특정 채팅방을 실제 WebSocket 연결 기준으로 강제 동기화
     * Redis 정보를 실제 연결 상태에 맞게 조정
     * @param roomId 채팅방 ID
     * @return 동기화 결과
     */
    SyncResult forceSyncByActualSessions(String roomId);
    
    // ===========================================
    // 내부 클래스들
    // ===========================================
    
    /**
     * 동기화 상태 정보
     */
    class SyncStatus {
        private final String roomId;
        private final int redisUserCount;          // Redis에 저장된 사용자 수
        private final int actualSessionCount;      // 실제 WebSocket 세션 수
        private final int zombieSessionCount;      // 좀비 세션 수
        private final int orphanedUserCount;       // 고아 사용자 정보 수
        private final boolean isSynced;            // 동기화 상태
        private final long lastSyncTime;           // 마지막 동기화 시각
        
        public SyncStatus(String roomId, int redisUserCount, int actualSessionCount, 
                         int zombieSessionCount, int orphanedUserCount, boolean isSynced, long lastSyncTime) {
            this.roomId = roomId;
            this.redisUserCount = redisUserCount;
            this.actualSessionCount = actualSessionCount;
            this.zombieSessionCount = zombieSessionCount;
            this.orphanedUserCount = orphanedUserCount;
            this.isSynced = isSynced;
            this.lastSyncTime = lastSyncTime;
        }
        
        // getters
        public String getRoomId() { return roomId; }
        public int getRedisUserCount() { return redisUserCount; }
        public int getActualSessionCount() { return actualSessionCount; }
        public int getZombieSessionCount() { return zombieSessionCount; }
        public int getOrphanedUserCount() { return orphanedUserCount; }
        public boolean isSynced() { return isSynced; }
        public long getLastSyncTime() { return lastSyncTime; }
        
        public int getDiscrepancy() {
            return Math.abs(redisUserCount - actualSessionCount);
        }
    }
    
    /**
     * 동기화 결과 정보
     */
    class SyncResult {
        private final String roomId;
        private final boolean success;
        private final int removedZombieSessions;
        private final int removedOrphanedUsers;
        private final int repairedSessions;
        private final String errorMessage;
        private final long syncTime;
        
        public SyncResult(String roomId, boolean success, int removedZombieSessions, 
                         int removedOrphanedUsers, int repairedSessions, String errorMessage) {
            this.roomId = roomId;
            this.success = success;
            this.removedZombieSessions = removedZombieSessions;
            this.removedOrphanedUsers = removedOrphanedUsers;
            this.repairedSessions = repairedSessions;
            this.errorMessage = errorMessage;
            this.syncTime = System.currentTimeMillis();
        }
        
        // getters
        public String getRoomId() { return roomId; }
        public boolean isSuccess() { return success; }
        public int getRemovedZombieSessions() { return removedZombieSessions; }
        public int getRemovedOrphanedUsers() { return removedOrphanedUsers; }
        public int getRepairedSessions() { return repairedSessions; }
        public String getErrorMessage() { return errorMessage; }
        public long getSyncTime() { return syncTime; }
        
        public int getTotalChanges() {
            return removedZombieSessions + removedOrphanedUsers + repairedSessions;
        }
    }
    
    /**
     * 전체 동기화 통계
     */
    class SyncStatistics {
        private final int totalRooms;
        private final int syncedRooms;
        private final int unsyncedRooms;
        private final int totalZombieSessions;
        private final int totalOrphanedUsers;
        private final double avgDiscrepancyRate;
        private final long lastGlobalSyncTime;
        
        public SyncStatistics(int totalRooms, int syncedRooms, int unsyncedRooms, 
                            int totalZombieSessions, int totalOrphanedUsers, 
                            double avgDiscrepancyRate, long lastGlobalSyncTime) {
            this.totalRooms = totalRooms;
            this.syncedRooms = syncedRooms;
            this.unsyncedRooms = unsyncedRooms;
            this.totalZombieSessions = totalZombieSessions;
            this.totalOrphanedUsers = totalOrphanedUsers;
            this.avgDiscrepancyRate = avgDiscrepancyRate;
            this.lastGlobalSyncTime = lastGlobalSyncTime;
        }
        
        // getters
        public int getTotalRooms() { return totalRooms; }
        public int getSyncedRooms() { return syncedRooms; }
        public int getUnsyncedRooms() { return unsyncedRooms; }
        public int getTotalZombieSessions() { return totalZombieSessions; }
        public int getTotalOrphanedUsers() { return totalOrphanedUsers; }
        public double getAvgDiscrepancyRate() { return avgDiscrepancyRate; }
        public long getLastGlobalSyncTime() { return lastGlobalSyncTime; }
        
        public double getSyncedRatio() {
            return totalRooms > 0 ? (double) syncedRooms / totalRooms : 0.0;
        }
    }
}