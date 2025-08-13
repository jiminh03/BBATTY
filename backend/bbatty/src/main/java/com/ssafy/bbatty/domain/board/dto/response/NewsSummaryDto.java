package com.ssafy.bbatty.domain.board.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsSummaryDto {
    
    private String title;
    private String summary;
}