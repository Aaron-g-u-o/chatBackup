package com.abin.mallchat.common.guild.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("channel_member")
public class ChannelMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long channelId;
    
    private Long uid;
    
    private Integer muted;
    
    private Integer deafened;
    
    private Integer speaking;
    
    @TableField(fill = FieldFill.INSERT)
    private Date joinTime;
}
