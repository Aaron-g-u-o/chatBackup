package com.abin.mallchat.common.chatai.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("ai_session")
public class AISession {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long uid;
    
    private Long roomId;
    
    private Integer aiType;
    
    private String title;
    
    private String systemPrompt;
    
    private Integer contextWindow;
    
    private Integer totalTokens;
    
    private Integer messageCount;
    
    private Integer isActive;
    
    private Date lastMessageTime;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
