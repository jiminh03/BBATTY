package com.ssafy.bbatty.domain.chat.common.service;

import com.ssafy.bbatty.domain.chat.common.dto.ChatRoomStatus;
import com.ssafy.bbatty.domain.chat.common.enums.ChatRoomType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 채팅방 관리를 위한 기본 서비스 인터페이스
 * 모든 채팅방 타입에서 공통으로 사용되는 기본 기능들을 정의
 */
public interface BaseChatRoomService {

    /**
     * 채팅방 생성
     * @param roomId 방 ID
     * @param roomType 방 타입
     * @param metadata 추가 메타데이터
     * @return 생성 성공 여부
     */
    boolean createChatRoom(String roomId, ChatRoomType roomType, Map<String, Object> metadata);

    /**
     * 채팅방 삭제
     * @param roomId 방 ID
     * @return 삭제 성공 여부
     */
    boolean deleteChatRoom(String roomId);

    /**
     * 채팅방 존재 여부 확인
     * @param roomId 방 ID
     * @return 존재 여부
     */
    boolean existsChatRoom(String roomId);

    /**
     * 채팅방 활성화 상태 확인
     * @param roomId 방 ID
     * @return 활성화 여부
     */
    boolean isRoomActive(String roomId);

    /**
     * 채팅방 활성화
     * @param roomId 방 ID
     * @return 활성화 성공 여부
     */
    boolean activateRoom(String roomId);

    /**
     * 채팅방 비활성화
     * @param roomId 방 ID
     * @return 비활성화 성공 여부
     */
    boolean deactivateRoom(String roomId);

    /**
     * 채팅방 상태 조회
     * @param roomId 방 ID
     * @return 채팅방 상태 정보
     */
    ChatRoomStatus getRoomStatus(String roomId);

    /**
     * 채팅방 메타데이터 조회
     * @param roomId 방 ID
     * @return 메타데이터
     */
    Map<String, Object> getRoomMetadata(String roomId);

    /**
     * 채팅방 메타데이터 업데이트
     * @param roomId 방 ID
     * @param metadata 업데이트할 메타데이터
     * @return 업데이트 성공 여부
     */
    boolean updateRoomMetadata(String roomId, Map<String, Object> metadata);

    /**
     * 활성 채팅방 목록 조회
     * @param roomType 방 타입 (null이면 모든 타입)
     * @return 활성 채팅방 ID 목록
     */
    Set<String> getActiveRooms(ChatRoomType roomType);

    /**
     * 만료된 채팅방 정리
     * @param expireTime 만료 기준 시간
     * @return 정리된 방 개수
     */
    int cleanupExpiredRooms(LocalDateTime expireTime);

    /**
     * 채팅방 TTL 설정
     * @param roomId 방 ID
     * @param ttlSeconds TTL (초)
     * @return 설정 성공 여부
     */
    boolean setRoomTTL(String roomId, long ttlSeconds);
}
