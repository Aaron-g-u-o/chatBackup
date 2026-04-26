package com.abin.mallchat.common.discovery.dao;

import com.abin.mallchat.common.discovery.domain.entity.UserInterestProfile;
import com.abin.mallchat.common.discovery.mapper.UserInterestProfileMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

@Repository
public class UserInterestProfileDao extends ServiceImpl<UserInterestProfileMapper, UserInterestProfile> {
    
    public UserInterestProfile getByUid(Long uid) {
        return lambdaQuery().eq(UserInterestProfile::getUid, uid).one();
    }
}
