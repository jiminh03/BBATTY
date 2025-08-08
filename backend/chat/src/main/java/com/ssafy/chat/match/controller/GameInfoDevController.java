package com.ssafy.chat.match.controller;

import com.ssafy.chat.global.response.ApiResponse;
import com.ssafy.chat.match.dto.GameInfo;
import com.ssafy.chat.match.dto.MatchChatRoomCreateRequest;
import com.ssafy.chat.match.service.GameInfoService;
import com.ssafy.chat.match.service.MatchChatRoomService;
import com.ssafy.chat.watch.service.WatchChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/dev/games")
@RequiredArgsConstructor
@Slf4j
public class GameInfoDevController {
    
    private final GameInfoService gameInfoService;
    private final MatchChatRoomService matchChatRoomService;
    private final WatchChatService watchChatService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final List<String> teams = Arrays.asList(
        "삼성 라이온즈", "LG 트윈스", "두산 베어스", "KT 위즈", "SSG 랜더스",
        "롯데 자이언츠", "한화 이글스", "키움 히어로즈", "NC 다이노스", "KIA 타이거즈"
    );
    
    private final List<String> stadiums = Arrays.asList(
        "잠실야구장", "수원KT위즈파크", "인천SSG랜더스필드", "대구삼성라이온즈파크",
        "창원NC파크", "광주기아챔피언스필드", "부산사직야구장", "대전한화생명이글스파크", "고척스카이돔"
    );

    @PostMapping("/generate")
    public ApiResponse<String> generateSampleGames(
            @RequestParam(defaultValue = "14") int days,
            @RequestParam(defaultValue = "true") boolean createMatchRooms,
            @RequestParam(defaultValue = "true") boolean createWatchRooms) {
        try {
            LocalDate startDate = LocalDate.now();
            Random random = new Random();
            int totalGames = 0;
            
            for (int day = 0; day < days; day++) {
                LocalDate currentDate = startDate.plusDays(day);
                String dateStr = currentDate.format(DATE_FORMATTER);
                
                // 하루에 2-5경기 생성
                int gamesPerDay = random.nextInt(4) + 2;
                List<GameInfo> dailyGames = new ArrayList<>();
                
                for (int game = 0; game < gamesPerDay; game++) {
                    // 랜덤 팀 선택 (중복 방지)
                    List<String> availableTeams = new ArrayList<>(teams);
                    String homeTeam = availableTeams.get(random.nextInt(availableTeams.size()));
                    availableTeams.remove(homeTeam);
                    String awayTeam = availableTeams.get(random.nextInt(availableTeams.size()));
                    
                    // 경기 시간 (14:00, 17:00, 18:30, 19:30 중 랜덤)
                    int[] hours = {14, 17, 18, 19};
                    int[] minutes = {0, 0, 30, 30};
                    int timeIndex = random.nextInt(hours.length);
                    
                    LocalDateTime gameTime = currentDate.atTime(hours[timeIndex], minutes[timeIndex]);
                    
                    // 경기 상태 (과거/현재/미래에 따라)
                    String status;
                    if (currentDate.isBefore(LocalDate.now())) {
                        status = random.nextBoolean() ? "FINISHED" : "CANCELLED";
                    } else if (currentDate.equals(LocalDate.now())) {
                        status = random.nextBoolean() ? "LIVE" : "SCHEDULED";
                    } else {
                        status = "SCHEDULED";
                    }
                    
                    GameInfo gameInfo = new GameInfo();
                    gameInfo.setGameId((long) (day * 100 + game + 1)); // ID 중복 방지를 위해 범위 확장
                    gameInfo.setHomeTeamId((long) (teams.indexOf(homeTeam) + 1));
                    gameInfo.setAwayTeamId((long) (teams.indexOf(awayTeam) + 1));
                    gameInfo.setHomeTeamName(homeTeam);
                    gameInfo.setAwayTeamName(awayTeam);
                    gameInfo.setDateTime(gameTime);
                    gameInfo.setStadium(stadiums.get(random.nextInt(stadiums.size())));
                    gameInfo.setStatus(status);
                    
                    dailyGames.add(gameInfo);
                    totalGames++;
                }
                
                gameInfoService.saveGameInfoList(dailyGames);
                log.info("Generated {} games for date: {}", dailyGames.size(), dateStr);
                
                // 매칭 채팅방도 함께 생성
                if (createMatchRooms) {
                    createMatchChatRoomsForGames(dailyGames);
                }
                
                // 직관 채팅방도 함께 생성 (각 팀당 하나씩)
                if (createWatchRooms) {
                    createWatchChatRoomsForGames(dailyGames);
                }
            }
            
            StringBuilder roomInfo = new StringBuilder();
            if (createMatchRooms) roomInfo.append("매칭 채팅방");
            if (createWatchRooms) {
                if (roomInfo.length() > 0) roomInfo.append(", ");
                roomInfo.append("직관 채팅방");
            }
            String roomInfoStr = roomInfo.length() > 0 ? " (" + roomInfo + " 포함)" : "";
            String message = String.format("%d일간 총 %d경기 데이터를 생성했습니다.%s", days, totalGames, roomInfoStr);
            return ApiResponse.success(message);
            
        } catch (Exception e) {
            log.error("Failed to generate sample games", e);
            return ApiResponse.fail(com.ssafy.chat.global.constants.ErrorCode.SERVER_ERROR, null);
        }
    }
    
