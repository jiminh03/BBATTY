package com.ssafy.schedule.service.base;

import com.ssafy.schedule.common.Stadium;
import com.ssafy.schedule.entity.Game;
import com.ssafy.schedule.entity.Team;
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
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 야구 경기 크롤링을 위한 공통 기능을 제공하는 추상 클래스
 * - 웹 크롤링 (Selenium)
 * - HTML 파싱 (Jsoup)
 * - 팀명 매핑
 * - 날짜/시간 파싱
 */
@Component
@Slf4j
@RequiredArgsConstructor
public abstract class BaseCrawlerService {

    protected final TeamRepository teamRepository;

    // 네이버 스포츠 KBO 일정 페이지 URL
    protected static final String BASE_URL = "https://m.sports.naver.com";
    protected static final String SCHEDULE_URL = BASE_URL + "/kbaseball/schedule/index?date=";
    
    /**
     * 크롤링된 팀명을 DB에 저장된 정확한 팀명으로 매핑하는 맵
     * 네이버 스포츠에서 사용하는 축약된 팀명 -> 데이터베이스의 정식 팀명
     */
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
     * 특정 날짜의 KBO 경기 링크들을 크롤링
     * 
     * @param date YYYY-MM-DD 형식의 날짜
     * @return 경기 상세 페이지 URL 목록
     */
    protected List<String> crawlGameUrls(String date) {
        log.info("{}일자 경기 링크 크롤링 시작", date);

        String html = getRenderedHtmlByUrl(SCHEDULE_URL + date);
        if (html == null) {
            log.error("HTML 렌더링 실패");
            return new ArrayList<>();
        }

        Document doc = Jsoup.parse(html);
        // 경기 박스 링크 선택자
        Elements links = doc.select("a.MatchBox_link_match_end__3HGjy");
        log.info("발견된 링크 개수: {}", links.size());

        List<String> gameUrls = new ArrayList<>();
        for (Element link : links) {
            String href = link.attr("href");
            
            // 경기 상세 페이지 링크만 필터링 (/game/으로 시작하고 2025로 끝남)
            if (href != null && href.startsWith("/game/")) {
                String gameCode = href.replace("/game/", "");
                if (gameCode.endsWith("2025")) {
                    String fullUrl = BASE_URL + href;
                    gameUrls.add(fullUrl);
                    log.debug("경기 링크 발견: {}", fullUrl);
                }
            }
        }

        log.info("총 {}개의 경기 링크 수집 완료", gameUrls.size());
        return gameUrls;
    }

    /**
     * 경기 상세 페이지에서 삽입할 데이터 추출
     * 
     * @param doc Jsoup Document 객체
     */
    protected String[] extractTeamNames(Document doc) {
        Elements teamElements = doc.select("em.MatchBox_name__11AyG");
        String homeTeamName = null;
        String awayTeamName = null;

        for (Element em : teamElements) {
            // 홈팀은 '홈' 아이콘이 있음
            if (em.selectFirst("i") != null && em.text().contains("홈")) {
                homeTeamName = em.ownText(); // 아이콘 텍스트 제외하고 팀명만
            } else {
                awayTeamName = em.text();
            }
        }

        return new String[]{awayTeamName, homeTeamName};
    }

    /**
     * 경기 시작 시간 추출
     * 
     * @param doc Jsoup Document 객체
     * @return 경기 시간 (HH:mm 형식, null일 수 있음)
     */
    protected String extractGameTime(Document doc) {
        Element timeElement = doc.selectFirst("span.MatchBox_time__2z_nB");
        return timeElement != null ? timeElement.text() : null;
    }

