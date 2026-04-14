package com.abin.mallchat.common.voice.service;

import com.abin.mallchat.common.voice.domain.vo.request.WSVoiceSignalReq;

public interface WebRTCSignalService {
    void handleSignal(Long uid, WSVoiceSignalReq signal);
    
    void broadcastToRoom(Long voiceRoomId, Long excludeUid, Object message);
    
    void sendToUser(Long uid, Object message);
}
