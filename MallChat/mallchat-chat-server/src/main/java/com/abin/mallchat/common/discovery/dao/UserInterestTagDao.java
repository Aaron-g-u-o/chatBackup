package com.abin.mallchat.common.discovery.dao;

import com.abin.mallchat.common.discovery.domain.entity.UserInterestTag;
import com.abin.mallchat.common.discovery.mapper.UserInterestTagMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserInterestTagDao extends ServiceImpl<UserInterestTagMapper, UserInterestTag> {
    
    public List<UserInterestTag> getByUid(Long uid) {
        return lambdaQuery()
                .eq(UserInterestTag::getUid, uid)
                .orderByDesc(UserInterestTag::getWeight)
                .list();
    }
    
    public List<UserInterestTag> getByUidAndSource(Long uid, Integer source) {
        return lambdaQuery()
                .eq(UserInterestTag::getUid, uid)
                .eq(UserInterestTag::getSource, source)
                .orderByDesc(UserInterestTag::getWeight)
                .list();
    }
    
    public UserInterestTag getByUidAndTagId(Long uid, Long tagId) {
        return lambdaQuery()
                .eq(UserInterestTag::getUid, uid)
                .eq(UserInterestTag::getTagId, tagId)
                .one();
    }
    
    public void removeByUidAndSource(Long uid, Integer source) {
        lambdaUpdate()
                .eq(UserInterestTag::getUid, uid)
                .eq(UserInterestTag::getSource, source)
                .remove();
    }
    
    public void removeByUid(Long uid) {
        lambdaUpdate().eq(UserInterestTag::getUid, uid).remove();
    }
}
