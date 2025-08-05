package com.ssafy.schedule.service;

import com.ssafy.schedule.common.GameResult;
import com.ssafy.schedule.common.GameStatus;
import com.ssafy.schedule.entity.Game;
import com.ssafy.schedule.entity.Team;
import com.ssafy.schedule.repository.GameRepository;
import com.ssafy.schedule.repository.TeamRepository;
import com.ssafy.schedule.service.base.BaseCrawlerService;
import com.ssafy.schedule.util.GameResultCalculator;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 진행된 경기 결과 크롤링 서비스
 * - 기존 예정된 경기를 FINISHED 상태로 업데이트
 * - 과거 경기 데이터를 직접 저장 (데이터 축적용)
 * - 점수가 표시된 경기만 처리
 */
@Service
@Slf4j
public class FinishedGameService extends BaseCrawlerService {

    private final GameRepository gameRepository;

    public FinishedGameService(TeamRepository teamRepository,
                              GameRepository gameRepository) {
        super(teamRepository);
        this.gameRepository = gameRepository;
    }

    /**
     * 특정 날짜의 진행된 경기 결과를 크롤링하여 기존 일정에 업데이트
     * 
     * @param date YYYY-MM-DD 형식의 날짜
     * @return 업데이트된 경기 수
     */
    public int crawlAndUpdateFinishedGames(String date) {
        log.info("{} 진행된 경기 결과 업데이트 시작", date);

        List<String> gameUrls = crawlGameUrls(date);
        if (gameUrls.isEmpty()) {
            log.warn("크롤링된 경기 링크가 없습니다.");
            return 0;
        }

        int updatedCount = 0;
        for (String gameUrl : gameUrls) {
            if (processFinishedGameForUpdate(gameUrl, date)) {
                updatedCount++;
            }
        }
        
        log.info("{}개의 경기 결과 업데이트 완료", updatedCount);
        return updatedCount;
    }

    /**
     * 특정 날짜의 진행된 경기 결과를 크롤링하여 직접 저장 (데이터 축적용)
     * 
     * @param date YYYY-MM-DD 형식의 날짜
     * @return 저장된 경기 수
     */
    public int crawlAndSaveFinishedGames(String date) {
        log.info("{} 진행된 경기 직접 저장 시작", date);

        List<String> gameUrls = crawlGameUrls(date);
        if (gameUrls.isEmpty()) {
            log.warn("크롤링된 경기 링크가 없습니다.");
            return 0;
        }

        int savedCount = 0;
        for (String gameUrl : gameUrls) {
            if (processFinishedGameForSave(gameUrl, date)) {
                savedCount++;
            }
        }
        
        log.info("{}개의 진행된 경기 직접 저장 완료", savedCount);
        return savedCount;
    }

    /**
     * 경기 데이터를 담는 내부 클래스
     */
    private static class GameData {
        final String homeTeamName;
        final String awayTeamName; 
        final String gameTime;
        final String stadiumLocation;
        final Integer homeScore;
        final Integer awayScore;
        final boolean isCancelled;

        GameData(String homeTeamName, String awayTeamName, String gameTime, 
                String stadiumLocation, Integer homeScore, Integer awayScore, boolean isCancelled) {
            this.homeTeamName = homeTeamName;
            this.awayTeamName = awayTeamName;
            this.gameTime = gameTime;
            this.stadiumLocation = stadiumLocation;
            this.homeScore = homeScore;
            this.awayScore = awayScore;
            this.isCancelled = isCancelled;
        }
    }

