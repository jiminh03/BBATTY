package com.ssafy.bbatty.domain.user.service;

import com.ssafy.bbatty.domain.user.dto.request.UserUpdateRequestDto;
import com.ssafy.bbatty.domain.user.dto.response.UserResponseDto;
import com.ssafy.bbatty.domain.user.entity.User;
import com.ssafy.bbatty.domain.user.repository.UserRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponseDto getUserProfile(Long userId, Long currentUserId) {
        User user = findUserById(userId);
        
        // 다른 사용자가 조회하는 경우 프라이버시 설정 체크
        boolean isOwnProfile = userId.equals(currentUserId);
        
        return UserResponseDto.from(user, isOwnProfile);
    }

    @Override
    public UserResponseDto getMyProfile(Long currentUserId) {
        User user = findUserById(currentUserId);
        return UserResponseDto.from(user, true);
    }

    @Override
    @Transactional
    public UserResponseDto updateProfile(Long currentUserId, UserUpdateRequestDto request) {
        User user = findUserById(currentUserId);
        
        // 닉네임 변경 시 중복 체크
        if (!user.getNickname().equals(request.getNickname()) && 
            !isNicknameAvailable(request.getNickname(), currentUserId)) {
            throw new ApiException(ErrorCode.DUPLICATE_NICKNAME);
        }
        
        user.updateProfile(request.getNickname(), request.getIntroduction(), request.getProfileImg());
        
        return UserResponseDto.from(user, true);
    }

    @Override
    public boolean isNicknameAvailable(String nickname, Long currentUserId) {
        return !userRepository.existsByNickname(nickname) || 
               userRepository.findByNickname(nickname)
                   .map(User::getId)
                   .filter(id -> id.equals(currentUserId))
                   .isPresent();
    }

    @Override
    @Transactional
    public void updatePrivacySettings(Long currentUserId, Boolean postsPublic, Boolean statsPublic, Boolean attendanceRecordsPublic) {
        User user = findUserById(currentUserId);
        user.updatePrivacySettings(postsPublic, statsPublic, attendanceRecordsPublic);
    }

    @Override
    @Transactional
    public void deleteUser(Long currentUserId) {
        User user = findUserById(currentUserId);
        userRepository.delete(user);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
