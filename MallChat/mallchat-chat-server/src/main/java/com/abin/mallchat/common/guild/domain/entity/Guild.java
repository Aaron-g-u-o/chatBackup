package com.abin.mallchat.common.guild.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("guild")
public class Guild {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String icon;
    
    private String description;
    
    private Long ownerUid;
    
    private Integer maxMembers;
    
    private Integer memberCount;
    
    private Integer isPublic;
    
    private String inviteCode;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