    /**
     * 경기 페이지에서 공통 데이터 추출
     * 
     * @param gameUrl 경기 상세 페이지 URL
     * @return GameData 객체 (실패시 null)
     */
    private GameData extractGameData(String gameUrl) {
        String html = getRenderedHtmlByUrl(gameUrl);
        if (html == null) {
            log.error("HTML 렌더링 실패: {}", gameUrl);
            return null;
        }

        Document doc = Jsoup.parse(html);

        // 팀명 추출
        String[] teamNames = extractTeamNames(doc);
        String awayTeamName = teamNames[0];
        String homeTeamName = teamNames[1];

        // 경기 시간 추출
        String gameTime = extractGameTime(doc);

        // 경기장 지역명 추출
        String stadiumLocation = extractStadiumLocation(doc);

        // 점수 확인
        Integer[] scores = extractScores(doc);
        boolean isCancelled = false;
        
        if (scores == null) {
            // 점수가 없을 때 취소 상태 확인
            isCancelled = isGameCancelled(doc);
            if (!isCancelled) {
                log.debug("점수가 없고 취소되지도 않은 경기 - 건너뛰기: {} vs {}", awayTeamName, homeTeamName);
                return null;
            }
            log.debug("취소된 경기 발견: {} vs {}", awayTeamName, homeTeamName);
            scores = new Integer[]{0, 0}; // 취소된 경기는 점수를 0으로 설정
        }

        // 필수 정보 검증
        if (homeTeamName == null || awayTeamName == null || gameTime == null) {
            log.warn("필수 정보 누락 - 홈팀: {}, 원정팀: {}, 시간: {}", homeTeamName, awayTeamName, gameTime);
            return null;
        }

        return new GameData(homeTeamName, awayTeamName, gameTime, stadiumLocation, scores[1], scores[0], isCancelled);
    }

    /**
     * 개별 경기 페이지를 분석하여 진행된 경기 결과를 기존 일정에 업데이트
     * 
     * @param gameUrl 경기 상세 페이지 URL
     * @param dateStr 경기 날짜 (YYYY-MM-DD)
     * @return 업데이트 성공 여부
     */
    @Transactional
    public boolean processFinishedGameForUpdate(String gameUrl, String dateStr) {
        log.debug("진행된 경기 업데이트 처리: {}", gameUrl);

        GameData gameData = extractGameData(gameUrl);
        if (gameData == null) {
            return false;
        }

        log.debug("진행된 경기 발견: {} vs {} ({}:{}) (경기장: {})", 
                gameData.awayTeamName, gameData.homeTeamName, 
                gameData.awayScore, gameData.homeScore, gameData.stadiumLocation);
        
        return updateExistingGame(gameData, dateStr);
    }

    /**
     * 개별 경기 페이지를 분석하여 진행된 경기 결과를 직접 저장
     * 
     * @param gameUrl 경기 상세 페이지 URL
     * @param dateStr 경기 날짜 (YYYY-MM-DD)
     * @return 저장 성공 여부
     */
    @Transactional
    public boolean processFinishedGameForSave(String gameUrl, String dateStr) {
        log.debug("진행된 경기 직접 저장 처리: {}", gameUrl);

        GameData gameData = extractGameData(gameUrl);
        if (gameData == null) {
            return false;
        }

        log.debug("진행된 경기 발견: {} vs {} ({}:{}) (경기장: {})", 
                gameData.awayTeamName, gameData.homeTeamName, 
                gameData.awayScore, gameData.homeScore, gameData.stadiumLocation);
        
        return saveFinishedGame(gameData, dateStr);
    }

    /**
     * 공통 팀 조회 로직
     */
    private Team[] getTeams(GameData gameData) {
        String mappedHomeTeamName = mapTeamName(gameData.homeTeamName);
        String mappedAwayTeamName = mapTeamName(gameData.awayTeamName);
        
        Team homeTeam = getTeamByName(mappedHomeTeamName);
        Team awayTeam = getTeamByName(mappedAwayTeamName);
        
        if (homeTeam == null || awayTeam == null) {
            log.warn("팀을 찾을 수 없음 - 홈팀: {} ({}), 원정팀: {} ({})", 
                    gameData.homeTeamName, mappedHomeTeamName, gameData.awayTeamName, mappedAwayTeamName);
            return null;
        }
        
        return new Team[]{awayTeam, homeTeam};
    }

