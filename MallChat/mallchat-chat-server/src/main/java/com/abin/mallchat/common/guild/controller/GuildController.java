package com.abin.mallchat.common.guild.controller;

import com.abin.mallchat.common.common.domain.dto.RequestInfo;
import com.abin.mallchat.common.common.domain.vo.response.ApiResult;
import com.abin.mallchat.common.common.utils.RequestHolder;
import com.abin.mallchat.common.guild.domain.vo.request.CreateChannelReq;
import com.abin.mallchat.common.guild.domain.vo.request.CreateGuildReq;
import com.abin.mallchat.common.guild.domain.vo.response.ChannelResp;
import com.abin.mallchat.common.guild.domain.vo.response.GuildResp;
import com.abin.mallchat.common.guild.service.GuildService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/capi/guild")
public class GuildController {
    
    @Autowired
    private GuildService guildService;
    
    private Long getUid() {
        RequestInfo requestInfo = RequestHolder.get();
        return requestInfo != null ? requestInfo.getUid() : null;
    }
    
    @PostMapping("/create")
    public ApiResult<GuildResp> createGuild(@RequestBody CreateGuildReq req) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        GuildResp resp = guildService.createGuild(uid, req);
        return ApiResult.success(resp);
    }
    
    @GetMapping("/list")
    public ApiResult<List<GuildResp>> getUserGuilds() {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.success(Collections.emptyList());
        }
        List<GuildResp> guilds = guildService.getUserGuilds(uid);
        return ApiResult.success(guilds);
    }
    
    @GetMapping("/public")
    public ApiResult<List<GuildResp>> getPublicGuilds(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.success(Collections.emptyList());
        }
        List<GuildResp> guilds = guildService.getPublicGuilds(uid, page, pageSize);
        return ApiResult.success(guilds);
    }
    
    @GetMapping("/detail/{guildId}")
    public ApiResult<GuildResp> getGuildDetail(@PathVariable Long guildId) {
        GuildResp resp = guildService.getGuildDetail(guildId);
        return ApiResult.success(resp);
    }
    
    @PostMapping("/join/{guildId}")
    public ApiResult<Void> joinGuild(@PathVariable Long guildId) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        guildService.joinGuild(uid, guildId);
        return ApiResult.success();
    }
    
    @PostMapping("/join/code")
    public ApiResult<GuildResp> joinGuildByInviteCode(@RequestParam String inviteCode) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        GuildResp resp = guildService.joinGuildByInviteCode(uid, inviteCode);
        return ApiResult.success(resp);
    }
    
    @PostMapping("/leave/{guildId}")
    public ApiResult<Void> leaveGuild(@PathVariable Long guildId) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        guildService.leaveGuild(uid, guildId);
        return ApiResult.success();
    }
    
    @PostMapping("/channel/create")
    public ApiResult<ChannelResp> createChannel(@RequestBody CreateChannelReq req) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        ChannelResp resp = guildService.createChannel(uid, req);
        return ApiResult.success(resp);
    }
    
    @DeleteMapping("/channel/{channelId}")
    public ApiResult<Void> deleteChannel(@PathVariable Long channelId) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        guildService.deleteChannel(uid, channelId);
        return ApiResult.success();
    }
    
    @GetMapping("/{guildId}/channels")
    public ApiResult<List<ChannelResp>> getGuildChannels(@PathVariable Long guildId) {
        List<ChannelResp> channels = guildService.getGuildChannels(guildId);
        return ApiResult.success(channels);
    }
    
    @PostMapping("/channel/voice/join/{channelId}")
    public ApiResult<Void> joinVoiceChannel(@PathVariable Long channelId) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        guildService.joinVoiceChannel(uid, channelId);
        return ApiResult.success();
    }
    
    @PostMapping("/channel/voice/leave/{channelId}")
    public ApiResult<Void> leaveVoiceChannel(@PathVariable Long channelId) {
        Long uid = getUid();
        if (uid == null) {
            return ApiResult.fail(401, "未登录");
        }
        guildService.leaveVoiceChannel(uid, channelId);
        return ApiResult.success();
    }
    
    @GetMapping("/channel/voice/members/{channelId}")
    public ApiResult<List<ChannelResp>> getVoiceChannelMembers(@PathVariable Long channelId) {
        List<ChannelResp> members = guildService.getVoiceChannelMembers(channelId);
        return ApiResult.success(members);
    }
}
