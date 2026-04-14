package com.abin.mallchat.common.voice.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("voice_room_member")
public class VoiceRoomMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long voiceRoomId;
    
    private Long uid;
    
    private Integer muted;
    
    private Integer deafened;
    
    private Integer speaking;
    
    @TableField(fill = FieldFill.INSERT)
    private Date joinTime;
    
    private Date leaveTime;
}
