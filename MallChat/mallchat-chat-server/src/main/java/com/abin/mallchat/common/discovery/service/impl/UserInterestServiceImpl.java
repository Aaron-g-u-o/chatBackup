package com.abin.mallchat.common.discovery.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.abin.mallchat.common.discovery.dao.*;
import com.abin.mallchat.common.discovery.domain.entity.*;
import com.abin.mallchat.common.discovery.domain.enums.InterestSourceEnum;
import com.abin.mallchat.common.discovery.domain.enums.PrivacyLevelEnum;
import com.abin.mallchat.common.discovery.domain.vo.request.UpdateInterestTagsReq;
import com.abin.mallchat.common.discovery.domain.vo.request.UpdatePrivacyReq;
import com.abin.mallchat.common.discovery.domain.vo.response.UserInterestResp;
import com.abin.mallchat.common.discovery.domain.vo.response.UserInterestTagResp;
import com.abin.mallchat.common.discovery.service.UserInterestService;
import com.abin.mallchat.common.guild.dao.GuildDao;
import com.abin.mallchat.common.guild.dao.GuildMemberDao;
import com.abin.mallchat.common.guild.domain.entity.Guild;
import com.abin.mallchat.common.guild.domain.entity.GuildMember;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserInterestServiceImpl implements UserInterestService {
    
    @Autowired
    private UserInterestProfileDao userInterestProfileDao;
    
    @Autowired
    private UserInterestTagDao userInterestTagDao;
    
    @Autowired
    private GuildTagDao guildTagDao;
    
    @Autowired
    private GuildTagRelationDao guildTagRelationDao;
    
    @Autowired
    private GuildDao guildDao;
    
    @Autowired
    private GuildMemberDao guildMemberDao;
    
    private static final double JOIN_WEIGHT = 0.4;
    private static final double ACTIVITY_WEIGHT = 0.3;
    private static final double MESSAGE_WEIGHT = 0.2;
    private static final double MANUAL_WEIGHT = 0.1;
    
    @Override
    @Transactional
    public void buildUserProfile(Long uid) {
        UserInterestProfile profile = userInterestProfileDao.getByUid(uid);
        if (profile == null) {
            profile = new UserInterestProfile();
            profile.setUid(uid);
            profile.setActivityScore(0.0);
            profile.setDiversityScore(0.5);
            profile.setPrivacyLevel(PrivacyLevelEnum.FULL_PERSONALIZED.getLevel());
            profile.setInterestVector(new ArrayList<>());
            userInterestProfileDao.save(profile);
        }
        
        deriveInterestsFromGuildJoins(uid);
        calculateActivityScore(uid);
        calculateDiversityScore(uid);
        buildInterestVector(uid);
    }
    
    @Override
    @Transactional
    public void updateOnGuildJoin(Long uid, Long guildId) {
        List<GuildTagRelation> tagRelations = guildTagRelationDao.getByGuildId(guildId);
        for (GuildTagRelation relation : tagRelations) {
            UserInterestTag existing = userInterestTagDao.getByUidAndTagId(uid, relation.getTagId());
            if (existing != null) {
                double newWeight = existing.getWeight() + JOIN_WEIGHT * relation.getRelevanceScore();
                existing.setWeight(Math.min(1.0, newWeight));
                existing.setUpdateTime(new Date());
                userInterestTagDao.updateById(existing);
            } else {
                UserInterestTag newTag = new UserInterestTag();
                newTag.setUid(uid);
                newTag.setTagId(relation.getTagId());
                newTag.setWeight(JOIN_WEIGHT * relation.getRelevanceScore());
                newTag.setSource(InterestSourceEnum.GUILD_JOIN.getType());
                newTag.setUpdateTime(new Date());
                userInterestTagDao.save(newTag);
            }
        }
        
        refreshUserProfile(uid);
    }
    
    @Override
    @Transactional
    public void updateOnGuildLeave(Long uid, Long guildId) {
        List<GuildTagRelation> tagRelations = guildTagRelationDao.getByGuildId(guildId);
        for (GuildTagRelation relation : tagRelations) {
            UserInterestTag existing = userInterestTagDao.getByUidAndTagId(uid, relation.getTagId());
            if (existing != null) {
                double newWeight = existing.getWeight() - JOIN_WEIGHT * relation.getRelevanceScore() * 0.5;
                if (newWeight <= 0.01) {
                    userInterestTagDao.removeById(existing.getId());
                } else {
                    existing.setWeight(newWeight);
                    existing.setUpdateTime(new Date());
                    userInterestTagDao.updateById(existing);
                }
            }
        }
        
        refreshUserProfile(uid);
    }
    
    @Override
    @Transactional
    public void updateOnUserActivity(Long uid, Long guildId, String activityType) {
        List<GuildTagRelation> tagRelations = guildTagRelationDao.getByGuildId(guildId);
        double weight = "message".equals(activityType) ? MESSAGE_WEIGHT : ACTIVITY_WEIGHT;
        int source = "message".equals(activityType) 
                ? InterestSourceEnum.MESSAGE_CONTENT.getType() 
                : InterestSourceEnum.ACTIVITY.getType();
        
        for (GuildTagRelation relation : tagRelations) {
            UserInterestTag existing = userInterestTagDao.getByUidAndTagId(uid, relation.getTagId());
            if (existing != null) {
                double newWeight = existing.getWeight() + weight * relation.getRelevanceScore() * 0.1;
                existing.setWeight(Math.min(1.0, newWeight));
                existing.setUpdateTime(new Date());
                userInterestTagDao.updateById(existing);
            } else {
                UserInterestTag newTag = new UserInterestTag();
                newTag.setUid(uid);
                newTag.setTagId(relation.getTagId());
                newTag.setWeight(weight * relation.getRelevanceScore() * 0.1);
                newTag.setSource(source);
                newTag.setUpdateTime(new Date());
                userInterestTagDao.save(newTag);
            }
        }
    }
    
    @Override
    public UserInterestResp getUserInterest(Long uid) {
        UserInterestProfile profile = userInterestProfileDao.getByUid(uid);
        List<UserInterestTag> tags = userInterestTagDao.getByUid(uid);
        
        UserInterestResp resp = new UserInterestResp();
        if (profile != null) {
            resp.setPrivacyLevel(profile.getPrivacyLevel());
            resp.setDiversityPreference(profile.getDiversityScore());
        } else {
            resp.setPrivacyLevel(PrivacyLevelEnum.FULL_PERSONALIZED.getLevel());
            resp.setDiversityPreference(0.5);
        }
        
        List<GuildTag> allTags = guildTagDao.list();
        Map<Long, GuildTag> tagMap = allTags.stream()
                .collect(Collectors.toMap(GuildTag::getId, t -> t));
        
        List<UserInterestTagResp> tagResps = tags.stream().map(t -> {
            UserInterestTagResp tagResp = new UserInterestTagResp();
            tagResp.setTagId(t.getTagId());
            GuildTag guildTag = tagMap.get(t.getTagId());
            if (guildTag != null) {
                tagResp.setTagName(guildTag.getName());
                tagResp.setCategory(guildTag.getCategory());
            }
            tagResp.setWeight(t.getWeight());
            tagResp.setSource(t.getSource());
            return tagResp;
        }).collect(Collectors.toList());
        
        resp.setInterestTags(tagResps);
        return resp;
    }
    
    @Override
    @Transactional
    public void updatePrivacy(Long uid, UpdatePrivacyReq req) {
        UserInterestProfile profile = userInterestProfileDao.getByUid(uid);
        if (profile == null) {
            profile = new UserInterestProfile();
            profile.setUid(uid);
            profile.setActivityScore(0.0);
            profile.setDiversityScore(0.5);
            profile.setInterestVector(new ArrayList<>());
        }
        
        if (req.getPrivacyLevel() != null) {
            profile.setPrivacyLevel(req.getPrivacyLevel());
        }
        if (req.getDiversityPreference() != null) {
            profile.setDiversityScore(req.getDiversityPreference());
        }
        
        userInterestProfileDao.saveOrUpdate(profile);
    }
    
    @Override
    @Transactional
    public void updateManualInterestTags(Long uid, UpdateInterestTagsReq req) {
        userInterestTagDao.removeByUidAndSource(uid, InterestSourceEnum.MANUAL.getType());
        
        if (CollUtil.isNotEmpty(req.getTagIds())) {
            double weightPerTag = MANUAL_WEIGHT;
            for (Long tagId : req.getTagIds()) {
                UserInterestTag existing = userInterestTagDao.getByUidAndTagId(uid, tagId);
                if (existing != null) {
                    double newWeight = existing.getWeight() + weightPerTag;
                    existing.setWeight(Math.min(1.0, newWeight));
                    existing.setUpdateTime(new Date());
                    userInterestTagDao.updateById(existing);
                } else {
                    UserInterestTag newTag = new UserInterestTag();
                    newTag.setUid(uid);
                    newTag.setTagId(tagId);
                    newTag.setWeight(weightPerTag);
                    newTag.setSource(InterestSourceEnum.MANUAL.getType());
                    newTag.setUpdateTime(new Date());
                    userInterestTagDao.save(newTag);
                }
            }
        }
        
        refreshUserProfile(uid);
    }
    
    @Override
    @Transactional
    public void refreshUserProfile(Long uid) {
        calculateActivityScore(uid);
        calculateDiversityScore(uid);
        buildInterestVector(uid);
    }
    
    private void deriveInterestsFromGuildJoins(Long uid) {
        List<GuildMember> memberships = guildMemberDao.lambdaQuery()
                .eq(GuildMember::getUid, uid)
                .eq(GuildMember::getStatus, 1)
                .list();
        
        if (CollUtil.isEmpty(memberships)) {
            return;
        }
        
        List<Long> guildIds = memberships.stream()
                .map(GuildMember::getGuildId)
                .collect(Collectors.toList());
        
        Map<Long, Integer> tagFrequency = new HashMap<>();
        for (Long guildId : guildIds) {
            List<GuildTagRelation> relations = guildTagRelationDao.getByGuildId(guildId);
            for (GuildTagRelation relation : relations) {
                tagFrequency.merge(relation.getTagId(), 1, Integer::sum);
            }
        }
        
        int totalGuilds = guildIds.size();
        for (Map.Entry<Long, Integer> entry : tagFrequency.entrySet()) {
            double weight = Math.min(1.0, (entry.getValue() / (double) totalGuilds) * JOIN_WEIGHT * 3);
            UserInterestTag existing = userInterestTagDao.getByUidAndTagId(uid, entry.getKey());
            if (existing != null) {
                existing.setWeight(Math.max(existing.getWeight(), weight));
                existing.setUpdateTime(new Date());
                userInterestTagDao.updateById(existing);
            } else {
                UserInterestTag newTag = new UserInterestTag();
                newTag.setUid(uid);
                newTag.setTagId(entry.getKey());
                newTag.setWeight(weight);
                newTag.setSource(InterestSourceEnum.GUILD_JOIN.getType());
                newTag.setUpdateTime(new Date());
                userInterestTagDao.save(newTag);
            }
        }
    }
    
    private void calculateActivityScore(Long uid) {
        List<GuildMember> memberships = guildMemberDao.lambdaQuery()
                .eq(GuildMember::getUid, uid)
                .eq(GuildMember::getStatus, 1)
                .list();
        
        double score = Math.min(1.0, memberships.size() / 10.0);
        
        UserInterestProfile profile = userInterestProfileDao.getByUid(uid);
        if (profile != null) {
            profile.setActivityScore(score);
            userInterestProfileDao.updateById(profile);
        }
    }
    
    private void calculateDiversityScore(Long uid) {
        List<UserInterestTag> tags = userInterestTagDao.getByUid(uid);
        if (CollUtil.isEmpty(tags)) {
            return;
        }
        
        List<GuildTag> allTags = guildTagDao.list();
        Map<Long, String> tagCategoryMap = allTags.stream()
                .collect(Collectors.toMap(GuildTag::getId, GuildTag::getCategory));
        
        Set<String> categories = tags.stream()
                .map(t -> tagCategoryMap.getOrDefault(t.getTagId(), "其他"))
                .collect(Collectors.toSet());
        
        double diversity = Math.min(1.0, categories.size() / 5.0);
        
        UserInterestProfile profile = userInterestProfileDao.getByUid(uid);
        if (profile != null) {
            double userPreference = profile.getDiversityScore() != null ? profile.getDiversityScore() : 0.5;
            profile.setDiversityScore(diversity * 0.5 + userPreference * 0.5);
            userInterestProfileDao.updateById(profile);
        }
    }
    
    private void buildInterestVector(Long uid) {
        List<GuildTag> allTags = guildTagDao.list();
        List<UserInterestTag> userTags = userInterestTagDao.getByUid(uid);
        
        Map<Long, Double> userTagWeights = userTags.stream()
                .collect(Collectors.toMap(UserInterestTag::getTagId, UserInterestTag::getWeight));
        
        List<Double> vector = allTags.stream()
                .map(tag -> userTagWeights.getOrDefault(tag.getId(), 0.0))
                .collect(Collectors.toList());
        
        double maxVal = vector.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxVal > 0) {
            vector = vector.stream().map(v -> v / maxVal).collect(Collectors.toList());
        }
        
        UserInterestProfile profile = userInterestProfileDao.getByUid(uid);
        if (profile != null) {
            profile.setInterestVector(vector);
            userInterestProfileDao.updateById(profile);
        }
    }
}
