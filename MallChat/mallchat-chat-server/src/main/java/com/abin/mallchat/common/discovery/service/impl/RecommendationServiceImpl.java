package com.abin.mallchat.common.discovery.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.abin.mallchat.common.discovery.dao.*;
import com.abin.mallchat.common.discovery.domain.entity.*;
import com.abin.mallchat.common.discovery.domain.enums.PrivacyLevelEnum;
import com.abin.mallchat.common.discovery.domain.enums.RecommendSourceEnum;
import com.abin.mallchat.common.discovery.domain.enums.RecommendTypeEnum;
import com.abin.mallchat.common.discovery.domain.vo.request.DismissReq;
import com.abin.mallchat.common.discovery.domain.vo.request.DiscoveryReq;
import com.abin.mallchat.common.discovery.domain.vo.request.TrackClickReq;
import com.abin.mallchat.common.discovery.domain.vo.response.*;
import com.abin.mallchat.common.discovery.service.RecommendationService;
import com.abin.mallchat.common.guild.dao.GuildDao;
import com.abin.mallchat.common.guild.dao.GuildMemberDao;
import com.abin.mallchat.common.guild.domain.entity.Guild;
import com.abin.mallchat.common.guild.domain.entity.GuildMember;
import com.abin.mallchat.common.common.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
    
    @Autowired
    private GuildDao guildDao;
    
    @Autowired
    private GuildMemberDao guildMemberDao;
    
    @Autowired
    private GuildTagDao guildTagDao;
    
    @Autowired
    private GuildTagRelationDao guildTagRelationDao;
    
    @Autowired
    private UserInterestProfileDao userInterestProfileDao;
    
    @Autowired
    private UserInterestTagDao userInterestTagDao;
    
    @Autowired
    private UserDismissRecordDao userDismissRecordDao;
    
    @Autowired
    private RecommendationTrackDao recommendationTrackDao;
    
    @Autowired
    private GuildActivityStatsDao guildActivityStatsDao;
    
    private static final String RECOMMEND_CACHE_PREFIX = "discovery:recommend:";
    private static final long CACHE_TTL_SECONDS = 300;
    
    @Override
    public DiscoveryPageResp getRecommendations(Long uid, DiscoveryReq req) {
        DiscoveryPageResp resp = new DiscoveryPageResp();
        
        String cacheKey = buildCacheKey(uid, req);
        DiscoveryPageResp cached = RedisUtils.get(cacheKey, DiscoveryPageResp.class);
        if (cached != null) {
            return cached;
        }
        
        List<RecommendedGuildResp> recommendations;
        RecommendTypeEnum type = RecommendTypeEnum.of(req.getRecommendType());
        
        UserInterestProfile profile = userInterestProfileDao.getByUid(uid);
        int privacyLevel = profile != null ? profile.getPrivacyLevel() : PrivacyLevelEnum.FULL_PERSONALIZED.getLevel();
        
        switch (type) {
            case PERSONALIZED:
                recommendations = getPersonalizedRecommendations(uid, req, privacyLevel);
                break;
            case POPULAR:
                recommendations = getPopularRecommendations(uid, req);
                break;
            case NEWEST:
                recommendations = getNewestRecommendations(uid, req);
                break;
            case TRENDING:
                recommendations = getTrendingRecommendations(uid, req);
                break;
            default:
                recommendations = getPersonalizedRecommendations(uid, req, privacyLevel);
        }
        
        recommendations = applyFilters(recommendations, req);
        recommendations = paginate(recommendations, req.getPage(), req.getPageSize());
        
        resp.setList(recommendations);
        resp.setIsLast(recommendations.size() < req.getPageSize());
        resp.setCategories(buildTagCategories());
        resp.setUserInterest(uid != null ? buildUserInterestResp(uid) : null);
        
        if (uid != null) {
            trackRecommendations(uid, recommendations, type.getType());
        }
        
        RedisUtils.set(cacheKey, resp, CACHE_TTL_SECONDS);
        return resp;
    }
    
    @Override
    @Transactional
    public void trackClick(Long uid, TrackClickReq req) {
        RecommendationTrack track = recommendationTrackDao.getByUidAndGuildId(uid, req.getGuildId());
        if (track != null) {
            track.setIsClicked(1);
            recommendationTrackDao.updateById(track);
        }
    }
    
    @Override
    @Transactional
    public void trackJoin(Long uid, Long guildId) {
        RecommendationTrack track = recommendationTrackDao.getByUidAndGuildId(uid, guildId);
        if (track != null) {
            track.setIsJoined(1);
            recommendationTrackDao.updateById(track);
        }
    }
    
    @Override
    @Transactional
    public void dismissRecommendation(Long uid, DismissReq req) {
        UserDismissRecord record = new UserDismissRecord();
        record.setUid(uid);
        record.setGuildId(req.getGuildId());
        record.setReason(req.getReason());
        userDismissRecordDao.saveOrUpdate(record);
        
        RecommendationTrack track = recommendationTrackDao.getByUidAndGuildId(uid, req.getGuildId());
        if (track != null) {
            track.setIsDismissed(1);
            recommendationTrackDao.updateById(track);
        }
        
        invalidateUserCache(uid);
    }
    
    @Override
    public RecommendationMetricsResp getMetrics(Long uid) {
        RecommendationMetricsResp resp = new RecommendationMetricsResp();
        Long total = recommendationTrackDao.countTotalByUid(uid);
        Long clicks = recommendationTrackDao.countClicksByUid(uid);
        Long joins = recommendationTrackDao.countJoinsByUid(uid);
        
        resp.setTotalRecommendations(total);
        resp.setTotalClicks(clicks);
        resp.setTotalJoins(joins);
        resp.setClickThroughRate(total > 0 ? clicks.doubleValue() / total : 0.0);
        resp.setConversionRate(clicks > 0 ? joins.doubleValue() / clicks : 0.0);
        resp.setAvgRelevanceScore(0.0);
        
        return resp;
    }
    
    @Override
    @Transactional
    public void refreshDailyRecommendations() {
        log.info("Starting daily recommendation refresh");
        List<String> keys = RedisUtils.scan(RECOMMEND_CACHE_PREFIX + "*");
        if (CollUtil.isNotEmpty(keys)) {
            RedisUtils.del(keys);
        }
        log.info("Daily recommendation refresh completed");
    }
    
    @Override
    @Transactional
    public void refreshTrendingScores() {
        log.info("Refreshing trending scores");
        List<Guild> publicGuilds = guildDao.lambdaQuery()
                .eq(Guild::getIsPublic, 1)
                .eq(Guild::getStatus, 1)
                .list();
        
        for (Guild guild : publicGuilds) {
            GuildActivityStats stats = guildActivityStatsDao.getByGuildId(guild.getId());
            if (stats == null) {
                stats = new GuildActivityStats();
                stats.setGuildId(guild.getId());
                stats.setDailyActiveUsers(0);
                stats.setWeeklyActiveUsers(0);
                stats.setDailyMessages(0);
                stats.setWeeklyMessages(0);
                stats.setJoinCount7d(0);
                stats.setTrendingScore(0.0);
                stats.setQualityScore(0.5);
            }
            
            List<GuildMember> members = guildMemberDao.lambdaQuery()
                    .eq(GuildMember::getGuildId, guild.getId())
                    .eq(GuildMember::getStatus, 1)
                    .list();
            
            int activeMembers = (int) (members.size() * 0.3);
            stats.setDailyActiveUsers(activeMembers);
            stats.setWeeklyActiveUsers((int) (members.size() * 0.6));
            stats.setDailyMessages(activeMembers * 5);
            stats.setWeeklyMessages(activeMembers * 30);
            
            double trendingScore = calculateTrendingScore(stats, guild.getMemberCount());
            stats.setTrendingScore(trendingScore);
            
            double qualityScore = calculateQualityScore(stats, guild.getMemberCount());
            stats.setQualityScore(qualityScore);
            
            guildActivityStatsDao.saveOrUpdate(stats);
        }
        
        log.info("Trending scores refresh completed");
    }
    
    private List<RecommendedGuildResp> getPersonalizedRecommendations(Long uid, DiscoveryReq req, int privacyLevel) {
        if (privacyLevel == PrivacyLevelEnum.POPULAR_ONLY.getLevel()) {
            return getPopularRecommendations(uid, req);
        }
        
        List<Long> joinedGuildIds = getJoinedGuildIds(uid);
        List<Long> dismissedGuildIds = userDismissRecordDao.getDismissedGuildIds(uid);
        
        List<UserInterestTag> userTags = userInterestTagDao.getByUid(uid);
        if (CollUtil.isEmpty(userTags) && privacyLevel == PrivacyLevelEnum.JOIN_HISTORY_ONLY.getLevel()) {
            return getPopularRecommendations(uid, req);
        }
        
        Map<Long, Double> userTagWeights = userTags.stream()
                .collect(Collectors.toMap(UserInterestTag::getTagId, UserInterestTag::getWeight));
        
        List<Guild> allPublicGuilds = guildDao.lambdaQuery()
                .eq(Guild::getIsPublic, 1)
                .eq(Guild::getStatus, 1)
                .list();
        
        List<ScoredGuild> scoredGuilds = new ArrayList<>();
        
        for (Guild guild : allPublicGuilds) {
            if (joinedGuildIds.contains(guild.getId()) || dismissedGuildIds.contains(guild.getId())) {
                continue;
            }
            
            double contentScore = calculateContentBasedScore(guild, userTagWeights);
            double collaborativeScore = calculateCollaborativeScore(uid, guild.getId(), joinedGuildIds);
            double socialScore = calculateSocialScore(uid, guild.getId());
            double popularityScore = calculatePopularityScore(guild);
            
            double finalScore;
            if (privacyLevel == PrivacyLevelEnum.JOIN_HISTORY_ONLY.getLevel()) {
                finalScore = contentScore * 0.6 + popularityScore * 0.4;
            } else {
                UserInterestProfile profile = userInterestProfileDao.getByUid(uid);
                double diversity = profile != null && profile.getDiversityScore() != null 
                        ? profile.getDiversityScore() : 0.5;
                
                finalScore = contentScore * 0.35 
                        + collaborativeScore * 0.25 
                        + socialScore * 0.15 
                        + popularityScore * 0.15
                        + diversity * 0.10;
            }
            
            int recommendSource;
            if (collaborativeScore > contentScore && collaborativeScore > socialScore) {
                recommendSource = RecommendSourceEnum.COLLABORATIVE.getType();
            } else if (contentScore > socialScore) {
                recommendSource = RecommendSourceEnum.CONTENT_BASED.getType();
            } else if (socialScore > 0.1) {
                recommendSource = RecommendSourceEnum.SOCIAL.getType();
            } else {
                recommendSource = RecommendSourceEnum.POPULARITY.getType();
            }
            
            scoredGuilds.add(new ScoredGuild(guild, finalScore, recommendSource));
        }
        
        scoredGuilds.sort((a, b) -> Double.compare(b.score, a.score));
        
        return scoredGuilds.stream()
                .map(sg -> buildRecommendedGuildResp(sg.guild, sg.score, sg.source, joinedGuildIds))
                .collect(Collectors.toList());
    }
    
    private List<RecommendedGuildResp> getPopularRecommendations(Long uid, DiscoveryReq req) {
        List<Long> joinedGuildIds = uid != null ? getJoinedGuildIds(uid) : Collections.emptyList();
        List<Long> dismissedGuildIds = uid != null ? userDismissRecordDao.getDismissedGuildIds(uid) : Collections.emptyList();
        
        List<Guild> popularGuilds = guildDao.lambdaQuery()
                .eq(Guild::getIsPublic, 1)
                .eq(Guild::getStatus, 1)
                .orderByDesc(Guild::getMemberCount)
                .list();
        
        return popularGuilds.stream()
                .filter(g -> !joinedGuildIds.contains(g.getId()) && !dismissedGuildIds.contains(g.getId()))
                .map(g -> buildRecommendedGuildResp(g, g.getMemberCount().doubleValue(), 
                        RecommendSourceEnum.POPULARITY.getType(), joinedGuildIds))
                .collect(Collectors.toList());
    }
    
    private List<RecommendedGuildResp> getNewestRecommendations(Long uid, DiscoveryReq req) {
        List<Long> joinedGuildIds = uid != null ? getJoinedGuildIds(uid) : Collections.emptyList();
        List<Long> dismissedGuildIds = uid != null ? userDismissRecordDao.getDismissedGuildIds(uid) : Collections.emptyList();
        
        List<Guild> newestGuilds = guildDao.lambdaQuery()
                .eq(Guild::getIsPublic, 1)
                .eq(Guild::getStatus, 1)
                .orderByDesc(Guild::getCreateTime)
                .list();
        
        return newestGuilds.stream()
                .filter(g -> !joinedGuildIds.contains(g.getId()) && !dismissedGuildIds.contains(g.getId()))
                .map(g -> buildRecommendedGuildResp(g, 0.5, RecommendSourceEnum.POPULARITY.getType(), joinedGuildIds))
                .collect(Collectors.toList());
    }
    
    private List<RecommendedGuildResp> getTrendingRecommendations(Long uid, DiscoveryReq req) {
        List<Long> joinedGuildIds = uid != null ? getJoinedGuildIds(uid) : Collections.emptyList();
        List<Long> dismissedGuildIds = uid != null ? userDismissRecordDao.getDismissedGuildIds(uid) : Collections.emptyList();
        
        List<GuildActivityStats> trendingStats = guildActivityStatsDao.getTrendingGuilds(100);
        
        List<RecommendedGuildResp> result = new ArrayList<>();
        for (GuildActivityStats stats : trendingStats) {
            if (joinedGuildIds.contains(stats.getGuildId()) || dismissedGuildIds.contains(stats.getGuildId())) {
                continue;
            }
            
            Guild guild = guildDao.getById(stats.getGuildId());
            if (guild == null || guild.getIsPublic() != 1 || guild.getStatus() != 1) {
                continue;
            }
            
            result.add(buildRecommendedGuildResp(guild, stats.getTrendingScore(), 
                    RecommendSourceEnum.POPULARITY.getType(), joinedGuildIds));
        }
        
        return result;
    }
    
    private double calculateContentBasedScore(Guild guild, Map<Long, Double> userTagWeights) {
        List<GuildTagRelation> guildTags = guildTagRelationDao.getByGuildId(guild.getId());
        if (CollUtil.isEmpty(guildTags) || userTagWeights.isEmpty()) {
            return 0.0;
        }
        
        double score = 0.0;
        double totalRelevance = 0.0;
        
        for (GuildTagRelation relation : guildTags) {
            Double userWeight = userTagWeights.get(relation.getTagId());
            if (userWeight != null) {
                score += userWeight * relation.getRelevanceScore();
                totalRelevance += relation.getRelevanceScore();
            }
        }
        
        return totalRelevance > 0 ? score / totalRelevance : 0.0;
    }
    
    private double calculateCollaborativeScore(Long uid, Long guildId, List<Long> joinedGuildIds) {
        if (CollUtil.isEmpty(joinedGuildIds)) {
            return 0.0;
        }
        
        List<GuildMember> targetGuildMembers = guildMemberDao.lambdaQuery()
                .eq(GuildMember::getGuildId, guildId)
                .eq(GuildMember::getStatus, 1)
                .list();
        
        if (CollUtil.isEmpty(targetGuildMembers)) {
            return 0.0;
        }
        
        List<Long> targetUids = targetGuildMembers.stream()
                .map(GuildMember::getUid)
                .collect(Collectors.toList());
        
        int overlapCount = 0;
        for (Long targetUid : targetUids) {
            if (targetUid.equals(uid)) continue;
            
            List<GuildMember> targetUserGuilds = guildMemberDao.lambdaQuery()
                    .eq(GuildMember::getUid, targetUid)
                    .eq(GuildMember::getStatus, 1)
                    .list();
            
            List<Long> targetUserGuildIds = targetUserGuilds.stream()
                    .map(GuildMember::getGuildId)
                    .collect(Collectors.toList());
            
            for (Long joinedGuildId : joinedGuildIds) {
                if (targetUserGuildIds.contains(joinedGuildId)) {
                    overlapCount++;
                    break;
                }
            }
        }
        
        return Math.min(1.0, (double) overlapCount / targetUids.size());
    }
    
    private double calculateSocialScore(Long uid, Long guildId) {
        List<GuildMember> targetGuildMembers = guildMemberDao.lambdaQuery()
                .eq(GuildMember::getGuildId, guildId)
                .eq(GuildMember::getStatus, 1)
                .list();
        
        if (CollUtil.isEmpty(targetGuildMembers)) {
            return 0.0;
        }
        
        List<Long> memberUids = targetGuildMembers.stream()
                .map(GuildMember::getUid)
                .collect(Collectors.toList());
        
        return 0.1;
    }
    
    private double calculatePopularityScore(Guild guild) {
        int memberCount = guild.getMemberCount() != null ? guild.getMemberCount() : 0;
        return Math.min(1.0, memberCount / 100.0);
    }
    
    private double calculateTrendingScore(GuildActivityStats stats, Integer memberCount) {
        double joinRate = memberCount > 0 ? (double) stats.getJoinCount7d() / memberCount : 0;
        double activityRate = memberCount > 0 ? (double) stats.getDailyActiveUsers() / memberCount : 0;
        double messageRate = memberCount > 0 ? (double) stats.getDailyMessages() / memberCount : 0;
        
        return joinRate * 0.4 + activityRate * 0.3 + messageRate * 0.3;
    }
    
    private double calculateQualityScore(GuildActivityStats stats, Integer memberCount) {
        double activityRatio = memberCount > 0 ? (double) stats.getWeeklyActiveUsers() / memberCount : 0;
        double messagePerUser = stats.getWeeklyActiveUsers() > 0 
                ? (double) stats.getWeeklyMessages() / stats.getWeeklyActiveUsers() : 0;
        
        return Math.min(1.0, activityRatio * 0.5 + Math.min(messagePerUser / 20.0, 1.0) * 0.5);
    }
    
    private List<RecommendedGuildResp> applyFilters(List<RecommendedGuildResp> guilds, DiscoveryReq req) {
        return guilds.stream()
                .filter(g -> {
                    if (req.getCategory() != null && !req.getCategory().isEmpty() 
                            && !req.getCategory().equals(g.getCategory())) {
                        return false;
                    }
                    if (req.getMinMembers() != null && g.getMemberCount() < req.getMinMembers()) {
                        return false;
                    }
                    if (req.getMaxMembers() != null && g.getMemberCount() > req.getMaxMembers()) {
                        return false;
                    }
                    if (req.getLanguage() != null && !req.getLanguage().equals(g.getLanguage())) {
                        return false;
                    }
                    if (req.getActivityLevel() != null && !req.getActivityLevel().equals(g.getActivityLevel())) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    private <T> List<T> paginate(List<T> list, int page, int pageSize) {
        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= list.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(fromIndex + pageSize, list.size());
        return list.subList(fromIndex, toIndex);
    }
    
    private List<Long> getJoinedGuildIds(Long uid) {
        List<GuildMember> memberships = guildMemberDao.lambdaQuery()
                .eq(GuildMember::getUid, uid)
                .eq(GuildMember::getStatus, 1)
                .list();
        return memberships.stream().map(GuildMember::getGuildId).collect(Collectors.toList());
    }
    
    private RecommendedGuildResp buildRecommendedGuildResp(Guild guild, double score, int source, List<Long> joinedGuildIds) {
        RecommendedGuildResp resp = new RecommendedGuildResp();
        resp.setId(guild.getId());
        resp.setName(guild.getName());
        resp.setIcon(guild.getIcon());
        resp.setDescription(guild.getDescription());
        resp.setCategory(guild.getCategory());
        resp.setMemberCount(guild.getMemberCount());
        resp.setLanguage(guild.getLanguage());
        resp.setActivityLevel(guild.getActivityLevel());
        resp.setIsJoined(joinedGuildIds.contains(guild.getId()));
        resp.setRelevanceScore(Math.round(score * 100.0) / 100.0);
        resp.setRecommendSource(source);
        resp.setRecommendSourceDesc(RecommendSourceEnum.values()[source].getDesc());
        resp.setOnlineCount(0);
        
        List<GuildTagRelation> tagRelations = guildTagRelationDao.getByGuildId(guild.getId());
        List<GuildTag> allTags = guildTagDao.list();
        Map<Long, String> tagNameMap = allTags.stream()
                .collect(Collectors.toMap(GuildTag::getId, GuildTag::getName));
        
        List<String> tagNames = tagRelations.stream()
                .map(r -> tagNameMap.getOrDefault(r.getTagId(), ""))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toList());
        resp.setTags(tagNames);
        
        return resp;
    }
    
    private List<TagCategoryResp> buildTagCategories() {
        List<GuildTag> allTags = guildTagDao.getAllTags();
        Map<String, List<GuildTag>> grouped = allTags.stream()
                .collect(Collectors.groupingBy(GuildTag::getCategory));
        
        return grouped.entrySet().stream().map(entry -> {
            TagCategoryResp resp = new TagCategoryResp();
            resp.setCategory(entry.getKey());
            resp.setTags(entry.getValue().stream().map(t -> {
                TagResp tagResp = new TagResp();
                tagResp.setId(t.getId());
                tagResp.setName(t.getName());
                tagResp.setCategory(t.getCategory());
                tagResp.setWeight(t.getWeight());
                return tagResp;
            }).collect(Collectors.toList()));
            return resp;
        }).collect(Collectors.toList());
    }
    
    private UserInterestResp buildUserInterestResp(Long uid) {
        List<UserInterestTag> tags = userInterestTagDao.getByUid(uid);
        UserInterestProfile profile = userInterestProfileDao.getByUid(uid);
        
        UserInterestResp resp = new UserInterestResp();
        resp.setPrivacyLevel(profile != null ? profile.getPrivacyLevel() : PrivacyLevelEnum.FULL_PERSONALIZED.getLevel());
        resp.setDiversityPreference(profile != null && profile.getDiversityScore() != null ? profile.getDiversityScore() : 0.5);
        
        List<GuildTag> allTags = guildTagDao.list();
        Map<Long, GuildTag> tagMap = allTags.stream().collect(Collectors.toMap(GuildTag::getId, t -> t));
        
        resp.setInterestTags(tags.stream().map(t -> {
            UserInterestTagResp tagResp = new UserInterestTagResp();
            tagResp.setTagId(t.getTagId());
            GuildTag gt = tagMap.get(t.getTagId());
            if (gt != null) {
                tagResp.setTagName(gt.getName());
                tagResp.setCategory(gt.getCategory());
            }
            tagResp.setWeight(t.getWeight());
            tagResp.setSource(t.getSource());
            return tagResp;
        }).collect(Collectors.toList()));
        
        return resp;
    }
    
    private void trackRecommendations(Long uid, List<RecommendedGuildResp> recs, int type) {
        for (int i = 0; i < recs.size(); i++) {
            RecommendedGuildResp rec = recs.get(i);
            RecommendationTrack existing = recommendationTrackDao.getByUidAndGuildId(uid, rec.getId());
            if (existing != null) {
                continue;
            }
            
            RecommendationTrack track = new RecommendationTrack();
            track.setUid(uid);
            track.setGuildId(rec.getId());
            track.setRecommendType(type);
            track.setRecommendSource(rec.getRecommendSource());
            track.setRelevanceScore(rec.getRelevanceScore());
            track.setPosition(i);
            track.setIsClicked(0);
            track.setIsJoined(0);
            track.setIsDismissed(0);
            track.setAbGroup(assignABGroup(uid));
            recommendationTrackDao.save(track);
        }
    }
    
    private String assignABGroup(Long uid) {
        return uid % 2 == 0 ? "A" : "B";
    }
    
    private String buildCacheKey(Long uid, DiscoveryReq req) {
        return RECOMMEND_CACHE_PREFIX + uid + ":" + req.getRecommendType() + ":" 
                + req.getCategory() + ":" + req.getPage() + ":" + req.getPageSize();
    }
    
    private void invalidateUserCache(Long uid) {
        List<String> keys = RedisUtils.scan(RECOMMEND_CACHE_PREFIX + uid + "*");
        if (CollUtil.isNotEmpty(keys)) {
            RedisUtils.del(keys);
        }
    }
    
    private static class ScoredGuild {
        Guild guild;
        double score;
        int source;
        
        ScoredGuild(Guild guild, double score, int source) {
            this.guild = guild;
            this.score = score;
            this.source = source;
        }
    }
}
