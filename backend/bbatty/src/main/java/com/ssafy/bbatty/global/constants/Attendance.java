package com.ssafy.bbatty.global.constants;

public enum Attendance {
    ; // enum 인스턴스는 없지만 상수들이 있다는 표시
      // 컴파일러에게 "이 enum은 상수 컨테이너용"이라고 알려줌

    // GPS 관련 상수
    public static final double STADIUM_RADIUS_KM = 0.2;  // 경기장 반경 200m
    public static final double EARTH_RADIUS_KM = 6371.0; // 지구 반지름

    // 시간 관련 상수
    public static final int ATTENDANCE_START_HOURS_BEFORE = 2;  // 경기 2시간 전부터 인증 가능
}
