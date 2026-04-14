package com.abin.mallchat.common.voice.dao;

import com.abin.mallchat.common.voice.domain.entity.VoiceRoom;
import com.abin.mallchat.common.voice.mapper.VoiceRoomMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VoiceRoomDao extends ServiceImpl<VoiceRoomMapper, VoiceRoom> {
    
    public List<VoiceRoom> getActiveRooms() {
        return lambdaQuery()
                .eq(VoiceRoom::getStatus, 1)
                .orderByDesc(VoiceRoom::getCurrentUserCount)
                .list();
    }
    
    public VoiceRoom getByRoomId(Long roomId) {
        return lambdaQuery()
                .eq(VoiceRoom::getRoomId, roomId)
                .eq(VoiceRoom::getStatus, 1)
                .one();
    }
    
    public void incrementUserCount(Long voiceRoomId) {
        lambdaUpdate()
                .eq(VoiceRoom::getId, voiceRoomId)
                .setSql("current_user_count = current_user_count + 1")
                .update();
    }
    
    public void decrementUserCount(Long voiceRoomId) {
        lambdaUpdate()
                .eq(VoiceRoom::getId, voiceRoomId)
                .setSql("current_user_count = GREATEST(current_user_count - 1, 0)")
                .update();
    }
}
