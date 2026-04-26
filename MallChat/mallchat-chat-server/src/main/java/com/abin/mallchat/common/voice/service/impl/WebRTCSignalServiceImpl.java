package com.abin.mallchat.common.voice.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.abin.mallchat.common.user.domain.enums.WSBaseResp;
import com.abin.mallchat.common.user.domain.enums.WSRespTypeEnum;
import com.abin.mallchat.common.user.service.WebSocketService;
import com.abin.mallchat.common.voice.dao.VoiceRoomMemberDao;
import com.abin.mallchat.common.voice.domain.entity.VoiceRoomMember;
import com.abin.mallchat.common.voice.domain.vo.request.WSVoiceSignalReq;
import com.abin.mallchat.common.voice.domain.vo.ws.WSVoiceSignal;
import com.abin.mallchat.common.voice.service.WebRTCSignalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class WebRTCSignalServiceImpl implements WebRTCSignalService {
    
    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired
    private VoiceRoomMemberDao voiceRoomMemberDao;
    
    @Override
    public void handleSignal(Long uid, WSVoiceSignalReq signal) {
        WSVoiceSignal wsSignal = new WSVoiceSignal();
        wsSignal.setType(signal.getType());
        wsSignal.setVoiceRoomId(signal.getVoiceRoomId());
        wsSignal.setFromUid(uid);
        wsSignal.setTargetUid(signal.getTargetUid());
        wsSignal.setSdp(signal.getSdp());
        wsSignal.setCandidate(signal.getCandidate());
        
        if (signal.getTargetUid() != null) {
            sendToUser(signal.getTargetUid(), wsSignal);
        } else {
            broadcastToRoom(signal.getVoiceRoomId(), uid, wsSignal);
        }
    }
    
    @Override
    public void broadcastToRoom(Long voiceRoomId, Long excludeUid, Object message) {
        List<VoiceRoomMember> members = voiceRoomMemberDao.getActiveMembers(voiceRoomId);
        if (CollectionUtil.isEmpty(members)) {
            return;
        }
        
        boolean isRoomUpdate = message instanceof com.abin.mallchat.common.voice.domain.vo.ws.WSVoiceRoomUpdate;
        
        WSBaseResp<Object> resp = new WSBaseResp<>();
        resp.setType(isRoomUpdate ? WSRespTypeEnum.VOICE_ROOM_UPDATE.getType() : WSRespTypeEnum.VOICE_SIGNAL.getType());
        resp.setData(message);
        
        for (VoiceRoomMember member : members) {
            if (excludeUid != null && excludeUid.equals(member.getUid())) {
                continue;
            }
            try {
                webSocketService.sendToUid(resp, member.getUid());
            } catch (Exception e) {
                log.warn("广播消息发送失败, uid={}, voiceRoomId={}", member.getUid(), voiceRoomId, e);
            }
        }
    }
    
    @Override
    public void sendToUser(Long uid, Object message) {
        WSBaseResp<Object> resp = new WSBaseResp<>();
        resp.setType(WSRespTypeEnum.VOICE_SIGNAL.getType());
        resp.setData(message);
        webSocketService.sendToUid(resp, uid);
    }
}
