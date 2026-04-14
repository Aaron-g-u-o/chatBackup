package com.abin.mallchat.common.guild.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GuildMemberRoleEnum {
    MEMBER(0, "普通成员"),
    ADMIN(1, "管理员"),
    OWNER(2, "服务器主");
    
    private final Integer roleId;
    private final String desc;
}
