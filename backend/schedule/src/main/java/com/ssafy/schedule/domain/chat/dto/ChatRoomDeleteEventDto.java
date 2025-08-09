package com.ssafy.schedule.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDeleteEventDto {
    
    private String date;
    
    public static ChatRoomDeleteEventDto of(String date) {
        return new ChatRoomDeleteEventDto(date);
    }
}