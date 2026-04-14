package com.abin.mallchat.common.guild.mapper;

import com.abin.mallchat.common.guild.domain.entity.GuildMember;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GuildMemberMapper extends BaseMapper<GuildMember> {
}
