package com.abin.mallchat.common.guild.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("channel")
public class Channel {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long guildId;
    
    private Long parentId;
    
    private String name;
    
    private Integer type;
    
    private String topic;
    
    private Integer position;
    
    private Integer maxUsers;
    
    private Long roomId;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
