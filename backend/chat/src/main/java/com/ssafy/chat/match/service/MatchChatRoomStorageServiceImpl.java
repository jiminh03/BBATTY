package com.ssafy.chat.match.service;

import com.ssafy.chat.match.dto.MatchChatRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 매칭 채팅방 저장소 서비스 구현체 (임시 스텁)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchChatRoomStorageServiceImpl implements MatchChatRoomStorageService {
    
    @Override
    public void saveMatchChatRoom(MatchChatRoom chatRoom, String gameDate) {
        log.info("매칭 채팅방 저장 (스텁) - matchId: {}", chatRoom.getMatchId());
        // TODO: 실제 Redis 저장 로직 구현 필요
    }
    
    @Override
    public List<MatchChatRoom> getMatchChatRoomsByGameId(Long gameId, int offset, int limit) {
        log.debug("게임별 매칭 채팅방 조회 (스텁) - gameId: {}", gameId);
        // TODO: 실제 조회 로직 구현 필요
        return Collections.emptyList();
    }
    
    @Override
    public List<MatchChatRoom> searchMatchChatRoomsByKeyword(String keyword, int offset, int limit) {
        log.debug("키워드 검색 (스텁) - keyword: {}", keyword);
        // TODO: 실제 검색 로직 구현 필요
        return Collections.emptyList();
    }
    
    @Override
    public MatchChatRoom getMatchChatRoom(String matchId) {
        log.debug("매칭 채팅방 조회 (스텁) - matchId: {}", matchId);
        // TODO: 실제 조회 로직 구현 필요
        return null;
    }
    
    @Override
    public boolean existsMatchChatRoom(String matchId) {
        log.debug("매칭 채팅방 존재 확인 (스텁) - matchId: {}", matchId);
        // TODO: 실제 존재 확인 로직 구현 필요
        return false;
    }
}