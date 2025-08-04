package com.ssafy.schedule.service;

import com.ssafy.schedule.common.GameStatus;
import com.ssafy.schedule.entity.Game;
import com.ssafy.schedule.entity.Team;
import com.ssafy.schedule.repository.GameRepository;
import com.ssafy.schedule.repository.TeamRepository;
import com.ssafy.schedule.service.base.BaseCrawlerService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예정된 경기 일정 크롤링 서비스
 * - 아직 진행되지 않은 경기들을 SCHEDULED 상태로 저장
 * - 점수가 표시되지 않은 경기만 처리
 */
@Service
@Slf4j
public class ScheduledGameService extends BaseCrawlerService {

    private final GameRepository gameRepository;
    private final GameEventScheduler gameEventScheduler;

    public ScheduledGameService(TeamRepository teamRepository, GameRepository gameRepository, GameEventScheduler gameEventScheduler) {
        super(teamRepository);
        this.gameRepository = gameRepository;
        this.gameEventScheduler = gameEventScheduler;
    }

    /**
     * 특정 날짜의 예정된 경기 일정을 크롤링하여 저장
     * 
     * @param date YYYY-MM-DD 형식의 날짜
     * @return 저장된 경기 일정 수
     */
    public int crawlAndSaveScheduledGames(String date) {
        log.info("{} 예정된 경기 일정 크롤링 시작", date);


        List<String> gameUrls = crawlGameUrls(date);
        if (gameUrls.isEmpty()) {
            log.warn("크롤링된 경기 링크가 없습니다.");
            return 0;
        }

        int savedCount = 0;
        for (String gameUrl : gameUrls) {
            if (processScheduledGame(gameUrl, date)) {
                savedCount++;
            }
        }
        
        log.info("{}개의 예정된 경기 일정 저장 완료", savedCount);
        return savedCount;
    }

    /**
     * 개별 경기 페이지를 분석하여 예정된 경기인지 확인 후 저장
     * 
     * @param gameUrl 경기 상세 페이지 URL
     * @param dateStr 경기 날짜 (YYYY-MM-DD)
     * @return 저장 성공 여부
     */
    @Transactional
    public boolean processScheduledGame(String gameUrl, String dateStr) {
        log.debug("예정된 경기 처리: {}", gameUrl);

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

        // 점수 확인 - 점수가 있으면 이미 진행된 경기이므로 건너뛰기
        Integer[] scores = extractScores(doc);
        if (scores != null) {
            log.debug("점수가 있는 경기 - 예정된 경기 저장 건너뛰기: {} vs {} ({}:{})", 
                    awayTeamName, homeTeamName, scores[0], scores[1]);
            return false;
        }

        // 필수 정보 검증
        if (homeTeamName == null || awayTeamName == null || gameTime == null) {
            log.warn("필수 정보 누락 - 홈팀: {}, 원정팀: {}, 시간: {}", homeTeamName, awayTeamName, gameTime);
            return false;
        }

        log.debug("예정된 경기 발견: {} vs {} at {}", awayTeamName, homeTeamName, gameTime);
        return saveScheduledGame(homeTeamName, awayTeamName, gameTime, dateStr);
    }

    /**
     * 예정된 경기 일정을 데이터베이스에 저장
     * 
     * @param homeTeamName 홈팀명 (크롤링된 이름)
     * @param awayTeamName 원정팀명 (크롤링된 이름)
     * @param gameTime 경기 시간 (HH:mm)
     * @param dateStr 경기 날짜 (YYYY-MM-DD)
     * @return 저장 성공 여부
     */
    private boolean saveScheduledGame(String homeTeamName, String awayTeamName, String gameTime, String dateStr) {
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
                log.debug("이미 존재하는 경기 일정 - 건너뛰기: {} vs {} on {}", 
                        mappedAwayTeamName, mappedHomeTeamName, dateStr);
                return false;
            }

            // 경기 일정 저장 (SCHEDULED 상태)
            Game game = new Game(awayTeam, homeTeam, gameDateTime);
            game.setStatus(GameStatus.SCHEDULED);
            Game savedGame = gameRepository.save(game);

            // 경기 시작 2시간 전 이벤트 스케줄 등록
            gameEventScheduler.scheduleGameStartingEvent(savedGame);

            log.info("✅ 예정된 경기 일정 저장: {} vs {} at {}", 
                    mappedAwayTeamName, mappedHomeTeamName, gameDateTime);
            return true;

        } catch (Exception e) {
            log.error("경기 일정 저장 실패: {}", e.getMessage(), e);
            return false;
        }
    }
}