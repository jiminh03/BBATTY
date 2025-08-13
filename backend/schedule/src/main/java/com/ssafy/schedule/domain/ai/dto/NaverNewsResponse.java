package com.ssafy.schedule.domain.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class NaverNewsResponse {
    private String lastBuildDate;
    private int total;
    private int start;
    private int display;
    private List<Item> items;


    @Data
    public static class Item {
        private String title;
        private String originalLink;
        private String link;
        private String description;
        private String pubDate;
    }
}
