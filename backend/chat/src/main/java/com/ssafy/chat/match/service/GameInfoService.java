package com.ssafy.chat.match.service;

import com.ssafy.chat.match.dto.GameInfo;
import com.ssafy.chat.match.dto.GameListResponse;
import java.util.List;

public interface GameInfoService {
    void processGameInfoMessage(String message);
    void saveGameInfoList(List<GameInfo> gameInfos);
    List<GameInfo> getGameInfosByDate(String date);
    GameListResponse getGamesListByDate(String date);
    List<GameListResponse> getGamesListByDateRange(String startDate, String endDate);
}