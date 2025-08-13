package com.ssafy.bbatty.domain.chat.strategy;

import com.ssafy.bbatty.domain.chat.dto.request.ChatAuthRequest;
import com.ssafy.bbatty.domain.game.entity.Game;
import com.ssafy.bbatty.domain.game.repository.GameRepository;
import com.ssafy.bbatty.global.constants.ErrorCode;
import com.ssafy.bbatty.global.constants.GameStatus;
import com.ssafy.bbatty.global.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

/**
 * 관전 채팅 검증 전략
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WatchChatValidationStrategy implements ChatValidationStrategy {
    
    private final GameRepository gameRepository;
    
    @Override
    public void validateChatPermission(Long userId, Long userTeamId, String userGender, int userAge, ChatAuthRequest request) {
        validateWatchChatPermission(userTeamId, request);
    }
    
    @Override
    public String getSupportedChatType() {
        return "WATCH";
    }
    
    /**
     * 직관 채팅 권한 검증 (사용자 팀 == 응원 팀)
     */
    private void validateWatchChatPermission(Long userTeamId, ChatAuthRequest request) {
        if (request.getGameId() == null || request.getRoomInfo() == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        try {
            // roomInfo에서 응원할 팀 정보 추출
            Map<String, Object> roomInfo = request.getRoomInfo();
            Object teamIdObj = roomInfo.get("teamId");
            
            if (teamIdObj == null) {
                log.error("직관 채팅방 정보에 teamId가 없습니다. roomInfo={}", roomInfo);
                throw new ApiException(ErrorCode.ROOM_INFO_MISSING);
            }
            
            Long supportTeamId = ((Number) teamIdObj).longValue();

            // 경기 정보 확인
            Game game = gameRepository.findById(request.getGameId())
                    .orElseThrow(() -> new ApiException(ErrorCode.GAME_NOT_FOUND));

            // 경기 상태 확인 (종료된 경기는 직관 채팅 불가)
            if (game.getStatus() == GameStatus.FINISHED) {
                throw new ApiException(ErrorCode.GAME_FINISHED);
            }

            // 당일 경기인지 확인 (한국 시간 기준, 당일 경기가 아니면 직관 채팅 불가)
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
            LocalDate gameDate = game.getDateTime().toLocalDate();
            if (!gameDate.equals(today)) {
                log.warn("당일 경기가 아닌 채팅방 입장 시도: gameId={}, gameDate={}, today={}", 
                        request.getGameId(), gameDate, today);
                throw new ApiException(ErrorCode.NO_GAME_TODAY);
            }

            // 응원할 팀이 경기에 참여하는지 확인
            if (!game.getHomeTeamId().equals(supportTeamId) && !game.getAwayTeamId().equals(supportTeamId)) {
                log.warn("경기에 참여하지 않는 팀으로 직관 채팅 시도: gameId={}, supportTeamId={}, homeTeamId={}, awayTeamId={}", 
                        request.getGameId(), supportTeamId, game.getHomeTeamId(), game.getAwayTeamId());
                throw new ApiException(ErrorCode.TEAM_NOT_IN_GAME);
            }

            // 사용자 팀과 응원 팀이 같은지 확인
            if (!userTeamId.equals(supportTeamId)) {
                log.info("다른 팀 직관 채팅 시도: userId의 teamId={}, 응원 teamId={}", userTeamId, supportTeamId);
                throw new ApiException(ErrorCode.UNAUTHORIZED_TEAM_ACCESS);
            }

            log.info("직관 채팅 권한 검증 완료: userId teamId={}, supportTeamId={}, gameId={}", 
                    userTeamId, supportTeamId, request.getGameId());
                    
        } catch (ClassCastException | NumberFormatException e) {
            log.error("직관 채팅방 정보 파싱 오류: roomInfo={}, error={}", request.getRoomInfo(), e.getMessage());
            throw new ApiException(ErrorCode.ROOM_INFO_MISSING);
        }
    }
}