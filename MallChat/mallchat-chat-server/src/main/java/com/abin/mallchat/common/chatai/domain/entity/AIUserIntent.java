package com.abin.mallchat.common.chatai.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("ai_user_intent")
public class AIUserIntent {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long uid;
    
    private Long sessionId;
    
    private String intentType;
    
    private String intentKeywords;
    
    private Double confidence;
    
    private String contextBefore;
    
    private String entities;
    
    private Integer followupNeeded;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