    @PostMapping("/single")
    public ApiResponse<String> generateSingleGame(
            @RequestParam String date,
            @RequestParam String homeTeam,
            @RequestParam String awayTeam,
            @RequestParam(defaultValue = "19:30") String time,
            @RequestParam(defaultValue = "잠실야구장") String stadium,
            @RequestParam(defaultValue = "true") boolean createMatchRooms,
            @RequestParam(defaultValue = "true") boolean createWatchRooms) {
        try {
            LocalDate gameDate = LocalDate.parse(date, DATE_FORMATTER);
            String[] timeParts = time.split(":");
            LocalDateTime gameTime = gameDate.atTime(Integer.parseInt(timeParts[0]), Integer.parseInt(timeParts[1]));
            
            GameInfo gameInfo = new GameInfo();
            gameInfo.setGameId(System.currentTimeMillis()); // 고유 ID로 timestamp 사용
            gameInfo.setHomeTeamId((long) (teams.indexOf(homeTeam) + 1));
            gameInfo.setAwayTeamId((long) (teams.indexOf(awayTeam) + 1));
            gameInfo.setHomeTeamName(homeTeam);
            gameInfo.setAwayTeamName(awayTeam);
            gameInfo.setDateTime(gameTime);
            gameInfo.setStadium(stadium);
            gameInfo.setStatus("SCHEDULED");
            
            gameInfoService.saveGameInfoList(List.of(gameInfo));
            
            // 매칭 채팅방도 함께 생성
            if (createMatchRooms) {
                createMatchChatRoomsForGames(List.of(gameInfo));
            }
            
            // 직관 채팅방도 함께 생성
            if (createWatchRooms) {
                createWatchChatRoomsForGames(List.of(gameInfo));
            }
            
            StringBuilder roomInfo = new StringBuilder();
            if (createMatchRooms) roomInfo.append("매칭 채팅방");
            if (createWatchRooms) {
                if (roomInfo.length() > 0) roomInfo.append(", ");
                roomInfo.append("직관 채팅방");
            }
            String roomInfoStr = roomInfo.length() > 0 ? " (" + roomInfo + " 포함)" : "";
            String message = String.format("%s vs %s 경기를 %s %s에 생성했습니다.%s", awayTeam, homeTeam, date, time, roomInfoStr);
            return ApiResponse.success(message);
            
        } catch (Exception e) {
            log.error("Failed to generate single game", e);
            return ApiResponse.fail(com.ssafy.chat.global.constants.ErrorCode.SERVER_ERROR, null);
        }
    }
    
    @GetMapping("/teams")
    public ApiResponse<List<String>> getAvailableTeams() {
        return ApiResponse.success(teams);
    }
    
