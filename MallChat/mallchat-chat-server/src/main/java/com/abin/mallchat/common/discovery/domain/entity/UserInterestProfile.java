package com.abin.mallchat.common.discovery.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@TableName(value = "user_interest_profile", autoResultMap = true)
public class UserInterestProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long uid;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Double> interestVector;
    
    private Double activityScore;
    
    private Double diversityScore;
    
    private Integer privacyLevel;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
