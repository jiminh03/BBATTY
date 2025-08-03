package com.ssafy.bbatty.domain.attendance.entity;

import lombok.*;

import java.io.Serializable;

/**
 * UserAttended 엔티티의 복합키
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserAttendedId implements Serializable {
    private Long userId;
    private Long gameId;
}