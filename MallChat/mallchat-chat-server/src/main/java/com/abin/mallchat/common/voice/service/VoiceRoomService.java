package com.abin.mallchat.common.voice.service;

import com.abin.mallchat.common.voice.domain.vo.request.VoiceRoomReq;
import com.abin.mallchat.common.voice.domain.vo.response.VoiceRoomResp;

import java.util.List;

public interface VoiceRoomService {
    VoiceRoomResp createRoom(Long uid, VoiceRoomReq req);
    
    VoiceRoomResp joinRoom(Long uid, Long voiceRoomId);
    
    void leaveRoom(Long uid, Long voiceRoomId);
    
    void leaveAllRooms(Long uid);
    
    List<VoiceRoomResp> getActiveRooms();
    
    VoiceRoomResp getRoomDetail(Long voiceRoomId);
    
    void updateMemberStatus(Long uid, Long voiceRoomId, Boolean muted, Boolean deafened, Boolean speaking);
}
