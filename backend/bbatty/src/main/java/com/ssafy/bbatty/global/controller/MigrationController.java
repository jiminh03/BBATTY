package com.ssafy.bbatty.global.controller;

import com.ssafy.bbatty.global.response.ApiResponse;
import com.ssafy.bbatty.global.util.AttendanceDataMigrationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 데이터 마이그레이션 컨트롤러 (개발/테스트용)
 */
@Slf4j
@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final AttendanceDataMigrationUtil migrationUtil;

    /**
     * 모든 직관 데이터를 DB에서 Redis로 마이그레이션
     */
    @PostMapping("/attendance/all")
    public ResponseEntity<ApiResponse<String>> migrateAllAttendanceData() {
        log.info("전체 직관 데이터 마이그레이션 요청");
        
        try {
            migrationUtil.migrateAllAttendanceDataToRedis();
            return ResponseEntity.ok(ApiResponse.success("전체 직관 데이터 마이그레이션 완료"));
        } catch (Exception e) {
            log.error("전체 마이그레이션 실패", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(com.ssafy.bbatty.global.constants.Status.ERROR, "마이그레이션 실패: " + e.getMessage(), null));
        }
    }

    /**
     * 특정 사용자의 직관 데이터를 DB에서 Redis로 마이그레이션
     */
    @PostMapping("/attendance/user/{userId}")
    public ResponseEntity<ApiResponse<String>> migrateUserAttendanceData(@PathVariable Long userId) {
        log.info("사용자 직관 데이터 마이그레이션 요청: userId={}", userId);
        
        try {
            migrationUtil.migrateUserAttendanceDataToRedis(userId);
            return ResponseEntity.ok(ApiResponse.success("사용자 " + userId + " 직관 데이터 마이그레이션 완료"));
        } catch (Exception e) {
            log.error("사용자 마이그레이션 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(com.ssafy.bbatty.global.constants.Status.ERROR, "사용자 마이그레이션 실패: " + e.getMessage(), null));
        }
    }

    /**
     * Redis에 저장된 사용자 직관 데이터 확인 (디버깅용)
     */
    @GetMapping("/debug/redis/{userId}")
    public ResponseEntity<ApiResponse<Object>> debugUserRedisData(@PathVariable Long userId) {
        log.info("Redis 데이터 디버깅: userId={}", userId);
        
        try {
            Object debugInfo = migrationUtil.debugUserRedisData(userId);
            return ResponseEntity.ok(ApiResponse.success(debugInfo));
        } catch (Exception e) {
            log.error("Redis 디버깅 실패: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(com.ssafy.bbatty.global.constants.Status.ERROR, "Redis 디버깅 실패: " + e.getMessage(), null));
        }
    }
}