    @GetMapping("/stadiums")
    public ApiResponse<List<String>> getAvailableStadiums() {
        return ApiResponse.success(stadiums);
    }
    
    /**
     * 게임에 대한 매칭 채팅방들 생성
     */
    private void createMatchChatRoomsForGames(List<GameInfo> games) {
        Random random = new Random();
        String[] genderConditions = {"ALL", "MALE", "FEMALE"};
        String[] roomTitles = {
            "열정 응원단 모집!",
            "같이 응원해요~",
            "승리를 위해!",
            "팬 여러분 모여라!",
            "응원 한마음!",
            "우리팀 화이팅!"
        };
        
        for (GameInfo game : games) {
            try {
                // 홈팀, 원정팀 각각 1-3개씩 매칭방 생성
                createMatchRoomsForTeam(game, game.getHomeTeamId(), game.getHomeTeamName(), random, genderConditions, roomTitles);
                createMatchRoomsForTeam(game, game.getAwayTeamId(), game.getAwayTeamName(), random, genderConditions, roomTitles);
                
            } catch (Exception e) {
                log.warn("매칭 채팅방 생성 실패 - gameId: {}", game.getGameId(), e);
            }
        }
    }
    
    private void createMatchRoomsForTeam(GameInfo game, Long teamId, String teamName, Random random, 
                                       String[] genderConditions, String[] roomTitles) {
        int roomCount = random.nextInt(3) + 1; // 1-3개
        
        for (int i = 0; i < roomCount; i++) {
            MatchChatRoomCreateRequest request = MatchChatRoomCreateRequest.builder()
                    .gameId(game.getGameId())
                    .matchTitle(teamName + " " + roomTitles[random.nextInt(roomTitles.length)])
                    .matchDescription("함께 응원해요!")
                    .teamId(teamId)
                    .minAge(20)
                    .maxAge(random.nextInt(30) + 40) // 40-70세
                    .genderCondition(genderConditions[random.nextInt(genderConditions.length)])
                    .maxParticipants(random.nextInt(12) + 4) // 4-16명
                    .build();
            
            try {
                matchChatRoomService.createMatchChatRoom(request, "dummy-jwt-token");
                log.debug("매칭 채팅방 생성 - gameId: {}, teamId: {}, title: {}", 
                         game.getGameId(), teamId, request.getMatchTitle());
            } catch (Exception e) {
                log.warn("개별 매칭 채팅방 생성 실패 - gameId: {}, teamId: {}", game.getGameId(), teamId, e);
            }
        }
    }
    
    /**
     * 게임에 대한 직관 채팅방들 생성 (각 팀당 하나씩)
     */
    private void createWatchChatRoomsForGames(List<GameInfo> games) {
        for (GameInfo game : games) {
            try {
                // 홈팀 직관 채팅방 생성
                createWatchChatRoom(game, game.getHomeTeamId(), game.getHomeTeamName());
                
                // 원정팀 직관 채팅방 생성
                createWatchChatRoom(game, game.getAwayTeamId(), game.getAwayTeamName());
                
            } catch (Exception e) {
                log.warn("직관 채팅방 생성 실패 - gameId: {}", game.getGameId(), e);
            }
        }
    }
    
    /**
     * 특정 팀의 직관 채팅방 생성
     */
    private void createWatchChatRoom(GameInfo game, Long teamId, String teamName) {
        try {
            // 직관 채팅방은 Redis에 바로 저장 (roomId 형태: watch-gameId-teamId)
            String roomId = String.format("watch-%d-%d", game.getGameId(), teamId);

            watchChatService.createWatchChatRoom(roomId, game.getGameId(), teamId, teamName);
            
            log.debug("직관 채팅방 생성 - gameId: {}, teamId: {}, teamName: {}", 
                     game.getGameId(), teamId, teamName);
                     
        } catch (Exception e) {
            log.warn("개별 직관 채팅방 생성 실패 - gameId: {}, teamId: {}", game.getGameId(), teamId, e);
        }
    }
}