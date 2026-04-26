package com.abin.mallchat.common.discovery.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("guild_tag_relation")
public class GuildTagRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long guildId;
    
    private Long tagId;
    
    private Double relevanceScore;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
