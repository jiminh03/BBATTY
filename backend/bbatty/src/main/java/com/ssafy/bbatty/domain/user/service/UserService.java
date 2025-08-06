package com.ssafy.bbatty.domain.user.service;

import com.ssafy.bbatty.domain.user.dto.request.UserUpdateRequestDto;
import com.ssafy.bbatty.domain.user.dto.response.UserResponseDto;

public interface UserService {

    /**
     * 사용자 프로필 조회 (다른 사용자가 조회)
     */
    UserResponseDto getUserProfile(Long userId, Long currentUserId);

    /**
     * 내 프로필 조회
     */
    UserResponseDto getMyProfile(Long currentUserId);

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
