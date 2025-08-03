package com.ssafy.schedule.service;

import com.ssafy.schedule.common.GameStatus;
import com.ssafy.schedule.entity.Game;
import com.ssafy.schedule.entity.Team;
import com.ssafy.schedule.repository.GameRepository;
import com.ssafy.schedule.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledGameService {

    private final TeamRepository teamRepository;
    private final GameRepository gameRepository;

    private static final String BASE_URL = "https://m.sports.naver.com";
    private static final String SCHEDULE_URL = BASE_URL + "/kbaseball/schedule/index?date=";
    
    private static final Map<String, String> TEAM_NAME_MAPPING = Map.of(
        "한화", "한화 이글스",
        "LG", "LG 트윈스", 
        "롯데", "롯데 자이언츠",
        "KT", "KT 위즈",
        "삼성", "삼성 라이온즈",
        "KIA", "KIA 타이거즈",
        "SSG", "SSG 랜더스",
        "NC", "NC 다이노스",
        "두산", "두산 베어스",
        "키움", "키움 히어로즈"
    );

    /**
     * 특정 날짜의 야구 경기 일정(점수 없는 경기)을 크롤링하여 저장
     */
    public int crawlAndSaveScheduledGames(String date) {
        log.info("{} 야구 경기 일정 크롤링 시작", date);

        String html = getRenderedHtmlByUrl(SCHEDULE_URL + date);
        if (html == null) {
            log.error("HTML 렌더링 실패");
            return 0;
        }

        Document doc = Jsoup.parse(html);
        Elements links = doc.select("a.MatchBox_link_match_end__3HGjy");

        log.info("경기 링크 개수: {}", links.size());

        List<String> gameUrls = new ArrayList<>();
        for (Element link : links) {
            String href = link.attr("href");

            if (href != null && href.startsWith("/game/")) {
                String gameCode = href.replace("/game/", "");
                if (gameCode.endsWith("2025")) {
                    String fullUrl = BASE_URL + href;
                    gameUrls.add(fullUrl);
                    log.info("경기 링크 발견: {}", fullUrl);
                }
            }
        }

        log.info("총 {}개의 경기를 찾았습니다.", gameUrls.size());

        int savedCount = 0;
        for (String gameUrl : gameUrls) {
            if (crawlAndSaveScheduledGameDetail(gameUrl, date)) {
                savedCount++;
            }
        }
        
        log.info("{}개의 경기 일정 저장 완료", savedCount);
        return savedCount;
    }

    @Transactional
    public boolean crawlAndSaveScheduledGameDetail(String gameUrl, String dateStr) {
        log.info("경기 상세 정보 크롤링: {}", gameUrl);

        String html = getRenderedHtmlByUrl(gameUrl);
        if (html == null) {
            log.error("HTML 렌더링 실패 for {}", gameUrl);
            return false;
        }

        Document doc = Jsoup.parse(html);

        Elements teamElements = doc.select("em.MatchBox_name__11AyG");
        String homeTeamName = null;
        String awayTeamName = null;

        for (Element em : teamElements) {
            if (em.selectFirst("i") != null && em.text().contains("홈")) {
                homeTeamName = em.ownText();
            } else {
                awayTeamName = em.text();
            }
        }

        Element timeElement = doc.selectFirst("span.MatchBox_time__2z_nB");
        String gameTime = timeElement != null ? timeElement.text() : null;

        Elements scoreElements = doc.select("strong.MatchBox_score__33SVc");
        
        if (scoreElements.size() >= 2) {
            try {
                Integer awayScore = Integer.parseInt(scoreElements.get(0).ownText());
                Integer homeScore = Integer.parseInt(scoreElements.get(1).ownText());
                
                log.info("점수가 있는 경기 - 일정 저장 건너뛰기: {} vs {} ({}:{})", 
                        awayTeamName, homeTeamName, awayScore, homeScore);
                return false;
                
            } catch (NumberFormatException e) {
                log.warn("점수 파싱 실패, 일정으로 처리: {}", e.getMessage());
            }
        }

        log.info("예정된 경기 발견: 시간: {}, 홈팀: {}, 원정팀: {}", gameTime, homeTeamName, awayTeamName);

        if (homeTeamName != null && awayTeamName != null && gameTime != null) {
            return saveScheduledGame(homeTeamName, awayTeamName, gameTime, dateStr);
        } else {
            log.warn("필수 정보 누락 - 홈팀: {}, 원정팀: {}, 시간: {}", homeTeamName, awayTeamName, gameTime);
            return false;
        }
    }

    private boolean saveScheduledGame(String homeTeamName, String awayTeamName, String gameTime, String dateStr) {
        try {
            String mappedHomeTeamName = mapTeamName(homeTeamName);
            String mappedAwayTeamName = mapTeamName(awayTeamName);
            
            Team homeTeam = getTeamByName(mappedHomeTeamName);
            Team awayTeam = getTeamByName(mappedAwayTeamName);
            
            if (homeTeam == null || awayTeam == null) {
                log.warn("팀을 찾을 수 없음 - 홈팀: {} ({}), 원정팀: {} ({})", 
                        homeTeamName, mappedHomeTeamName, awayTeamName, mappedAwayTeamName);
                return false;
            }

            LocalDateTime gameDateTime = parseGameDateTime(dateStr, gameTime);

            if (gameRepository.existsByHomeTeamAndAwayTeamAndDate(homeTeam, awayTeam, gameDateTime)) {
                log.info("이미 존재하는 경기 일정 - 건너뛰기: {} vs {} on {}", 
                        mappedHomeTeamName, mappedAwayTeamName, dateStr);
                return false;
            }

            Game game = new Game(awayTeam, homeTeam, gameDateTime);
            game.setStatus(GameStatus.SCHEDULED);
            gameRepository.save(game);

            log.info("✅ 경기 일정 저장 완료: {} vs {} at {}", 
                    mappedAwayTeamName, mappedHomeTeamName, gameDateTime);
            return true;

        } catch (Exception e) {
            log.error("경기 일정 저장 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    private String getRenderedHtmlByUrl(String url) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = null;

        try {
            driver = new ChromeDriver(options);
            driver.get(url);
            Thread.sleep(3000);
            return driver.getPageSource();
        } catch (Exception e) {
            log.error("Selenium 렌더링 실패: {}", e.getMessage(), e);
            return null;
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private String mapTeamName(String crawledTeamName) {
        return TEAM_NAME_MAPPING.getOrDefault(crawledTeamName, crawledTeamName);
    }

    private Team getTeamByName(String teamName) {
        return teamRepository.findByName(teamName).orElse(null);
    }

    private LocalDateTime parseGameDateTime(String dateStr, String timeStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            return LocalDateTime.of(date, time);
        } catch (Exception e) {
            log.error("날짜/시간 파싱 실패: {} {}, 기본값(14:00) 사용", dateStr, timeStr);
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return LocalDateTime.of(date, LocalTime.of(14, 0));
        }
    }
}