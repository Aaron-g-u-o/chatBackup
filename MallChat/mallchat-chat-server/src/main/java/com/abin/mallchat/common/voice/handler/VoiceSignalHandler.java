package com.abin.mallchat.common.voice.handler;

import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import com.abin.mallchat.common.user.domain.dto.WSChannelExtraDTO;
import com.abin.mallchat.common.user.service.WebSocketService;
import com.abin.mallchat.common.user.service.impl.WebSocketServiceImpl;
import com.abin.mallchat.common.voice.domain.vo.request.WSVoiceSignalReq;
import com.abin.mallchat.common.voice.service.WebRTCSignalService;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class VoiceSignalHandler {

    private static final ConcurrentHashMap<Long, Long> USER_VOICE_ROOM_MAP = new ConcurrentHashMap<>();

    private WebRTCSignalService webRTCSignalService;
    private WebSocketService webSocketService;

    public void handleSignal(Channel channel, String data) {
        initServices();
        WSChannelExtraDTO extra = WebSocketServiceImpl.getOnlineMap().get(channel);
        if (extra == null || extra.getUid() == null) {
            log.warn("用户未登录，无法处理语音信令");
            return;
        }

        Long uid = extra.getUid();
        WSVoiceSignalReq signal = JSONUtil.toBean(data, WSVoiceSignalReq.class);

        webRTCSignalService.handleSignal(uid, signal);
    }

    public static void bindVoiceRoom(Long uid, Long voiceRoomId) {
        USER_VOICE_ROOM_MAP.put(uid, voiceRoomId);
    }

    public static void unbindVoiceRoom(Long uid) {
        USER_VOICE_ROOM_MAP.remove(uid);
    }

    public static Long getVoiceRoomId(Long uid) {
        return USER_VOICE_ROOM_MAP.get(uid);
    }

    private void initServices() {
        if (webRTCSignalService == null) {
            webRTCSignalService = SpringUtil.getBean(WebRTCSignalService.class);
        }
        if (webSocketService == null) {
            webSocketService = SpringUtil.getBean(WebSocketService.class);
        }
    }

    private static Class<?> webSocketServiceImplClass;

    static {
        try {
            webSocketServiceImplClass = Class.forName("com.abin.mallchat.common.user.service.impl.WebSocketServiceImpl");
        } catch (ClassNotFoundException e) {
            log.error("WebSocketServiceImpl class not found", e);
        }
    }
}
