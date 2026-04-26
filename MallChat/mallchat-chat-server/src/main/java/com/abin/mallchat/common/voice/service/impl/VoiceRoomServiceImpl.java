package com.abin.mallchat.common.voice.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.abin.mallchat.common.user.dao.UserDao;
import com.abin.mallchat.common.user.domain.entity.User;
import com.abin.mallchat.common.voice.dao.VoiceRoomDao;
import com.abin.mallchat.common.voice.dao.VoiceRoomMemberDao;
import com.abin.mallchat.common.voice.domain.entity.VoiceRoom;
import com.abin.mallchat.common.voice.domain.entity.VoiceRoomMember;
import com.abin.mallchat.common.voice.domain.enums.VoiceRoomStatusEnum;
import com.abin.mallchat.common.voice.domain.vo.request.VoiceRoomReq;
import com.abin.mallchat.common.voice.domain.vo.response.VoiceMemberResp;
import com.abin.mallchat.common.voice.domain.vo.response.VoiceRoomResp;
import com.abin.mallchat.common.voice.domain.vo.ws.WSVoiceRoomUpdate;
import com.abin.mallchat.common.voice.handler.VoiceSignalHandler;
import com.abin.mallchat.common.voice.service.VoiceRoomService;
import com.abin.mallchat.common.voice.service.WebRTCSignalService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VoiceRoomServiceImpl implements VoiceRoomService {

    @Autowired
    private VoiceRoomDao voiceRoomDao;

    @Autowired
    private VoiceRoomMemberDao voiceRoomMemberDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private WebRTCSignalService webRTCSignalService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public VoiceRoomResp createRoom(Long uid, VoiceRoomReq req) {
        VoiceRoom voiceRoom = new VoiceRoom();
        voiceRoom.setName(req.getName());
        voiceRoom.setRoomId(req.getRoomId());
        voiceRoom.setCreatorUid(uid);
        voiceRoom.setMaxUsers(req.getMaxUsers() != null ? req.getMaxUsers() : 10);
        voiceRoom.setCurrentUserCount(0);
        voiceRoom.setStatus(VoiceRoomStatusEnum.ACTIVE.getStatus());
        voiceRoomDao.save(voiceRoom);

        return buildVoiceRoomResp(voiceRoom, new ArrayList<>());
    }

    @Override
    @Transactional
    public VoiceRoomResp joinRoom(Long uid, Long voiceRoomId) {
        VoiceRoom voiceRoom = voiceRoomDao.getById(voiceRoomId);
        if (voiceRoom == null || !VoiceRoomStatusEnum.ACTIVE.getStatus().equals(voiceRoom.getStatus())) {
            throw new RuntimeException("语音房间不存在或已关闭");
        }

        VoiceRoomMember existingMember = voiceRoomMemberDao.getActiveMember(voiceRoomId, uid);
        if (existingMember != null) {
            return getRoomDetail(voiceRoomId);
        }

        if (voiceRoom.getCurrentUserCount() >= voiceRoom.getMaxUsers()) {
            throw new RuntimeException("语音房间已满");
        }

        VoiceRoomMember member = new VoiceRoomMember();
        member.setVoiceRoomId(voiceRoomId);
        member.setUid(uid);
        member.setMuted(0);
        member.setDeafened(0);
        member.setSpeaking(0);
        voiceRoomMemberDao.save(member);

        voiceRoomDao.incrementUserCount(voiceRoomId);

        List<VoiceRoomMember> members = voiceRoomMemberDao.getActiveMembers(voiceRoomId);
        VoiceRoomResp roomResp = buildVoiceRoomResp(voiceRoom, members);

        WSVoiceRoomUpdate update = new WSVoiceRoomUpdate();
        update.setVoiceRoomId(voiceRoomId);
        update.setAction("join");
        update.setMember(buildMemberResp(uid));
        webRTCSignalService.broadcastToRoom(voiceRoomId, uid, update);

        VoiceSignalHandler.bindVoiceRoom(uid, voiceRoomId);

        log.info("用户 {} 加入语音房间 {}", uid, voiceRoomId);

        return roomResp;
    }

    @Override
    @Transactional
    public void leaveRoom(Long uid, Long voiceRoomId) {
        VoiceRoomMember member = voiceRoomMemberDao.getActiveMember(voiceRoomId, uid);
        if (member == null) {
            return;
        }

        voiceRoomMemberDao.leaveRoom(voiceRoomId, uid);
        voiceRoomDao.decrementUserCount(voiceRoomId);

        WSVoiceRoomUpdate update = new WSVoiceRoomUpdate();
        update.setVoiceRoomId(voiceRoomId);
        update.setAction("leave");
        update.setMember(buildMemberResp(uid));
        webRTCSignalService.broadcastToRoom(voiceRoomId, null, update);

        VoiceSignalHandler.unbindVoiceRoom(uid);

        log.info("用户 {} 离开语音房间 {}", uid, voiceRoomId);
    }

    @Override
    @Transactional
    public void leaveAllRooms(Long uid) {
        List<VoiceRoomMember> activeMemberships = voiceRoomMemberDao.lambdaQuery()
                .eq(VoiceRoomMember::getUid, uid)
                .isNull(VoiceRoomMember::getLeaveTime)
                .list();

        if (CollUtil.isEmpty(activeMemberships)) {
            return;
        }

        for (VoiceRoomMember membership : activeMemberships) {
            voiceRoomMemberDao.leaveRoom(membership.getVoiceRoomId(), uid);
            voiceRoomDao.decrementUserCount(membership.getVoiceRoomId());

            WSVoiceRoomUpdate update = new WSVoiceRoomUpdate();
            update.setVoiceRoomId(membership.getVoiceRoomId());
            update.setAction("leave");
            update.setMember(buildMemberResp(uid));
            webRTCSignalService.broadcastToRoom(membership.getVoiceRoomId(), null, update);
        }

        VoiceSignalHandler.unbindVoiceRoom(uid);

        log.info("用户 {} 断开连接，已清理 {} 个语音房间", uid, activeMemberships.size());
    }

    @Override
    public List<VoiceRoomResp> getActiveRooms() {
        List<VoiceRoom> rooms = voiceRoomDao.getActiveRooms();
        return rooms.stream()
                .map(room -> {
                    List<VoiceRoomMember> members = voiceRoomMemberDao.getActiveMembers(room.getId());
                    return buildVoiceRoomResp(room, members);
                })
                .collect(Collectors.toList());
    }

    @Override
    public VoiceRoomResp getRoomDetail(Long voiceRoomId) {
        VoiceRoom voiceRoom = voiceRoomDao.getById(voiceRoomId);
        if (voiceRoom == null) {
            return null;
        }
        List<VoiceRoomMember> members = voiceRoomMemberDao.getActiveMembers(voiceRoomId);
        return buildVoiceRoomResp(voiceRoom, members);
    }

    @Override
    public void updateMemberStatus(Long uid, Long voiceRoomId, Boolean muted, Boolean deafened, Boolean speaking) {
        VoiceRoomMember member = voiceRoomMemberDao.getActiveMember(voiceRoomId, uid);
        if (member == null) {
            return;
        }

        if (muted != null) {
            member.setMuted(muted ? 1 : 0);
        }
        if (deafened != null) {
            member.setDeafened(deafened ? 1 : 0);
        }
        if (speaking != null) {
            member.setSpeaking(speaking ? 1 : 0);
        }
        voiceRoomMemberDao.updateById(member);

        WSVoiceRoomUpdate update = new WSVoiceRoomUpdate();
        update.setVoiceRoomId(voiceRoomId);
        update.setAction("status");
        update.setMember(buildMemberResp(uid));
        webRTCSignalService.broadcastToRoom(voiceRoomId, uid, update);
    }

    private VoiceRoomResp buildVoiceRoomResp(VoiceRoom voiceRoom, List<VoiceRoomMember> members) {
        VoiceRoomResp resp = new VoiceRoomResp();
        resp.setId(voiceRoom.getId());
        resp.setName(voiceRoom.getName());
        resp.setRoomId(voiceRoom.getRoomId());
        resp.setCreatorUid(voiceRoom.getCreatorUid());
        resp.setMaxUsers(voiceRoom.getMaxUsers());
        resp.setCurrentUserCount(voiceRoom.getCurrentUserCount());

        if (CollUtil.isNotEmpty(members)) {
            List<Long> uids = members.stream()
                    .map(VoiceRoomMember::getUid)
                    .collect(Collectors.toList());
            List<User> users = userDao.listByIds(uids);
            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            List<VoiceMemberResp> memberResps = members.stream()
                    .map(m -> {
                        VoiceMemberResp memberResp = new VoiceMemberResp();
                        memberResp.setUid(m.getUid());
                        User user = userMap.get(m.getUid());
                        if (user != null) {
                            memberResp.setName(user.getName());
                            memberResp.setAvatar(user.getAvatar());
                        }
                        memberResp.setMuted(m.getMuted());
                        memberResp.setDeafened(m.getDeafened());
                        memberResp.setSpeaking(m.getSpeaking());
                        return memberResp;
                    })
                    .collect(Collectors.toList());
            resp.setMembers(memberResps);
        } else {
            resp.setMembers(new ArrayList<>());
        }

        return resp;
    }

    private VoiceMemberResp buildMemberResp(Long uid) {
        User user = userDao.getById(uid);
        VoiceMemberResp resp = new VoiceMemberResp();
        resp.setUid(uid);
        if (user != null) {
            resp.setName(user.getName());
            resp.setAvatar(user.getAvatar());
        }
        return resp;
    }
}
