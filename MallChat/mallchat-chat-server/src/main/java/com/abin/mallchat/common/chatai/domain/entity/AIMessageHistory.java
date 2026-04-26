package com.abin.mallchat.common.chatai.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("ai_message_history")
public class AIMessageHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long sessionId;
    
    private String role;
    
    private String content;
    
    private Integer tokenCount;
    
    private String intentTags;
    
    private String metadata;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
