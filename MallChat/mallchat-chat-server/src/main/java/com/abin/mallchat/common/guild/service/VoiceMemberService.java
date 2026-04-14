package com.abin.mallchat.common.guild.service;

import com.abin.mallchat.common.guild.domain.vo.response.VoiceConnectionState;
import com.abin.mallchat.common.guild.domain.vo.response.VoiceMemberManageResult;
import com.abin.mallchat.common.guild.domain.vo.response.VoiceMemberVolume;

import java.util.List;

public interface VoiceMemberService {
    
    VoiceMemberManageResult setMemberVolume(Long operatorUid, Long voiceRoomId, Long targetUid, Integer volume);
    
    VoiceMemberManageResult muteMember(Long operatorUid, Long voiceRoomId, Long targetUid);
    
    VoiceMemberManageResult unmuteMember(Long operatorUid, Long voiceRoomId, Long targetUid);
    
    VoiceMemberManageResult kickMember(Long operatorUid, Long voiceRoomId, Long targetUid);
    
    VoiceConnectionState getConnectionStatus(Long operatorUid, Long voiceRoomId, Long targetUid);
    
    List<VoiceMemberVolume> getMemberVolumes(Long operatorUid, Long voiceRoomId);
}
