package com.ssafy.chat.match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameListResponse {
    private String date;
    private List<GameInfoResponse> games;
}