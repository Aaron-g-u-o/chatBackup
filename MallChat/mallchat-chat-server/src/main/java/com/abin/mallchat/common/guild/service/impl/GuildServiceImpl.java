package com.abin.mallchat.common.guild.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.abin.mallchat.common.chat.dao.GroupMemberDao;
import com.abin.mallchat.common.chat.dao.RoomDao;
import com.abin.mallchat.common.chat.dao.RoomGroupDao;
import com.abin.mallchat.common.chat.domain.entity.GroupMember;
import com.abin.mallchat.common.chat.domain.entity.Room;
import com.abin.mallchat.common.chat.domain.entity.RoomGroup;
import com.abin.mallchat.common.chat.domain.enums.GroupRoleEnum;
import com.abin.mallchat.common.chat.domain.enums.HotFlagEnum;
import com.abin.mallchat.common.chat.domain.enums.RoomTypeEnum;
import com.abin.mallchat.common.guild.dao.ChannelDao;
import com.abin.mallchat.common.guild.dao.ChannelMemberDao;
import com.abin.mallchat.common.guild.dao.GuildDao;
import com.abin.mallchat.common.guild.dao.GuildMemberDao;
import com.abin.mallchat.common.guild.domain.entity.Channel;
import com.abin.mallchat.common.guild.domain.entity.ChannelMember;
import com.abin.mallchat.common.guild.domain.entity.Guild;
import com.abin.mallchat.common.guild.domain.entity.GuildMember;
import com.abin.mallchat.common.guild.domain.enums.ChannelTypeEnum;
import com.abin.mallchat.common.guild.domain.enums.GuildMemberRoleEnum;
import com.abin.mallchat.common.guild.domain.vo.request.CreateChannelReq;
import com.abin.mallchat.common.guild.domain.vo.request.CreateGuildReq;
import com.abin.mallchat.common.guild.domain.vo.response.ChannelMemberResp;
import com.abin.mallchat.common.guild.domain.vo.response.ChannelResp;
import com.abin.mallchat.common.guild.domain.vo.response.GuildResp;
import com.abin.mallchat.common.guild.service.GuildService;
import com.abin.mallchat.common.guild.service.GuildVoiceBroadcastService;
import com.abin.mallchat.common.user.dao.UserDao;
import com.abin.mallchat.common.user.domain.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GuildServiceImpl implements GuildService {
    
    @Autowired
    private GuildDao guildDao;
    
    @Autowired
    private GuildMemberDao guildMemberDao;
    
    @Autowired
    private ChannelDao channelDao;
    
    @Autowired
    private ChannelMemberDao channelMemberDao;
    
    @Autowired
    private UserDao userDao;
    
    @Autowired
    private RoomDao roomDao;
    
    @Autowired
    private RoomGroupDao roomGroupDao;
    
    @Autowired
    private GroupMemberDao groupMemberDao;
    
    @Autowired
    private GuildVoiceBroadcastService guildVoiceBroadcastService;
    
    @Override
    @Transactional
    public GuildResp createGuild(Long uid, CreateGuildReq req) {
        Guild guild = new Guild();
        guild.setName(req.getName());
        guild.setIcon(req.getIcon());
        guild.setDescription(req.getDescription());
        guild.setOwnerUid(uid);
        guild.setMaxMembers(100);
        guild.setMemberCount(1);
        guild.setIsPublic(req.getIsPublic() != null ? req.getIsPublic() : 1);
        guild.setInviteCode(generateInviteCode());
        guild.setStatus(1);
        guildDao.save(guild);
        
        GuildMember member = new GuildMember();
        member.setGuildId(guild.getId());
        member.setUid(uid);
        member.setRoleId(GuildMemberRoleEnum.OWNER.getRoleId());
        member.setStatus(1);
        guildMemberDao.save(member);
        
        Channel textCategory = createCategory(guild.getId(), "文字频道", 0);
        Channel textChannel = createTextChannel(guild.getId(), textCategory.getId(), "综合讨论", 1);
        
        Channel voiceCategory = createCategory(guild.getId(), "语音频道", 2);
        createVoiceChannel(guild.getId(), voiceCategory.getId(), "语音大厅", 3);
        
        joinTextChannelsRoomGroup(guild.getId(), uid, GroupRoleEnum.LEADER.getType());
        
        return buildGuildResp(guild, uid);
    }
    
    private String generateInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
    
    private Channel createCategory(Long guildId, String name, int position) {
        Channel category = new Channel();
        category.setGuildId(guildId);
        category.setName(name);
        category.setType(ChannelTypeEnum.CATEGORY.getType());
        category.setPosition(position);
        category.setStatus(1);
        channelDao.save(category);
        return category;
    }
    
    private Channel createTextChannel(Long guildId, Long parentId, String name, int position) {
        Room room = new Room();
        room.setType(RoomTypeEnum.GROUP.getType());
        room.setHotFlag(HotFlagEnum.NOT.getType());
        roomDao.save(room);
        
        RoomGroup roomGroup = new RoomGroup();
        roomGroup.setRoomId(room.getId());
        roomGroup.setName(name);
        roomGroup.setAvatar("https://img.zbin.cn/group_default.png");
        roomGroupDao.save(roomGroup);
        
        Channel channel = new Channel();
        channel.setGuildId(guildId);
        channel.setParentId(parentId);
        channel.setName(name);
        channel.setType(ChannelTypeEnum.TEXT.getType());
        channel.setPosition(position);
        channel.setRoomId(room.getId());
        channel.setStatus(1);
        channelDao.save(channel);
        return channel;
    }
    
    private Channel createVoiceChannel(Long guildId, Long parentId, String name, int position) {
        Channel channel = new Channel();
        channel.setGuildId(guildId);
        channel.setParentId(parentId);
        channel.setName(name);
        channel.setType(ChannelTypeEnum.VOICE.getType());
        channel.setMaxUsers(0);
        channel.setPosition(position);
        channel.setStatus(1);
        channelDao.save(channel);
        return channel;
    }
    
    @Override
    public GuildResp getGuildDetail(Long guildId) {
        Guild guild = guildDao.getById(guildId);
        if (guild == null) {
            return null;
        }
        return buildGuildResp(guild, null);
    }
    
    @Override
    public List<GuildResp> getUserGuilds(Long uid) {
        List<Guild> guilds = guildDao.getGuildsByUid(uid);
        return guilds.stream()
                .map(g -> {
                    try {
                        return buildGuildResp(g, uid);
                    } catch (Exception e) {
                        log.error("buildGuildResp error for guild: {}", g.getId(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<GuildResp> getPublicGuilds(Long uid, int page, int pageSize) {
        List<Guild> allPublicGuilds = guildDao.lambdaQuery()
                .eq(Guild::getIsPublic, 1)
                .eq(Guild::getStatus, 1)
                .orderByDesc(Guild::getMemberCount)
                .list();
        
        List<Long> joinedGuildIds = guildMemberDao.lambdaQuery()
                .eq(GuildMember::getUid, uid)
                .eq(GuildMember::getStatus, 1)
                .list()
                .stream()
                .map(GuildMember::getGuildId)
                .collect(Collectors.toList());
        
        List<Guild> publicGuilds = allPublicGuilds.stream()
                .filter(g -> !joinedGuildIds.contains(g.getId()))
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
        
        return publicGuilds.stream()
                .map(g -> buildGuildResp(g, uid))
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void joinGuild(Long uid, Long guildId) {
        Guild guild = guildDao.getById(guildId);
        if (guild == null) {
            throw new RuntimeException("服务器不存在");
        }
        
        if (guild.getIsPublic() != 1) {
            throw new RuntimeException("该服务器为私密服务器，需要邀请码才能加入");
        }
        
        GuildMember existing = guildMemberDao.getMember(guildId, uid);
        if (existing != null) {
            return;
        }
        
        GuildMember member = new GuildMember();
        member.setGuildId(guildId);
        member.setUid(uid);
        member.setRoleId(GuildMemberRoleEnum.MEMBER.getRoleId());
        member.setStatus(1);
        guildMemberDao.save(member);
        
        guild.setMemberCount(guild.getMemberCount() + 1);
        guildDao.updateById(guild);
        
        joinTextChannelsRoomGroup(guildId, uid, GroupRoleEnum.MEMBER.getType());
    }
    
    @Override
    @Transactional
    public GuildResp joinGuildByInviteCode(Long uid, String inviteCode) {
        Guild guild = guildDao.lambdaQuery()
                .eq(Guild::getInviteCode, inviteCode)
                .eq(Guild::getStatus, 1)
                .one();
        
        if (guild == null) {
            throw new RuntimeException("邀请码无效或服务器不存在");
        }
        
        GuildMember existing = guildMemberDao.getMember(guild.getId(), uid);
        if (existing != null) {
            return buildGuildResp(guild, uid);
        }
        
        GuildMember member = new GuildMember();
        member.setGuildId(guild.getId());
        member.setUid(uid);
        member.setRoleId(GuildMemberRoleEnum.MEMBER.getRoleId());
        member.setStatus(1);
        guildMemberDao.save(member);
        
        guild.setMemberCount(guild.getMemberCount() + 1);
        guildDao.updateById(guild);
        
        joinTextChannelsRoomGroup(guild.getId(), uid, GroupRoleEnum.MEMBER.getType());
        
        return buildGuildResp(guild, uid);
    }
    
    @Override
    @Transactional
    public void leaveGuild(Long uid, Long guildId) {
        Guild guild = guildDao.getById(guildId);
        if (guild != null && guild.getOwnerUid().equals(uid)) {
            throw new RuntimeException("服务器创建者不能退出服务器");
        }
        
        guildMemberDao.lambdaUpdate()
                .eq(GuildMember::getGuildId, guildId)
                .eq(GuildMember::getUid, uid)
                .set(GuildMember::getStatus, 0)
                .update();
    }
    
    @Override
    @Transactional
    public ChannelResp createChannel(Long uid, CreateChannelReq req) {
        GuildMember member = guildMemberDao.getMember(req.getGuildId(), uid);
        if (member == null || member.getRoleId() < GuildMemberRoleEnum.ADMIN.getRoleId()) {
            throw new RuntimeException("没有权限创建频道");
        }
        
        Channel channel = new Channel();
        channel.setGuildId(req.getGuildId());
        channel.setParentId(req.getParentId());
        channel.setName(req.getName());
        channel.setType(req.getType());
        channel.setTopic(req.getTopic());
        channel.setMaxUsers(req.getMaxUsers() != null ? req.getMaxUsers() : 0);
        channel.setPosition(0);
        channel.setStatus(1);
        
        if (req.getType() == ChannelTypeEnum.TEXT.getType()) {
            Room room = new Room();
            room.setType(RoomTypeEnum.GROUP.getType());
            room.setHotFlag(HotFlagEnum.NOT.getType());
            roomDao.save(room);
            
            RoomGroup roomGroup = new RoomGroup();
            roomGroup.setRoomId(room.getId());
            roomGroup.setName(req.getName());
            roomGroup.setAvatar("https://img.zbin.cn/group_default.png");
            roomGroupDao.save(roomGroup);
            
            channel.setRoomId(room.getId());
            
            addAllGuildMembersToRoomGroup(req.getGuildId(), room.getId());
        }
        
        channelDao.save(channel);
        
        return buildChannelResp(channel, new ArrayList<>());
    }
    
    private void addAllGuildMembersToRoomGroup(Long guildId, Long roomId) {
        RoomGroup roomGroup = roomGroupDao.getByRoomId(roomId);
        if (roomGroup == null) {
            return;
        }
        List<GuildMember> guildMembers = guildMemberDao.getMembersByGuildId(guildId);
        for (GuildMember guildMember : guildMembers) {
            GroupMember existingMember = groupMemberDao.getMember(roomGroup.getId(), guildMember.getUid());
            if (existingMember == null) {
                GroupMember groupMember = new GroupMember();
                groupMember.setGroupId(roomGroup.getId());
                groupMember.setUid(guildMember.getUid());
                Integer role = guildMember.getRoleId() == GuildMemberRoleEnum.OWNER.getRoleId() 
                    ? GroupRoleEnum.LEADER.getType() 
                    : GroupRoleEnum.MEMBER.getType();
                groupMember.setRole(role);
                groupMemberDao.save(groupMember);
            }
        }
    }
    
    @Override
    @Transactional
    public void deleteChannel(Long uid, Long channelId) {
        Channel channel = channelDao.getById(channelId);
        if (channel == null) {
            return;
        }
        
        GuildMember member = guildMemberDao.getMember(channel.getGuildId(), uid);
        if (member == null || member.getRoleId() < GuildMemberRoleEnum.ADMIN.getRoleId()) {
            throw new RuntimeException("没有权限删除频道");
        }
        
        channel.setStatus(0);
        channelDao.updateById(channel);
    }
    
    @Override
    public List<ChannelResp> getGuildChannels(Long guildId) {
        List<Channel> channels = channelDao.getChannelsByGuildId(guildId);
        
        for (Channel channel : channels) {
            if (ChannelTypeEnum.TEXT.getType().equals(channel.getType()) && channel.getRoomId() == null) {
                Room room = new Room();
                room.setType(RoomTypeEnum.GROUP.getType());
                room.setHotFlag(HotFlagEnum.NOT.getType());
                roomDao.save(room);
                
                RoomGroup roomGroup = new RoomGroup();
                roomGroup.setRoomId(room.getId());
                roomGroup.setName(channel.getName() != null ? channel.getName() : "频道");
                roomGroup.setAvatar("https://img.zbin.cn/group_default.png");
                roomGroupDao.save(roomGroup);
                
                channel.setRoomId(room.getId());
                channelDao.updateById(channel);
            }
        }
        
        return buildChannelTree(channels);
    }
    
    @Override
    @Transactional
    public void joinVoiceChannel(Long uid, Long channelId) {
        Channel channel = channelDao.getById(channelId);
        if (channel == null || !ChannelTypeEnum.VOICE.getType().equals(channel.getType())) {
            throw new RuntimeException("频道不存在或不是语音频道");
        }
        
        ChannelMember existing = channelMemberDao.getMember(channelId, uid);
        if (existing != null) {
            return;
        }
        
        ChannelMember member = new ChannelMember();
        member.setChannelId(channelId);
        member.setUid(uid);
        member.setMuted(0);
        member.setDeafened(0);
        member.setSpeaking(0);
        member.setVolume(100);
        channelMemberDao.save(member);
        
        ChannelMemberResp memberResp = buildChannelMemberResp(member);
        guildVoiceBroadcastService.broadcastVoiceChannelUpdate(channelId, "join", memberResp);
        
        log.info("用户 {} 加入语音频道 {}", uid, channelId);
    }
    
    @Override
    @Transactional
    public void leaveVoiceChannel(Long uid, Long channelId) {
        ChannelMember member = channelMemberDao.getMember(channelId, uid);
        if (member == null) {
            return;
        }
        
        ChannelMemberResp memberResp = buildChannelMemberResp(member);
        
        channelMemberDao.removeMember(channelId, uid);
        
        guildVoiceBroadcastService.broadcastVoiceChannelUpdate(channelId, "leave", memberResp);
        
        log.info("用户 {} 离开语音频道 {}", uid, channelId);
    }
    
    @Override
    public List<ChannelResp> getVoiceChannelMembers(Long channelId) {
        List<ChannelMember> members = channelMemberDao.getMembersByChannelId(channelId);
        if (CollUtil.isEmpty(members)) {
            return new ArrayList<>();
        }
        
        List<Long> uids = members.stream().map(ChannelMember::getUid).collect(Collectors.toList());
        List<User> users = userDao.listByIds(uids);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        
        return members.stream().map(m -> {
            ChannelMemberResp resp = new ChannelMemberResp();
            resp.setUid(m.getUid());
            resp.setMuted(m.getMuted());
            resp.setDeafened(m.getDeafened());
            resp.setSpeaking(m.getSpeaking());
            User user = userMap.get(m.getUid());
            if (user != null) {
                resp.setName(user.getName());
                resp.setAvatar(user.getAvatar());
            }
            return buildChannelResp(channelDao.getById(channelId), Collections.singletonList(resp));
        }).collect(Collectors.toList());
    }
    
    private GuildResp buildGuildResp(Guild guild, Long currentUid) {
        GuildResp resp = new GuildResp();
        resp.setId(guild.getId());
        resp.setName(guild.getName());
        resp.setIcon(guild.getIcon());
        resp.setDescription(guild.getDescription());
        resp.setOwnerUid(guild.getOwnerUid());
        resp.setMemberCount(guild.getMemberCount());
        resp.setIsPublic(guild.getIsPublic());
        resp.setInviteCode(guild.getInviteCode());
        
        List<Channel> channels = channelDao.getChannelsByGuildId(guild.getId());
        ensureTextChannelRoomId(channels);
        
        if (currentUid != null) {
            ensureUserInTextChannelsRoomGroup(channels, currentUid);
        }
        
        resp.setChannels(buildChannelTree(channels));
        
        return resp;
    }
    
    @Transactional
    private void ensureTextChannelRoomId(List<Channel> channels) {
        for (Channel channel : channels) {
            try {
                if (ChannelTypeEnum.TEXT.getType().equals(channel.getType()) && channel.getRoomId() == null) {
                    Room room = new Room();
                    room.setType(RoomTypeEnum.GROUP.getType());
                    room.setHotFlag(HotFlagEnum.NOT.getType());
                    roomDao.save(room);
                    
                    RoomGroup roomGroup = new RoomGroup();
                    roomGroup.setRoomId(room.getId());
                    roomGroup.setName(channel.getName() != null ? channel.getName() : "频道");
                    roomGroup.setAvatar("https://img.zbin.cn/group_default.png");
                    roomGroupDao.save(roomGroup);
                    
                    channel.setRoomId(room.getId());
                    channelDao.updateById(channel);
                }
            } catch (Exception e) {
                log.error("ensureTextChannelRoomId error for channel: {}", channel.getId(), e);
            }
        }
    }
    
    private List<ChannelResp> buildChannelTree(List<Channel> channels) {
        Map<Long, List<Channel>> parentMap = channels.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() != null ? c.getParentId() : 0L));
        
        List<Channel> rootChannels = parentMap.getOrDefault(0L, new ArrayList<>());
        
        return rootChannels.stream().map(c -> {
            ChannelResp resp = buildChannelResp(c, new ArrayList<>());
            List<Channel> children = parentMap.getOrDefault(c.getId(), new ArrayList<>());
            resp.setChildren(children.stream()
                    .map(child -> buildChannelResp(child, new ArrayList<>()))
                    .collect(Collectors.toList()));
            return resp;
        }).collect(Collectors.toList());
    }
    
    private ChannelResp buildChannelResp(Channel channel, List<ChannelMemberResp> members) {
        ChannelResp resp = new ChannelResp();
        resp.setId(channel.getId());
        resp.setGuildId(channel.getGuildId());
        resp.setParentId(channel.getParentId());
        resp.setName(channel.getName());
        resp.setType(channel.getType());
        resp.setTopic(channel.getTopic());
        resp.setPosition(channel.getPosition());
        resp.setMaxUsers(channel.getMaxUsers());
        resp.setRoomId(channel.getRoomId());
        resp.setMembers(members);
        return resp;
    }
    
    private void joinTextChannelsRoomGroup(Long guildId, Long uid, Integer role) {
        List<Channel> textChannels = channelDao.getTextChannels(guildId);
        for (Channel channel : textChannels) {
            if (channel.getRoomId() != null) {
                RoomGroup roomGroup = roomGroupDao.getByRoomId(channel.getRoomId());
                if (roomGroup != null) {
                    GroupMember existingMember = groupMemberDao.getMember(roomGroup.getId(), uid);
                    if (existingMember == null) {
                        GroupMember groupMember = new GroupMember();
                        groupMember.setGroupId(roomGroup.getId());
                        groupMember.setUid(uid);
                        groupMember.setRole(role);
                        groupMemberDao.save(groupMember);
                    }
                }
            }
        }
    }
    
    private void ensureUserInTextChannelsRoomGroup(List<Channel> channels, Long uid) {
        for (Channel channel : channels) {
            if (ChannelTypeEnum.TEXT.getType().equals(channel.getType()) && channel.getRoomId() != null) {
                try {
                    RoomGroup roomGroup = roomGroupDao.getByRoomId(channel.getRoomId());
                    if (roomGroup != null) {
                        GroupMember existingMember = groupMemberDao.getMember(roomGroup.getId(), uid);
                        if (existingMember == null) {
                            GroupMember groupMember = new GroupMember();
                            groupMember.setGroupId(roomGroup.getId());
                            groupMember.setUid(uid);
                            groupMember.setRole(GroupRoleEnum.MEMBER.getType());
                            groupMemberDao.save(groupMember);
                        }
                    }
                } catch (Exception e) {
                    log.error("ensureUserInTextChannelsRoomGroup error for channel: {}", channel.getId(), e);
                }
            }
        }
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
}
