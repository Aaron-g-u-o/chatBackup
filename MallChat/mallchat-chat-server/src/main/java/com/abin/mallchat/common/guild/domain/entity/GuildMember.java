package com.abin.mallchat.common.guild.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("guild_member")
public class GuildMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long guildId;
    
    private Long uid;
    
    private String nickname;
    
    private Integer roleId;
    
    @TableField(fill = FieldFill.INSERT)
    private Date joinTime;
    
    private Integer status;
}
