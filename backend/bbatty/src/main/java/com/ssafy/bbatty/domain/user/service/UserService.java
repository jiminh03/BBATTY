package com.ssafy.bbatty.domain.user.service;

import com.ssafy.bbatty.domain.user.dto.request.UserUpdateRequestDto;
import com.ssafy.bbatty.domain.user.dto.response.UserResponseDto;

public interface UserService {

    /**
     * 사용자 프로필 헤더 조회 (항상 공개: 닉네임, 프로필 이미지, 간단한 승률, 자기소개)
     */
    UserResponseDto getUserProfile(Long targetUserId, Long currentUserId);

    /**
     * 사용자 게시글 목록 조회 (postsPublic 검증)
     */
    Object getUserPosts(Long targetUserId, Long currentUserId, Long cursor);

    /**
     * 사용자 상세 통계 조회 (statsPublic 검증)
     */
    Object getUserStats(Long targetUserId, Long currentUserId, String season, String type);

    /**
     * 사용자 직관 기록 조회 (attendanceRecordsPublic 검증)
     */
    Object getUserAttendanceRecords(Long targetUserId, Long currentUserId, String season, Long cursor);

    /**
     * 프로필 수정 (닉네임, 프로필 이미지, 자기소개)
     */
    UserResponseDto updateProfile(Long currentUserId, UserUpdateRequestDto request);

    /**
     * 닉네임 중복 체크
     */
    boolean isNicknameAvailable(String nickname, Long currentUserId);

    /**
     * 프라이버시 설정 업데이트
     */
    void updatePrivacySettings(Long currentUserId, Boolean postsPublic, Boolean statsPublic, Boolean attendanceRecordsPublic);

    /**
     * 회원 탈퇴
     */
    void deleteUser(Long currentUserId);
}
