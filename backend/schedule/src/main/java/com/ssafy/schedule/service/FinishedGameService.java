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

    public FinishedGameService(TeamRepository teamRepository, GameRepository gameRepository) {
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
     * 개별 경기 페이지를 분석하여 진행된 경기 결과를 기존 일정에 업데이트
     * 
     * @param gameUrl 경기 상세 페이지 URL
     * @param dateStr 경기 날짜 (YYYY-MM-DD)
     * @return 업데이트 성공 여부
     */
    @Transactional
    public boolean processFinishedGameForUpdate(String gameUrl, String dateStr) {
        log.debug("진행된 경기 업데이트 처리: {}", gameUrl);

        String html = getRenderedHtmlByUrl(gameUrl);
        if (html == null) {
            log.error("HTML 렌더링 실패: {}", gameUrl);
            return false;
        }

        Document doc = Jsoup.parse(html);

        // 팀명 추출
        String[] teamNames = extractTeamNames(doc);
        String awayTeamName = teamNames[0];
        String homeTeamName = teamNames[1];

        // 경기 시간 추출
        String gameTime = extractGameTime(doc);

        // 점수 확인 - 점수가 없으면 아직 진행되지 않은 경기이므로 건너뛰기
        Integer[] scores = extractScores(doc);
        if (scores == null) {
            log.debug("점수가 없는 경기 - 결과 업데이트 건너뛰기: {} vs {}", awayTeamName, homeTeamName);
            return false;
        }

        // 필수 정보 검증
        if (homeTeamName == null || awayTeamName == null || gameTime == null) {
            log.warn("필수 정보 누락 - 홈팀: {}, 원정팀: {}, 시간: {}", homeTeamName, awayTeamName, gameTime);
            return false;
        }

        log.debug("진행된 경기 발견: {} vs {} ({}:{})", awayTeamName, homeTeamName, scores[0], scores[1]);
        return updateExistingGame(homeTeamName, awayTeamName, gameTime, dateStr, scores[1], scores[0]);
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

        String html = getRenderedHtmlByUrl(gameUrl);
        if (html == null) {
            log.error("HTML 렌더링 실패: {}", gameUrl);
            return false;
        }

        Document doc = Jsoup.parse(html);

        // 팀명 추출
        String[] teamNames = extractTeamNames(doc);
        String awayTeamName = teamNames[0];
        String homeTeamName = teamNames[1];

        // 경기 시간 추출
        String gameTime = extractGameTime(doc);

        // 점수 확인 - 점수가 없으면 아직 진행되지 않은 경기이므로 건너뛰기
        Integer[] scores = extractScores(doc);
        if (scores == null) {
            log.debug("점수가 없는 경기 - 결과 저장 건너뛰기: {} vs {}", awayTeamName, homeTeamName);
            return false;
        }

        // 필수 정보 검증
        if (homeTeamName == null || awayTeamName == null || gameTime == null) {
            log.warn("필수 정보 누락 - 홈팀: {}, 원정팀: {}, 시간: {}", homeTeamName, awayTeamName, gameTime);
            return false;
        }

        log.debug("진행된 경기 발견: {} vs {} ({}:{})", awayTeamName, homeTeamName, scores[0], scores[1]);
        return saveFinishedGame(homeTeamName, awayTeamName, gameTime, dateStr, scores[1], scores[0]);
    }

    /**
     * 기존 예정된 경기를 진행된 결과로 업데이트
     * 
     * @param homeTeamName 홈팀명 (크롤링된 이름)
     * @param awayTeamName 원정팀명 (크롤링된 이름)
     * @param gameTime 경기 시간 (HH:mm)
     * @param dateStr 경기 날짜 (YYYY-MM-DD)
     * @param homeScore 홈팀 점수
     * @param awayScore 원정팀 점수
     * @return 업데이트 성공 여부
     */
    private boolean updateExistingGame(String homeTeamName, String awayTeamName, String gameTime, 
                                     String dateStr, Integer homeScore, Integer awayScore) {
        try {
            // 팀명 매핑 및 조회
            String mappedHomeTeamName = mapTeamName(homeTeamName);
            String mappedAwayTeamName = mapTeamName(awayTeamName);
            
            Team homeTeam = getTeamByName(mappedHomeTeamName);
            Team awayTeam = getTeamByName(mappedAwayTeamName);
            
            if (homeTeam == null || awayTeam == null) {
                log.warn("팀을 찾을 수 없음 - 홈팀: {} ({}), 원정팀: {} ({})", 
                        homeTeamName, mappedHomeTeamName, awayTeamName, mappedAwayTeamName);
                return false;
            }

            // 날짜/시간 파싱
            LocalDateTime gameDateTime = parseGameDateTime(dateStr, gameTime);

            // 기존 경기 일정 조회 (날짜 기준으로 매칭)
            Game existingGame = gameRepository.findByHomeTeamAndAwayTeamAndDate(homeTeam, awayTeam, gameDateTime)
                    .orElse(null);
                    
            if (existingGame == null) {
                log.warn("업데이트할 기존 경기 일정을 찾을 수 없음: {} vs {} on {}", 
                        mappedAwayTeamName, mappedHomeTeamName, dateStr);
                return false;
            }

            // 경기 결과 계산 및 업데이트
            GameResult gameResult = GameResultCalculator.calculateResult(homeScore, awayScore);

            existingGame.setStatus(GameStatus.FINISHED);
            existingGame.setHomeScore(homeScore);
            existingGame.setAwayScore(awayScore);
            existingGame.setResult(gameResult);
            
            gameRepository.save(existingGame);

            log.info("✅ 경기 결과 업데이트: {} vs {} ({}:{}) - 결과: {}", 
                    mappedAwayTeamName, mappedHomeTeamName, awayScore, homeScore, gameResult);
            return true;

        } catch (Exception e) {
            log.error("경기 결과 업데이트 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 진행된 경기 결과를 새 경기로 직접 저장 (과거 데이터 축적용)
     * 
     * @param homeTeamName 홈팀명 (크롤링된 이름)
     * @param awayTeamName 원정팀명 (크롤링된 이름)
     * @param gameTime 경기 시간 (HH:mm)
     * @param dateStr 경기 날짜 (YYYY-MM-DD)
     * @param homeScore 홈팀 점수
     * @param awayScore 원정팀 점수
     * @return 저장 성공 여부
     */
    private boolean saveFinishedGame(String homeTeamName, String awayTeamName, String gameTime, 
                                   String dateStr, Integer homeScore, Integer awayScore) {
        try {
            // 팀명 매핑 및 조회
            String mappedHomeTeamName = mapTeamName(homeTeamName);
            String mappedAwayTeamName = mapTeamName(awayTeamName);
            
            Team homeTeam = getTeamByName(mappedHomeTeamName);
            Team awayTeam = getTeamByName(mappedAwayTeamName);
            
            if (homeTeam == null || awayTeam == null) {
                log.warn("팀을 찾을 수 없음 - 홈팀: {} ({}), 원정팀: {} ({})", 
                        homeTeamName, mappedHomeTeamName, awayTeamName, mappedAwayTeamName);
                return false;
            }

            // 날짜/시간 파싱
            LocalDateTime gameDateTime = parseGameDateTime(dateStr, gameTime);

            // 중복 확인 (같은 날짜에 같은 팀들의 경기가 이미 있는지)
            if (gameRepository.existsByHomeTeamAndAwayTeamAndDate(homeTeam, awayTeam, gameDateTime)) {
                log.debug("이미 존재하는 경기 - 건너뛰기: {} vs {} on {}", 
                        mappedAwayTeamName, mappedHomeTeamName, dateStr);
                return false;
            }

            // 경기 결과 계산 및 저장
            GameResult gameResult = GameResultCalculator.calculateResult(homeScore, awayScore);

            Game game = new Game(awayTeam, homeTeam, gameDateTime);
            game.setStatus(GameStatus.FINISHED);
            game.setHomeScore(homeScore);
            game.setAwayScore(awayScore);
            game.setResult(gameResult);
            
            gameRepository.save(game);

            log.info("✅ 진행된 경기 직접 저장: {} vs {} ({}:{}) - 결과: {}", 
                    mappedAwayTeamName, mappedHomeTeamName, awayScore, homeScore, gameResult);
            return true;

        } catch (Exception e) {
            log.error("진행된 경기 직접 저장 실패: {}", e.getMessage(), e);
            return false;
        }
    }
}