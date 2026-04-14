package com.abin.mallchat.common.guild.service.impl;

import com.abin.mallchat.common.guild.dao.ChannelDao;
import com.abin.mallchat.common.guild.dao.ChannelMemberDao;
import com.abin.mallchat.common.guild.dao.GuildMemberDao;
import com.abin.mallchat.common.guild.domain.entity.Channel;
import com.abin.mallchat.common.guild.domain.entity.ChannelMember;
import com.abin.mallchat.common.guild.domain.entity.GuildMember;
import com.abin.mallchat.common.guild.domain.enums.ChannelTypeEnum;
import com.abin.mallchat.common.guild.domain.enums.GuildMemberRoleEnum;
import com.abin.mallchat.common.guild.domain.vo.response.ChannelMemberResp;
import com.abin.mallchat.common.guild.domain.vo.response.VoiceConnectionState;
import com.abin.mallchat.common.guild.domain.vo.response.VoiceMemberManageResult;
import com.abin.mallchat.common.guild.domain.vo.response.VoiceMemberVolume;
import com.abin.mallchat.common.guild.service.VoiceMemberService;
import com.abin.mallchat.common.user.dao.UserDao;
import com.abin.mallchat.common.user.domain.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VoiceMemberServiceImpl implements VoiceMemberService {
    
    @Autowired
    private ChannelDao channelDao;
    
    @Autowired
    private ChannelMemberDao channelMemberDao;
    
    @Autowired
    private GuildMemberDao guildMemberDao;
    
    @Autowired
    private UserDao userDao;
    
    @Override
    @Transactional
    public VoiceMemberManageResult setMemberVolume(Long operatorUid, Long voiceRoomId, Long targetUid, Integer volume) {
        VoiceMemberManageResult result = new VoiceMemberManageResult();
        
        try {
            Channel channel = channelDao.getById(voiceRoomId);
            if (channel == null || !ChannelTypeEnum.VOICE.getType().equals(channel.getType())) {
                result.setSuccess(false);
                result.setMessage("语音频道不存在");
                return result;
            }
            
            ChannelMember operator = channelMemberDao.getMember(voiceRoomId, operatorUid);
            if (operator == null) {
                result.setSuccess(false);
                result.setMessage("您不在该语音频道中");
                return result;
            }
            
            ChannelMember target = channelMemberDao.getMember(voiceRoomId, targetUid);
            if (target == null) {
                result.setSuccess(false);
                result.setMessage("目标用户不在该语音频道中");
                return result;
            }
            
            channelMemberDao.updateMemberVolume(voiceRoomId, targetUid, volume);
            
            target.setVolume(volume);
            result.setSuccess(true);
            result.setMessage("音量调整成功");
            result.setMember(buildChannelMemberResp(target));
            
        } catch (Exception e) {
            log.error("调整音量失败", e);
            result.setSuccess(false);
            result.setMessage("调整音量失败: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    @Transactional
    public VoiceMemberManageResult muteMember(Long operatorUid, Long voiceRoomId, Long targetUid) {
        VoiceMemberManageResult result = new VoiceMemberManageResult();
        
        try {
            Channel channel = channelDao.getById(voiceRoomId);
            if (channel == null || !ChannelTypeEnum.VOICE.getType().equals(channel.getType())) {
                result.setSuccess(false);
                result.setMessage("语音频道不存在");
                return result;
            }
            
            ChannelMember operator = channelMemberDao.getMember(voiceRoomId, operatorUid);
            if (operator == null) {
                result.setSuccess(false);
                result.setMessage("您不在该语音频道中");
                return result;
            }
            
            ChannelMember target = channelMemberDao.getMember(voiceRoomId, targetUid);
            if (target == null) {
                result.setSuccess(false);
                result.setMessage("目标用户不在该语音频道中");
                return result;
            }
            
            if (!hasPermission(operatorUid, channel.getGuildId(), targetUid)) {
                result.setSuccess(false);
                result.setMessage("没有权限执行此操作");
                return result;
            }
            
            channelMemberDao.updateMemberMuted(voiceRoomId, targetUid, 1);
            
            target.setMuted(1);
            result.setSuccess(true);
            result.setMessage("已静音");
            result.setMember(buildChannelMemberResp(target));
            
        } catch (Exception e) {
            log.error("静音失败", e);
            result.setSuccess(false);
            result.setMessage("静音失败: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    @Transactional
    public VoiceMemberManageResult unmuteMember(Long operatorUid, Long voiceRoomId, Long targetUid) {
        VoiceMemberManageResult result = new VoiceMemberManageResult();
        
        try {
            Channel channel = channelDao.getById(voiceRoomId);
            if (channel == null || !ChannelTypeEnum.VOICE.getType().equals(channel.getType())) {
                result.setSuccess(false);
                result.setMessage("语音频道不存在");
                return result;
            }
            
            ChannelMember operator = channelMemberDao.getMember(voiceRoomId, operatorUid);
            if (operator == null) {
                result.setSuccess(false);
                result.setMessage("您不在该语音频道中");
                return result;
            }
            
            ChannelMember target = channelMemberDao.getMember(voiceRoomId, targetUid);
            if (target == null) {
                result.setSuccess(false);
                result.setMessage("目标用户不在该语音频道中");
                return result;
            }
            
            if (!hasPermission(operatorUid, channel.getGuildId(), targetUid)) {
                result.setSuccess(false);
                result.setMessage("没有权限执行此操作");
                return result;
            }
            
            channelMemberDao.updateMemberMuted(voiceRoomId, targetUid, 0);
            
            target.setMuted(0);
            result.setSuccess(true);
            result.setMessage("已取消静音");
            result.setMember(buildChannelMemberResp(target));
            
        } catch (Exception e) {
            log.error("取消静音失败", e);
            result.setSuccess(false);
            result.setMessage("取消静音失败: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    @Transactional
    public VoiceMemberManageResult kickMember(Long operatorUid, Long voiceRoomId, Long targetUid) {
        VoiceMemberManageResult result = new VoiceMemberManageResult();
        
        try {
            Channel channel = channelDao.getById(voiceRoomId);
            if (channel == null || !ChannelTypeEnum.VOICE.getType().equals(channel.getType())) {
                result.setSuccess(false);
                result.setMessage("语音频道不存在");
                return result;
            }
            
            ChannelMember operator = channelMemberDao.getMember(voiceRoomId, operatorUid);
            if (operator == null) {
                result.setSuccess(false);
                result.setMessage("您不在该语音频道中");
                return result;
            }
            
            ChannelMember target = channelMemberDao.getMember(voiceRoomId, targetUid);
            if (target == null) {
                result.setSuccess(false);
                result.setMessage("目标用户不在该语音频道中");
                return result;
            }
            
            if (targetUid.equals(operatorUid)) {
                result.setSuccess(false);
                result.setMessage("不能将自己移出语音频道");
                return result;
            }
            
            if (!hasPermission(operatorUid, channel.getGuildId(), targetUid)) {
                result.setSuccess(false);
                result.setMessage("没有权限执行此操作");
                return result;
            }
            
            channelMemberDao.removeMember(voiceRoomId, targetUid);
            
            result.setSuccess(true);
            result.setMessage("已移出语音频道");
            
        } catch (Exception e) {
            log.error("移出用户失败", e);
            result.setSuccess(false);
            result.setMessage("移出用户失败: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public VoiceConnectionState getConnectionStatus(Long operatorUid, Long voiceRoomId, Long targetUid) {
        Channel channel = channelDao.getById(voiceRoomId);
        if (channel == null || !ChannelTypeEnum.VOICE.getType().equals(channel.getType())) {
            return null;
        }
        
        ChannelMember operator = channelMemberDao.getMember(voiceRoomId, operatorUid);
        if (operator == null) {
            return null;
        }
        
        ChannelMember target = channelMemberDao.getMember(voiceRoomId, targetUid);
        if (target == null) {
            return null;
        }
        
        VoiceConnectionState state = new VoiceConnectionState();
        state.setUid(targetUid);
        state.setConnectionState("connected");
        state.setLatency((int) (Math.random() * 100) + 20);
        state.setQuality(calculateQuality(state.getLatency()));
        state.setLastUpdateTime(System.currentTimeMillis());
        
        return state;
    }
    
    @Override
    public List<VoiceMemberVolume> getMemberVolumes(Long operatorUid, Long voiceRoomId) {
        Channel channel = channelDao.getById(voiceRoomId);
        if (channel == null || !ChannelTypeEnum.VOICE.getType().equals(channel.getType())) {
            return new ArrayList<>();
        }
        
        ChannelMember operator = channelMemberDao.getMember(voiceRoomId, operatorUid);
        if (operator == null) {
            return new ArrayList<>();
        }
        
        List<ChannelMember> members = channelMemberDao.getMembersByChannelId(voiceRoomId);
        
        return members.stream().map(m -> {
            VoiceMemberVolume volume = new VoiceMemberVolume();
            volume.setUid(m.getUid());
            volume.setVolume(m.getVolume() != null ? m.getVolume() : 100);
            return volume;
        }).collect(Collectors.toList());
    }
    
    private boolean hasPermission(Long operatorUid, Long guildId, Long targetUid) {
        if (operatorUid.equals(targetUid)) {
            return true;
        }
        
        GuildMember operatorMember = guildMemberDao.getMember(guildId, operatorUid);
        if (operatorMember == null) {
            return false;
        }
        
        if (operatorMember.getRoleId() >= GuildMemberRoleEnum.ADMIN.getRoleId()) {
            return true;
        }
        
        return false;
    }
    
    private ChannelMemberResp buildChannelMemberResp(ChannelMember member) {
        ChannelMemberResp resp = new ChannelMemberResp();
        resp.setUid(member.getUid());
        resp.setMuted(member.getMuted());
        resp.setDeafened(member.getDeafened());
        resp.setSpeaking(member.getSpeaking());
        resp.setVolume(member.getVolume() != null ? member.getVolume() : 100);
        
        User user = userDao.getById(member.getUid());
        if (user != null) {
            resp.setName(user.getName());
            resp.setAvatar(user.getAvatar());
        }
        
        return resp;
    }
    
    private String calculateQuality(Integer latency) {
        if (latency < 50) {
            return "excellent";
        } else if (latency < 100) {
            return "good";
        } else {
            return "poor";
        }
    }
}
