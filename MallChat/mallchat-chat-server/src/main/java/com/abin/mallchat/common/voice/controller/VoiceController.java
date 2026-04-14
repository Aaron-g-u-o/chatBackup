package com.abin.mallchat.common.voice.controller;

import com.abin.mallchat.common.common.domain.vo.response.ApiResult;
import com.abin.mallchat.common.common.utils.RequestHolder;
import com.abin.mallchat.common.voice.domain.vo.request.VoiceRoomReq;
import com.abin.mallchat.common.voice.domain.vo.response.VoiceRoomResp;
import com.abin.mallchat.common.voice.service.VoiceRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/voice")
public class VoiceController {
    
    @Autowired
    private VoiceRoomService voiceRoomService;
    
    @PostMapping("/room/create")
    public ApiResult<VoiceRoomResp> createRoom(@RequestBody VoiceRoomReq req) {
        Long uid = RequestHolder.get().getUid();
        VoiceRoomResp resp = voiceRoomService.createRoom(uid, req);
        return ApiResult.success(resp);
    }
    
    @PostMapping("/room/join/{voiceRoomId}")
    public ApiResult<VoiceRoomResp> joinRoom(@PathVariable Long voiceRoomId) {
        Long uid = RequestHolder.get().getUid();
        VoiceRoomResp resp = voiceRoomService.joinRoom(uid, voiceRoomId);
        return ApiResult.success(resp);
    }
    
    @PostMapping("/room/leave/{voiceRoomId}")
    public ApiResult<Void> leaveRoom(@PathVariable Long voiceRoomId) {
        Long uid = RequestHolder.get().getUid();
        voiceRoomService.leaveRoom(uid, voiceRoomId);
        return ApiResult.success();
    }
    
    @GetMapping("/room/list")
    public ApiResult<List<VoiceRoomResp>> getRoomList() {
        List<VoiceRoomResp> rooms = voiceRoomService.getActiveRooms();
        return ApiResult.success(rooms);
    }
    
    @GetMapping("/room/detail/{voiceRoomId}")
    public ApiResult<VoiceRoomResp> getRoomDetail(@PathVariable Long voiceRoomId) {
        VoiceRoomResp resp = voiceRoomService.getRoomDetail(voiceRoomId);
        return ApiResult.success(resp);
    }
    
    @PostMapping("/room/status")
    public ApiResult<Void> updateStatus(
            @RequestParam Long voiceRoomId,
            @RequestParam(required = false) Boolean muted,
            @RequestParam(required = false) Boolean deafened,
            @RequestParam(required = false) Boolean speaking) {
        Long uid = RequestHolder.get().getUid();
        voiceRoomService.updateMemberStatus(uid, voiceRoomId, muted, deafened, speaking);
        return ApiResult.success();
    }
}
