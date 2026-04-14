package com.abin.mallchat.common.guild.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.abin.mallchat.common.guild.dao.ChannelMemberDao;
import com.abin.mallchat.common.guild.domain.entity.ChannelMember;
import com.abin.mallchat.common.guild.domain.vo.response.ChannelMemberResp;
import com.abin.mallchat.common.guild.service.GuildVoiceBroadcastService;
import com.abin.mallchat.common.user.domain.enums.WSBaseResp;
import com.abin.mallchat.common.user.domain.enums.WSRespTypeEnum;
import com.abin.mallchat.common.user.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GuildVoiceBroadcastServiceImpl implements GuildVoiceBroadcastService {
    
    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired
    private ChannelMemberDao channelMemberDao;
    
    @Override
    public void broadcastVoiceChannelUpdate(Long channelId, String action, ChannelMemberResp member) {
        Map<String, Object> update = new HashMap<>();
        update.put("voiceRoomId", channelId);
        update.put("action", action);
        update.put("member", member);
        
        sendToChannelMembers(channelId, update);
    }
    
    @Override
    public void sendToChannelMembers(Long channelId, Object message) {
        List<ChannelMember> members = channelMemberDao.getMembersByChannelId(channelId);
        if (CollectionUtil.isEmpty(members)) {
            return;
        }
        
        WSBaseResp<Object> resp = new WSBaseResp<>();
        resp.setType(WSRespTypeEnum.VOICE_ROOM_UPDATE.getType());
        resp.setData(message);
        
        for (ChannelMember member : members) {
            try {
                webSocketService.sendToUid(resp, member.getUid());
            } catch (Exception e) {
                log.error("发送WebSocket消息失败, uid: {}", member.getUid(), e);
            }
        }
    }
}
