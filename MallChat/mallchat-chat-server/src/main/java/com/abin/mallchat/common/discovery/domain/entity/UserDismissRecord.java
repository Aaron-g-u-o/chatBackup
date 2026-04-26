package com.abin.mallchat.common.discovery.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_dismiss_record")
public class UserDismissRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long uid;
    
    private Long guildId;
    
    private String reason;
    
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
