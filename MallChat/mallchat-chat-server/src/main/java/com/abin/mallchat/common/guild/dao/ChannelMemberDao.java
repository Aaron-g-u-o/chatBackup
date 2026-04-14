package com.abin.mallchat.common.guild.dao;

import com.abin.mallchat.common.guild.domain.entity.ChannelMember;
import com.abin.mallchat.common.guild.mapper.ChannelMemberMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChannelMemberDao extends ServiceImpl<ChannelMemberMapper, ChannelMember> {
    
    public List<ChannelMember> getMembersByChannelId(Long channelId) {
        return lambdaQuery()
                .eq(ChannelMember::getChannelId, channelId)
                .list();
    }
    
    public ChannelMember getMember(Long channelId, Long uid) {
        return lambdaQuery()
                .eq(ChannelMember::getChannelId, channelId)
                .eq(ChannelMember::getUid, uid)
                .one();
    }
    
    public void removeMember(Long channelId, Long uid) {
        lambdaUpdate()
                .eq(ChannelMember::getChannelId, channelId)
                .eq(ChannelMember::getUid, uid)
                .remove();
    }
    
    public void updateMemberVolume(Long channelId, Long uid, Integer volume) {
        lambdaUpdate()
                .eq(ChannelMember::getChannelId, channelId)
                .eq(ChannelMember::getUid, uid)
                .set(ChannelMember::getVolume, volume)
                .update();
    }
    
    public void updateMemberMuted(Long channelId, Long uid, Integer muted) {
        lambdaUpdate()
                .eq(ChannelMember::getChannelId, channelId)
                .eq(ChannelMember::getUid, uid)
                .set(ChannelMember::getMuted, muted)
                .update();
    }
}
