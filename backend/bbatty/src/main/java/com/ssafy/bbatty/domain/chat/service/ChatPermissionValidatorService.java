package com.ssafy.bbatty.domain.chat.service;

import com.ssafy.bbatty.domain.chat.dto.request.ChatAuthRequest;
import com.ssafy.bbatty.domain.chat.strategy.ChatValidationStrategy;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 채팅 권한 검증 전담 서비스
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ChatPermissionValidatorService {
    
    private final Map<String, ChatValidationStrategy> validationStrategies;
    
    public ChatPermissionValidatorService(List<ChatValidationStrategy> strategies) {
        this.validationStrategies = strategies.stream()
                .collect(Collectors.toMap(
                    ChatValidationStrategy::getSupportedChatType,
                    Function.identity()
                ));
    }
    
    /**
     * 채팅 유형별 권한 검증 (Strategy 패턴 사용)
     */
    public void validateChatPermission(Long userId, Long userTeamId, String userGender, int userAge, ChatAuthRequest request) {
        String chatType = request.getChatType();
        
        ChatValidationStrategy strategy = validationStrategies.get(chatType);
        if (strategy == null) {
            log.error("지원하지 않는 채팅 타입: {}", chatType);
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        strategy.validateChatPermission(userId, userTeamId, userGender, userAge, request);
    }

}