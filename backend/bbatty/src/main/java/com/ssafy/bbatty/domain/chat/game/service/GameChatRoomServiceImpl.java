package com.ssafy.bbatty.domain.chat.game.service;

import com.ssafy.bbatty.domain.chat.game.dto.TeamChatRoomInfo;
import com.ssafy.bbatty.domain.chat.game.service.GameChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 경기 직관 채팅방 관리 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameChatRoomServiceImpl implements GameChatRoomService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final GameChatUserService gameChatUserService;

    private static final String CHATROOM_KEY_PREFIX = "game_chat:room:";
    private static final String ACTIVE_ROOMS_KEY = "game_chat:active_rooms";
    private static final long ROOM_TTL = 24; // 24시간

    // KBO 10개 팀 정보 (예시)
    private static final String[][] TEAMS = {
            {"1", "KIA 타이거즈", "tigers"},
            {"2", "삼성 라이온즈", "lions"},
            {"3", "LG 트윈스", "twins"},
            {"4", "두산 베어스", "bears"},
            {"5", "KT 위즈", "wiz"},
            {"6", "SSG 랜더스", "landers"},
            {"7", "롯데 자이언츠", "giants"},
            {"8", "한화 이글스", "eagles"},
            {"9", "NC 다이노스", "dinos"},
            {"10", "키움 히어로즈", "heroes"}
    };

    @Override
    public List<TeamChatRoomInfo> createTeamChatRooms(Long gameId) {
        List<TeamChatRoomInfo> chatRooms = new ArrayList<>();

        try {
            // 예시로 첫 번째와 두 번째 팀으로 경기 생성
            String[] homeTeam = TEAMS[0]; // KIA
            String[] awayTeam = TEAMS[1]; // 삼성

            // 홈팀 채팅방
            TeamChatRoomInfo homeChatRoom = createTeamChatRoom(gameId, homeTeam, awayTeam, "HOME");
            chatRooms.add(homeChatRoom);

            // 어웨이팀 채팅방
            TeamChatRoomInfo awayChatRoom = createTeamChatRoom(gameId, awayTeam, homeTeam, "AWAY");
            chatRooms.add(awayChatRoom);

            // Redis에 채팅방 정보 저장
            saveChatRoomToRedis(homeChatRoom);
            saveChatRoomToRedis(awayChatRoom);

            log.info("팀별 채팅방 생성 완료 - gameId: {}, 생성된 방 수: {}", gameId, chatRooms.size());

        } catch (Exception e) {
            log.error("팀별 채팅방 생성 실패 - gameId: {}", gameId, e);
        }

        return chatRooms;
    }

    @Override
    public void activateGameChatRooms(Long gameId) {
        try {
            String pattern = CHATROOM_KEY_PREFIX + "game_" + gameId + "_*";

            // 해당 경기의 모든 채팅방 활성화
            String homeRoomKey = CHATROOM_KEY_PREFIX + "game_" + gameId + "_team_" + TEAMS[0][0];
            String awayRoomKey = CHATROOM_KEY_PREFIX + "game_" + gameId + "_team_" + TEAMS[1][0];

            redisTemplate.opsForHash().put(homeRoomKey, "active", true);
            redisTemplate.opsForHash().put(awayRoomKey, "active", true);

            // 활성 채팅방 목록에 추가
            redisTemplate.opsForSet().add(ACTIVE_ROOMS_KEY, homeRoomKey, awayRoomKey);

            log.info("게임 채팅방 활성화 완료 - gameId: {}", gameId);
        } catch (Exception e) {
            log.error("게임 채팅방 활성화 실패 - gameId: {}", gameId, e);
        }
    }

    @Override
    public void deactivateGameChatRooms(Long gameId) {
        try {
            String homeRoomKey = CHATROOM_KEY_PREFIX + "game_" + gameId + "_team_" + TEAMS[0][0];
            String awayRoomKey = CHATROOM_KEY_PREFIX + "game_" + gameId + "_team_" + TEAMS[1][0];

            redisTemplate.opsForHash().put(homeRoomKey, "active", false);
            redisTemplate.opsForHash().put(awayRoomKey, "active", false);

            // 활성 채팅방 목록에서 제거
            redisTemplate.opsForSet().remove(ACTIVE_ROOMS_KEY, homeRoomKey, awayRoomKey);

            log.info("게임 채팅방 비활성화 완료 - gameId: {}", gameId);
        } catch (Exception e) {
            log.error("게임 채팅방 비활성화 실패 - gameId: {}", gameId, e);
        }
    }

    @Override
    public TeamChatRoomInfo getAvailableTeamChatRooms(Long userId) {
        try {
            // 사용자의 응원팀 조회 (예시로 KIA로 고정)
            String userTeamId = "1"; // KIA

            // 오늘 경기에서 해당 팀 채팅방 찾기
            List<TeamChatRoomInfo> todayRooms = getTodayTeamChatRooms();

            return todayRooms.stream()
                    .filter(room -> userTeamId.equals(room.getTeamId().toString()))
                    .filter(TeamChatRoomInfo::canJoin)
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            log.error("사용자 입장 가능 채팅방 조회 실패 - userId: {}", userId, e);
            return null;
        }
    }

    @Override
    public List<TeamChatRoomInfo> getTodayTeamChatRooms() {
        List<TeamChatRoomInfo> rooms = new ArrayList<>();

        try {
            // 예시로 현재 게임의 채팅방들 반환
            Long gameId = 123L; // 실제로는 오늘 경기 ID를 조회해야 함

            String homeRoomKey = CHATROOM_KEY_PREFIX + "game_" + gameId + "_team_" + TEAMS[0][0];
            String awayRoomKey = CHATROOM_KEY_PREFIX + "game_" + gameId + "_team_" + TEAMS[1][0];

            // 실시간 데이터로 채팅방 정보 생성 (일관성 보장)
            TeamChatRoomInfo homeRoom = buildTeamChatRoomWithRealtimeData(homeRoomKey, TEAMS[0][0]);
            TeamChatRoomInfo awayRoom = buildTeamChatRoomWithRealtimeData(awayRoomKey, TEAMS[1][0]);

            if (homeRoom != null) rooms.add(homeRoom);
            if (awayRoom != null) rooms.add(awayRoom);

            log.info("오늘 채팅방 목록 조회 완료 - 총 {}개 방", rooms.size());

        } catch (Exception e) {
            log.error("오늘 채팅방 목록 조회 실패", e);
        }

        return rooms;
    }

    @Override
    public boolean isUserAuthenticated(Long userId, Long gameId) {
        try {
            // 직관 인증 확인 로직 (예시)
            String authKey = "game_auth:" + userId + ":" + gameId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(authKey));
        } catch (Exception e) {
            log.error("직관 인증 확인 실패 - userId: {}, gameId: {}", userId, gameId, e);
            return false;
        }
    }

    @Override
    public boolean isWinningFairy(Long userId) {
        try {
            // 승리요정 여부 확인 로직 (예시)
            String fairyKey = "winning_fairy:" + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(fairyKey));
        } catch (Exception e) {
            log.error("승리요정 확인 실패 - userId: {}", userId, e);
            return false;
        }
    }

    @Override
    public void mergeDoubleHeaderChatRooms(Long gameId1, Long gameId2) {
        try {
            // 더블헤더 채팅방 통합 로직
            log.info("더블헤더 채팅방 통합 - gameId1: {}, gameId2: {}", gameId1, gameId2);

            // 첫 번째 경기 채팅방에 더블헤더 플래그 설정
            String homeRoomKey = CHATROOM_KEY_PREFIX + "game_" + gameId1 + "_team_" + TEAMS[0][0];
            String awayRoomKey = CHATROOM_KEY_PREFIX + "game_" + gameId1 + "_team_" + TEAMS[1][0];

            redisTemplate.opsForHash().put(homeRoomKey, "doubleHeader", true);
            redisTemplate.opsForHash().put(awayRoomKey, "doubleHeader", true);

        } catch (Exception e) {
            log.error("더블헤더 채팅방 통합 실패", e);
        }
    }

    @Override
    public void sendTrafficAlert(Long gameId, Long teamId) {
        try {
            // 트래픽 급증 알림 전송 로직
            log.warn("트래픽 급증 감지 - gameId: {}, teamId: {}", gameId, teamId);

            // 챗봇 공지 메시지 생성 및 전송
            String alertMessage = "현재 접속자가 많아 일시적으로 채팅이 지연될 수 있습니다.";

            // 실제로는 WebSocket을 통해 해당 팀 채팅방에 공지 전송

        } catch (Exception e) {
            log.error("트래픽 알림 전송 실패", e);
        }
    }

    @Override
    public boolean isChatRoomActive(String teamId) {
        try {
            // 현재 활성화된 채팅방 중에서 해당 팀 찾기
            String roomPattern = "*_team_" + teamId;

            // 임시로 true 반환 (실제로는 Redis에서 확인)
            return true;

        } catch (Exception e) {
            log.error("채팅방 활성화 상태 확인 실패 - teamId: {}", teamId, e);
            return false;
        }
    }

    /**
     * 팀별 채팅방 생성
     */
    private TeamChatRoomInfo createTeamChatRoom(Long gameId, String[] team, String[] opponent, String teamType) {
        String roomId = "game_" + gameId + "_team_" + team[0];

        return TeamChatRoomInfo.builder()
                .roomId(roomId)
                .roomName(team[1] + " 응원방")
                .gameId(gameId)
                .teamId(Long.valueOf(team[0]))
                .teamName(team[1])
                .opponentTeamId(Long.valueOf(opponent[0]))
                .opponentTeamName(opponent[1])
                .gameDateTime(LocalDateTime.now().plusHours(2)) // 2시간 후 경기
                .stadium("잠실야구장")
                .currentUsers(0)
                .maxUsers(200)
                .active(true) // 테스트를 위해 활성화
                .doubleHeader(false)
                .winningFairyCount(0)
                .teamType(teamType)
                .build();
    }

    /**
     * 채팅방 정보를 Redis에 저장
     */
    private void saveChatRoomToRedis(TeamChatRoomInfo chatRoom) {
        try {
            String key = CHATROOM_KEY_PREFIX + chatRoom.getRoomId();

            redisTemplate.opsForHash().put(key, "roomId", chatRoom.getRoomId());
            redisTemplate.opsForHash().put(key, "roomName", chatRoom.getRoomName());
            redisTemplate.opsForHash().put(key, "gameId", chatRoom.getGameId());
            redisTemplate.opsForHash().put(key, "teamId", chatRoom.getTeamId());
            redisTemplate.opsForHash().put(key, "teamName", chatRoom.getTeamName());
            redisTemplate.opsForHash().put(key, "active", chatRoom.getActive());
            redisTemplate.opsForHash().put(key, "currentUsers", chatRoom.getCurrentUsers());
            redisTemplate.opsForHash().put(key, "maxUsers", chatRoom.getMaxUsers());

            redisTemplate.expire(key, ROOM_TTL, TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("채팅방 Redis 저장 실패 - roomId: {}", chatRoom.getRoomId(), e);
        }
    }

    /**
     * Redis에서 채팅방 정보 로드
     */
    private TeamChatRoomInfo loadChatRoomFromRedis(String roomKey) {
        try {
            if (!redisTemplate.hasKey(roomKey)) {
                return null;
            }

            Map<Object, Object> roomData = redisTemplate.opsForHash().entries(roomKey);

            return TeamChatRoomInfo.builder()
                    .roomId((String) roomData.get("roomId"))
                    .roomName((String) roomData.get("roomName"))
                    .gameId(Long.valueOf(roomData.get("gameId").toString()))
                    .teamId(Long.valueOf(roomData.get("teamId").toString()))
                    .teamName((String) roomData.get("teamName"))
                    .active((Boolean) roomData.get("active"))
                    .currentUsers((Integer) roomData.get("currentUsers"))
                    .maxUsers((Integer) roomData.get("maxUsers"))
                    .build();

        } catch (Exception e) {
            log.error("채팅방 Redis 로드 실패 - roomKey: {}", roomKey, e);
            return null;
        }
    }

    /**
     * 실시간 데이터로 채팅방 정보 생성 (데이터 일관성 보장)
     */
    private TeamChatRoomInfo buildTeamChatRoomWithRealtimeData(String roomKey, String teamId) {
        try {
            // 항상 실시간 접속자 수를 조회하여 데이터 일관성 보장
            long currentUsers = gameChatUserService.getConnectedUserCount(teamId);
            
            log.debug("채팅방 실시간 정보 생성 - teamId: {}, currentUsers: {}", teamId, currentUsers);

            if (!redisTemplate.hasKey(roomKey)) {
                // 채팅방이 Redis에 없으면 기본 정보로 생성
                log.info("Redis에 채팅방 정보 없음 - 기본 정보로 생성: teamId={}", teamId);
                return createTeamChatRoomWithRealtimeUsers(teamId, currentUsers);
            }

            Map<Object, Object> roomData = redisTemplate.opsForHash().entries(roomKey);

            return TeamChatRoomInfo.builder()
                    .roomId((String) roomData.get("roomId"))
                    .roomName((String) roomData.get("roomName"))
                    .gameId(Long.valueOf(roomData.get("gameId").toString()))
                    .teamId(Long.valueOf(roomData.get("teamId").toString()))
                    .teamName((String) roomData.get("teamName"))
                    .gameDateTime(LocalDateTime.now().plusHours(2)) // 실시간 게임 시간
                    .stadium("잠실야구장") // 실제로는 DB에서 조회
                    .active((Boolean) roomData.get("active"))
                    .currentUsers((int) currentUsers) // 실시간 접속자 수 반영
                    .maxUsers((Integer) roomData.get("maxUsers"))
                    .doubleHeader(false)
                    .winningFairyCount(0)
                    .teamType(teamId.equals("1") ? "HOME" : "AWAY")
                    .build();

        } catch (Exception e) {
            log.error("채팅방 실시간 데이터 생성 실패 - roomKey: {}, teamId: {}", roomKey, teamId, e);
            // 오류 시 기본 정보라도 반환 (실시간 접속자 수 0으로)
            return createTeamChatRoomWithRealtimeUsers(teamId, 0);
        }
    }

    /**
     * Redis에서 채팅방 정보 로드 + 실시간 데이터 반영 (하위 호환성 유지)
     */
    private TeamChatRoomInfo loadChatRoomFromRedisWithRealtimeData(String roomKey, String teamId) {
        return buildTeamChatRoomWithRealtimeData(roomKey, teamId);
    }

    /**
     * 실시간 접속자 수로 채팅방 정보 생성 (데이터 일관성 보장)
     */
    private TeamChatRoomInfo createTeamChatRoomWithRealtimeUsers(String teamId, long currentUsers) {
        try {
            String[] teamInfo = findTeamInfoById(teamId);

            return TeamChatRoomInfo.builder()
                    .roomId("game_123_team_" + teamId)
                    .roomName(teamInfo[1] + " 응원방")
                    .gameId(123L)
                    .teamId(Long.valueOf(teamInfo[0]))
                    .teamName(teamInfo[1])
                    .gameDateTime(LocalDateTime.now().plusHours(2))
                    .stadium("잠실야구장")
                    .active(true)
                    .currentUsers((int) currentUsers) // 매개변수로 받은 실시간 접속자 수
                    .maxUsers(200)
                    .doubleHeader(false)
                    .winningFairyCount(0)
                    .teamType(teamId.equals("1") ? "HOME" : "AWAY")
                    .build();
        } catch (Exception e) {
            log.error("실시간 채팅방 정보 생성 실패 - teamId: {}, currentUsers: {}", teamId, currentUsers, e);
            return null;
        }
    }

    /**
     * 기본 채팅방 정보 생성 (Redis에 정보가 없을 때) - 하위 호환성 유지
     */
    private TeamChatRoomInfo createDefaultTeamChatRoomInfo(String teamId) {
        long currentUsers = gameChatUserService.getConnectedUserCount(teamId);
        return createTeamChatRoomWithRealtimeUsers(teamId, currentUsers);
    }

    /**
     * 팀 ID로 팀 정보 찾기
     */
    private String[] findTeamInfoById(String teamId) {
        for (String[] team : TEAMS) {
            if (team[0].equals(teamId)) {
                return team;
            }
        }
        // 기본값 반환
        return new String[]{teamId, "Unknown Team", "unknown"};
    }
}