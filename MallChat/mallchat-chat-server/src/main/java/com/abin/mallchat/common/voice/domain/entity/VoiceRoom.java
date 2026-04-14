package com.abin.mallchat.common.voice.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("voice_room")
public class VoiceRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private Long roomId;
    
    private Long creatorUid;
    
    private Integer maxUsers;
    
    private Integer currentUserCount;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
