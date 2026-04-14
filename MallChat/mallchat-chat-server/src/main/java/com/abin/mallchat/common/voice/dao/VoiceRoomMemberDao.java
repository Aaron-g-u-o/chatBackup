package com.abin.mallchat.common.voice.dao;

import com.abin.mallchat.common.voice.domain.entity.VoiceRoomMember;
import com.abin.mallchat.common.voice.mapper.VoiceRoomMemberMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VoiceRoomMemberDao extends ServiceImpl<VoiceRoomMemberMapper, VoiceRoomMember> {
    
    public List<VoiceRoomMember> getActiveMembers(Long voiceRoomId) {
        return lambdaQuery()
                .eq(VoiceRoomMember::getVoiceRoomId, voiceRoomId)
                .isNull(VoiceRoomMember::getLeaveTime)
                .list();
    }
    
    public VoiceRoomMember getActiveMember(Long voiceRoomId, Long uid) {
        return lambdaQuery()
                .eq(VoiceRoomMember::getVoiceRoomId, voiceRoomId)
                .eq(VoiceRoomMember::getUid, uid)
                .isNull(VoiceRoomMember::getLeaveTime)
                .one();
    }
    
    public void leaveRoom(Long voiceRoomId, Long uid) {
        lambdaUpdate()
                .eq(VoiceRoomMember::getVoiceRoomId, voiceRoomId)
                .eq(VoiceRoomMember::getUid, uid)
                .isNull(VoiceRoomMember::getLeaveTime)
                .set(VoiceRoomMember::getLeaveTime, new java.util.Date())
                .update();
    }
}