    /**
     * 기존 예정된 경기를 진행된 결과로 업데이트
     * 
     * @param gameData 경기 데이터
     * @param dateStr 경기 날짜 (YYYY-MM-DD)
     * @return 업데이트 성공 여부
     */
    private boolean updateExistingGame(GameData gameData, String dateStr) {
        try {
            Team[] teams = getTeams(gameData);
            if (teams == null) {
                return false;
            }
            Team awayTeam = teams[0];
            Team homeTeam = teams[1];

            // 날짜/시간 파싱
            LocalDateTime gameDateTime = parseGameDateTime(dateStr, gameData.gameTime);

            // 기존 경기 일정 조회 (날짜 기준으로 매칭)
            Game existingGame = gameRepository.findByHomeTeamAndAwayTeamAndDate(homeTeam, awayTeam, gameDateTime)
                    .orElse(null);
                    
            if (existingGame == null) {
                log.warn("업데이트할 기존 경기 일정을 찾을 수 없음: {} vs {} on {}", 
                        gameData.awayTeamName, gameData.homeTeamName, dateStr);
                return false;
            }

            // 취소된 경기와 완료된 경기 구분 처리
            if (gameData.isCancelled) {
                existingGame.setStatus(GameStatus.CANCELLED);
                existingGame.setResult(GameResult.CANCELLED);
            } else {
                // 경기 결과 계산 및 업데이트
                GameResult gameResult = GameResultCalculator.calculateResult(gameData.homeScore, gameData.awayScore);
                existingGame.setStatus(GameStatus.FINISHED);
                existingGame.setResult(gameResult);
            }
            
            existingGame.setHomeScore(gameData.homeScore);
            existingGame.setAwayScore(gameData.awayScore);
            
            // 경기장 정보 업데이트 (기존 경기에 경기장 정보가 없을 경우)
            if (existingGame.getStadium() == null && gameData.stadiumLocation != null) {
                setStadiumInfo(existingGame, gameData.stadiumLocation);
            }
            
            gameRepository.save(existingGame);

            String resultText = gameData.isCancelled ? "CANCELLED" : 
                    GameResultCalculator.calculateResult(gameData.homeScore, gameData.awayScore).name();
            log.info("✅ 경기 결과 업데이트: {} vs {} ({}:{}) - {}", 
                    mapTeamName(gameData.awayTeamName), mapTeamName(gameData.homeTeamName), 
                    gameData.awayScore, gameData.homeScore, resultText);
            return true;

        } catch (Exception e) {
            log.error("경기 결과 업데이트 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 진행된 경기 결과를 새 경기로 직접 저장 (과거 데이터 축적용)
     * 
     * @param gameData 경기 데이터
     * @param dateStr 경기 날짜 (YYYY-MM-DD)
     * @return 저장 성공 여부
     */
    private boolean saveFinishedGame(GameData gameData, String dateStr) {
        try {
            Team[] teams = getTeams(gameData);
            if (teams == null) {
                return false;
            }
            Team awayTeam = teams[0];
            Team homeTeam = teams[1];

            // 날짜/시간 파싱
            LocalDateTime gameDateTime = parseGameDateTime(dateStr, gameData.gameTime);

            // 중복 확인 (같은 날짜에 같은 팀들의 경기가 이미 있는지)
            if (gameRepository.existsByHomeTeamAndAwayTeamAndDate(homeTeam, awayTeam, gameDateTime)) {
                log.debug("이미 존재하는 경기 - 건너뛰기: {} vs {} on {}", 
                        gameData.awayTeamName, gameData.homeTeamName, dateStr);
                return false;
            }

            // 취소된 경기와 완료된 경기 구분 처리
            Game game = new Game(awayTeam, homeTeam, gameDateTime);
            game.setHomeScore(gameData.homeScore);
            game.setAwayScore(gameData.awayScore);
            
            if (gameData.isCancelled) {
                game.setStatus(GameStatus.CANCELLED);
                game.setResult(GameResult.CANCELLED);
            } else {
                // 경기 결과 계산 및 저장
                GameResult gameResult = GameResultCalculator.calculateResult(gameData.homeScore, gameData.awayScore);
                game.setStatus(GameStatus.FINISHED);
                game.setResult(gameResult);
            }
            
            // 경기장 정보 설정
            setStadiumInfo(game, gameData.stadiumLocation);

            gameRepository.save(game);

            String resultText = gameData.isCancelled ? "CANCELLED" : 
                    GameResultCalculator.calculateResult(gameData.homeScore, gameData.awayScore).name();
            log.info("✅ 진행된 경기 직접 저장: {} vs {} ({}:{}) - {}", 
                    mapTeamName(gameData.awayTeamName), mapTeamName(gameData.homeTeamName), 
                    gameData.awayScore, gameData.homeScore, resultText);
            return true;

        } catch (Exception e) {
            log.error("진행된 경기 직접 저장 실패: {}", e.getMessage(), e);
            return false;
        }
    }
}