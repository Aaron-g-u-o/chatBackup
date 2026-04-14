package com.abin.mallchat.common.guild.controller;

import com.abin.mallchat.common.common.domain.dto.RequestInfo;
import com.abin.mallchat.common.common.domain.vo.response.ApiResult;
import com.abin.mallchat.common.common.utils.RequestHolder;
import com.abin.mallchat.common.guild.domain.entity.ChannelMember;
import com.abin.mallchat.common.guild.domain.vo.response.ChannelMemberResp;
import com.abin.mallchat.common.guild.domain.vo.response.VoiceConnectionState;
import com.abin.mallchat.common.guild.domain.vo.response.VoiceMemberManageResult;
import com.abin.mallchat.common.guild.domain.vo.response.VoiceMemberVolume;
import com.abin.mallchat.common.guild.service.VoiceMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/capi/voice/room/member")
public class VoiceMemberController {
    
    @Autowired
    private VoiceMemberService voiceMemberService;
    
    private Long getUid() {
        RequestInfo requestInfo = RequestHolder.get();
        return requestInfo != null ? requestInfo.getUid() : null;
    }
    
    @PostMapping("/volume")
    public ApiResult<VoiceMemberManageResult> setVolume(
            @RequestParam Long voiceRoomId,
            @RequestParam Long targetUid,
            @RequestParam Integer volume) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        VoiceMemberManageResult result = voiceMemberService.setMemberVolume(uid, voiceRoomId, targetUid, volume);
        return ApiResult.success(result);
    }
    
    @PostMapping("/mute")
    public ApiResult<VoiceMemberManageResult> muteMember(
            @RequestParam Long voiceRoomId,
            @RequestParam Long targetUid) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        VoiceMemberManageResult result = voiceMemberService.muteMember(uid, voiceRoomId, targetUid);
        return ApiResult.success(result);
    }
    
    @PostMapping("/unmute")
    public ApiResult<VoiceMemberManageResult> unmuteMember(
            @RequestParam Long voiceRoomId,
            @RequestParam Long targetUid) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        VoiceMemberManageResult result = voiceMemberService.unmuteMember(uid, voiceRoomId, targetUid);
        return ApiResult.success(result);
    }
    
    @PostMapping("/kick")
    public ApiResult<VoiceMemberManageResult> kickMember(
            @RequestParam Long voiceRoomId,
            @RequestParam Long targetUid) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        VoiceMemberManageResult result = voiceMemberService.kickMember(uid, voiceRoomId, targetUid);
        return ApiResult.success(result);
    }
    
    @GetMapping("/status")
    public ApiResult<VoiceConnectionState> getConnectionStatus(
            @RequestParam Long voiceRoomId,
            @RequestParam Long targetUid) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        VoiceConnectionState state = voiceMemberService.getConnectionStatus(uid, voiceRoomId, targetUid);
        return ApiResult.success(state);
    }
    
    @GetMapping("/volumes")
    public ApiResult<List<VoiceMemberVolume>> getMemberVolumes(@RequestParam Long voiceRoomId) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        List<VoiceMemberVolume> volumes = voiceMemberService.getMemberVolumes(uid, voiceRoomId);
        return ApiResult.success(volumes);
    }
}
