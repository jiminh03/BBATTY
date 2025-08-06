package com.ssafy.chat.match.controller;

import com.ssafy.chat.global.response.ApiResponse;
import com.ssafy.chat.match.dto.GameInfo;
import com.ssafy.chat.match.service.GameInfoService;
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
    public ApiResponse<String> generateSampleGames(@RequestParam(defaultValue = "14") int days) {
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
                    gameInfo.setGameId((long) (day * 10 + game + 1));
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
            }
            
            String message = String.format("%d일간 총 %d경기 데이터를 생성했습니다.", days, totalGames);
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
            @RequestParam(defaultValue = "잠실야구장") String stadium) {
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
            
            String message = String.format("%s vs %s 경기를 %s %s에 생성했습니다.", awayTeam, homeTeam, date, time);
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
}