    /**
     * 경기 점수 추출
     * 
     * @param doc Jsoup Document 객체
     * @return [원정팀 점수, 홈팀 점수] 배열 (점수가 없으면 null)
     */
    protected Integer[] extractScores(Document doc) {
        Elements scoreElements = doc.select("strong.MatchBox_score__33SVc");
        
        if (scoreElements.size() < 2) {
            return null; // 점수가 표시되지 않음
        }

        try {
            Integer awayScore = Integer.parseInt(scoreElements.get(0).ownText());
            Integer homeScore = Integer.parseInt(scoreElements.get(1).ownText());
            return new Integer[]{awayScore, homeScore};
        } catch (NumberFormatException e) {
            log.warn("점수 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 경기장 지역명 추출
     * 
     * @param doc Jsoup Document 객체
     * @return 경기장 지역명 (예: "대전", "고척" 등, null일 수 있음)
     */
    protected String extractStadiumLocation(Document doc) {
        try {
            Element stadiumElement = doc.selectFirst(".MatchBox_stadium__17mQ4");
            if (stadiumElement != null) {
                String locationText = stadiumElement.text().trim();
                log.debug("HTML에서 추출한 경기장 지역: {}", locationText);
                return locationText;
            }
        } catch (Exception e) {
            log.error("HTML에서 경기장 지역 추출 중 오류 발생", e);
        }
        
        log.warn("HTML에서 경기장 지역 정보를 찾을 수 없습니다.");
        return null;
    }

    /**
     * 경기 취소 상태 확인
     * 
     * @param doc Jsoup Document 객체
     * @return 경기가 취소되었으면 true, 아니면 false
     */
    protected boolean isGameCancelled(Document doc) {
        try {
            Element stateElement = doc.selectFirst(".MatchBox_state__2AzL_");
            if (stateElement != null) {
                String stateText = stateElement.text().trim();
                log.info("HTML에서 추출한 경기 상태: {}", stateText);
                return "경기취소".equals(stateText);
            }
        } catch (Exception e) {
            log.info("HTML에서 경기 상태 추출 중 오류 발생", e);
        }
        
        return false;
    }

    /**
     * Game 엔티티에 경기장 정보 설정
     * 
     * @param game 경기 엔티티
     * @param stadiumLocation 경기장 지역명
     */
    protected void setStadiumInfo(Game game, String stadiumLocation) {
        if (stadiumLocation != null) {
            Stadium stadium = Stadium.findByLocation(stadiumLocation);
            if (stadium != null) {
                game.setStadium(stadium.getStadiumName());
                game.setLatitude(stadium.getLatitude());
                game.setLongitude(stadium.getLongitude());
                log.info("경기장 정보 설정 완료: {} ({})", stadium.getStadiumName(), stadiumLocation);
            } else {
                log.info("지역명 '{}'에 해당하는 경기장을 찾을 수 없습니다.", stadiumLocation);
                game.setStadium(stadiumLocation); // 지역명이라도 저장
            }
        } 
    }

    /**
     * Selenium을 사용하여 동적 웹페이지 렌더링
     * 
     * @param url 크롤링할 URL
     * @return 렌더링된 HTML 문자열 (실패시 null)
     */
    protected String getRenderedHtmlByUrl(String url) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");       // GUI 없이 실행
        options.addArguments("--no-sandbox");     // 리눅스에서 권한 문제 방지
        options.addArguments("--disable-dev-shm-usage"); // 메모리 부족 방지

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            driver.get(url);
            Thread.sleep(3000); // 페이지 로딩 대기
            return driver.getPageSource();
        } catch (Exception e) {
            log.error("Selenium 렌더링 실패 - URL: {}, 오류: {}", url, e.getMessage());
            return null;
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    /**
     * 크롤링된 팀명을 데이터베이스의 정식 팀명으로 변환
     * 
     * @param crawledTeamName 크롤링된 팀명 (예: "한화")
     * @return 데이터베이스 팀명 (예: "한화 이글스")
     */
    protected String mapTeamName(String crawledTeamName) {
        return TEAM_NAME_MAPPING.getOrDefault(crawledTeamName, crawledTeamName);
    }

    /**
     * 팀명으로 데이터베이스에서 팀 엔티티 조회
     * 
     * @param teamName 팀명
     * @return Team 엔티티 (없으면 null)
     */
    protected Team getTeamByName(String teamName) {
        return teamRepository.findByName(teamName).orElse(null);
    }

    /**
     * 날짜 문자열과 시간 문자열을 LocalDateTime으로 변환
     * 
     * @param dateStr 날짜 문자열 (yyyy-MM-dd)
     * @param timeStr 시간 문자열 (HH:mm)
     * @return LocalDateTime 객체 (파싱 실패시 14:00으로 기본값 설정)
     */
    protected LocalDateTime parseGameDateTime(String dateStr, String timeStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            return LocalDateTime.of(date, time);
        } catch (Exception e) {
            log.info("날짜/시간 파싱 실패: {} {}, 기본값(14:00) 사용", dateStr, timeStr);
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return LocalDateTime.of(date, LocalTime.of(14, 0));
        }
    }
}