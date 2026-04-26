<template>
  <div class="discovery-page">
    <div class="discovery-header">
      <div class="header-top">
        <div class="header-title-section">
          <h1 class="header-title">发现社区</h1>
          <p class="header-subtitle">探索你感兴趣的社区服务器</p>
        </div>
        <div class="header-actions">
          <el-button
            class="settings-btn"
            :icon="Setting"
            circle
            @click="showSettings = true"
          />
        </div>
      </div>

      <div class="search-bar">
        <el-input
          v-model="discoveryStore.searchKeyword"
          placeholder="搜索社区名称、标签或描述..."
          :prefix-icon="Search"
          clearable
          class="search-input"
        />
      </div>

      <div class="recommend-tabs">
        <button
          v-for="(label, type) in discoveryStore.typeLabels"
          :key="type"
          :class="['tab-btn', { active: discoveryStore.currentType === Number(type) }]"
          @click="discoveryStore.setRecommendType(Number(type) as RecommendTypeEnum)"
        >
          <Icon
            :icon="tabIcons[Number(type) as RecommendTypeEnum]"
            :size="16"
            class="tab-icon"
          />
          <span>{{ label }}</span>
        </button>
      </div>
    </div>

    <div class="discovery-body">
      <aside class="filter-sidebar">
        <div class="filter-section">
          <h3 class="filter-title">分类筛选</h3>
          <div class="filter-tags">
            <button
              :class="['filter-tag', { active: !discoveryStore.selectedCategory }]"
              @click="discoveryStore.setCategory(undefined)"
            >
              全部
            </button>
            <button
              v-for="cat in discoveryStore.categories"
              :key="cat.category"
              :class="['filter-tag', { active: discoveryStore.selectedCategory === cat.category }]"
              @click="discoveryStore.setCategory(cat.category)"
            >
              {{ cat.category }}
            </button>
          </div>
        </div>

        <div class="filter-section">
          <h3 class="filter-title">活跃度</h3>
          <div class="filter-tags">
            <button
              :class="['filter-tag', { active: !discoveryStore.selectedActivityLevel }]"
              @click="discoveryStore.setActivityLevel(undefined)"
            >
              全部
            </button>
            <button
              :class="['filter-tag', { active: discoveryStore.selectedActivityLevel === 3 }]"
              @click="discoveryStore.setActivityLevel(3)"
            >
              🔥 高活跃
            </button>
            <button
              :class="['filter-tag', { active: discoveryStore.selectedActivityLevel === 2 }]"
              @click="discoveryStore.setActivityLevel(2)"
            >
              🌤 中活跃
            </button>
            <button
              :class="['filter-tag', { active: discoveryStore.selectedActivityLevel === 1 }]"
              @click="discoveryStore.setActivityLevel(1)"
            >
              😴 低活跃
            </button>
          </div>
        </div>

        <div v-if="discoveryStore.userInterest" class="filter-section">
          <h3 class="filter-title">你的兴趣</h3>
          <div class="interest-tags">
            <span
              v-for="tag in discoveryStore.userInterest.interestTags.slice(0, 8)"
              :key="tag.tagId"
              class="interest-tag"
            >
              {{ tag.tagName }}
            </span>
          </div>
        </div>
      </aside>

      <main class="discovery-content">
        <div v-if="discoveryStore.loading && discoveryStore.recommendations.length === 0" class="loading-state">
          <div class="loading-spinner"></div>
          <p>正在为你发现精彩社区...</p>
        </div>

        <div v-else-if="discoveryStore.filteredRecommendations.length === 0" class="empty-state">
          <div class="empty-icon">🔍</div>
          <p class="empty-title">没有找到匹配的社区</p>
          <p class="empty-desc">试试调整筛选条件或切换推荐类型</p>
        </div>

        <div v-else class="guild-grid">
          <div
            v-for="(guild, index) in discoveryStore.filteredRecommendations"
            :key="guild.id"
            class="guild-card"
            :style="{ animationDelay: `${index * 50}ms` }"
            @click="handleGuildClick(guild)"
          >
            <div class="card-banner">
              <div class="card-gradient"></div>
              <div class="card-badge" v-if="guild.relevanceScore > 0.7">
                <Icon icon="fire" :size="12" />
                <span>高度匹配</span>
              </div>
              <button
                class="card-dismiss"
                @click.stop="handleDismiss(guild)"
                title="不感兴趣"
              >
                <el-icon :size="14"><Close /></el-icon>
              </button>
            </div>

            <div class="card-body">
              <div class="card-icon-wrapper">
                <div class="card-icon">
                  <img v-if="guild.icon" :src="guild.icon" :alt="guild.name" />
                  <span v-else class="icon-initial">{{ guild.name.charAt(0) }}</span>
                </div>
                <div v-if="guild.onlineCount > 0" class="online-indicator"></div>
              </div>

              <div class="card-info">
                <h3 class="card-name">{{ guild.name }}</h3>
                <p v-if="guild.description" class="card-desc">{{ guild.description }}</p>
                <div class="card-tags" v-if="guild.tags.length > 0">
                  <span v-for="tag in guild.tags.slice(0, 3)" :key="tag" class="card-tag">
                    {{ tag }}
                  </span>
                </div>
              </div>

              <div class="card-meta">
                <div class="meta-item">
                  <Icon icon="group" :size="14" />
                  <span>{{ formatMemberCount(guild.memberCount) }}</span>
                </div>
                <div v-if="guild.category" class="meta-item">
                  <Icon icon="tag" :size="14" />
                  <span>{{ guild.category }}</span>
                </div>
              </div>

              <div class="card-footer">
                <div class="relevance-bar" v-if="guild.relevanceScore > 0">
                  <div class="relevance-fill" :style="{ width: `${guild.relevanceScore * 100}%` }"></div>
                </div>
                <span class="relevance-label" v-if="guild.relevanceScore > 0">
                  匹配度 {{ Math.round(guild.relevanceScore * 100) }}%
                </span>
                <span class="source-label">{{ guild.recommendSourceDesc }}</span>
                <el-button
                  v-if="guild.isJoined"
                  type="info"
                  size="small"
                  disabled
                  class="join-btn joined"
                >
                  已加入
                </el-button>
                <el-button
                  v-else
                  type="primary"
                  size="small"
                  class="join-btn"
                  @click.stop="handleJoin(guild)"
                  :loading="joiningGuildId === guild.id"
                >
                  加入
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="!discoveryStore.isLast && discoveryStore.recommendations.length > 0" class="load-more">
          <el-button
            :loading="discoveryStore.loading"
            @click="discoveryStore.fetchRecommendations(false)"
            class="load-more-btn"
          >
            {{ discoveryStore.loading ? '加载中...' : '加载更多' }}
          </el-button>
        </div>
      </main>
    </div>

    <DiscoverySettings v-model="showSettings" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Setting, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useDiscoveryStore } from '@/stores/discovery'
import { RecommendTypeEnum } from '@/services/discoveryTypes'
import type { RecommendedGuildType } from '@/services/discoveryTypes'
import DiscoverySettings from './DiscoverySettings.vue'

const discoveryStore = useDiscoveryStore()
const showSettings = ref(false)
const joiningGuildId = ref<number | null>(null)

const tabIcons: Record<RecommendTypeEnum, string> = {
  [RecommendTypeEnum.PERSONALIZED]: 'star',
  [RecommendTypeEnum.POPULAR]: 'fire',
  [RecommendTypeEnum.NEWEST]: 'sparkles',
  [RecommendTypeEnum.TRENDING]: 'trending',
}

const formatMemberCount = (count: number): string => {
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`
  return `${count}`
}

const handleGuildClick = (guild: RecommendedGuildType) => {
  discoveryStore.trackClick(guild.id)
}

const handleJoin = async (guild: RecommendedGuildType) => {
  joiningGuildId.value = guild.id
  const success = await discoveryStore.joinGuild(guild.id)
  if (success) {
    ElMessage.success(`已加入 ${guild.name}`)
  } else {
    ElMessage.error('加入服务器失败')
  }
  joiningGuildId.value = null
}

const handleDismiss = async (guild: RecommendedGuildType) => {
  await discoveryStore.dismissGuild(guild.id, '不感兴趣')
  ElMessage.info('已移除该推荐')
}

onMounted(() => {
  discoveryStore.fetchRecommendations(true)
  discoveryStore.buildProfile()
})
</script>

<style scoped lang="scss">
.discovery-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-surface-0);
  overflow: hidden;
}

.discovery-header {
  flex-shrink: 0;
  padding: var(--spacing-6) var(--spacing-8);
  background: linear-gradient(180deg, var(--color-surface-1) 0%, var(--color-surface-0) 100%);
  border-bottom: 1px solid var(--color-border-primary);
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-5);
}

.header-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-extrabold);
  background: linear-gradient(135deg, var(--color-brand-gradient-start), var(--color-accent-gradient-start));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: var(--letter-spacing-tight);
  margin: 0;
}

.header-subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  margin-top: var(--spacing-1);
}

.settings-btn {
  background: var(--color-surface-3) !important;
  border: 1px solid var(--color-border-primary) !important;
  color: var(--color-text-secondary) !important;
  transition: all var(--transition-fast) var(--ease-out);

  &:hover {
    background: var(--color-surface-4) !important;
    color: var(--color-primary-400) !important;
    transform: rotate(30deg);
  }
}

.search-bar {
  margin-bottom: var(--spacing-5);
}

.search-input {
  :deep(.el-input__wrapper) {
    background: var(--color-surface-2);
    border: 1px solid var(--color-border-primary);
    border-radius: var(--radius-2xl);
    box-shadow: var(--shadow-inner);
    padding: var(--spacing-1) var(--spacing-4);
    transition: all var(--transition-fast) var(--ease-out);

    &:hover {
      border-color: var(--color-primary-500);
    }

    &.is-focus {
      border-color: var(--color-primary-500);
      box-shadow: var(--shadow-glow-primary);
    }
  }

  :deep(.el-input__inner) {
    color: var(--color-text-primary);
    font-size: var(--font-size-sm);

    &::placeholder {
      color: var(--color-text-placeholder);
    }
  }
}

.recommend-tabs {
  display: flex;
  gap: var(--spacing-2);
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-2) var(--spacing-4);
  border: 1px solid var(--color-border-primary);
  border-radius: var(--radius-full);
  background: var(--color-surface-2);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: all var(--transition-fast) var(--ease-out);

  &:hover {
    background: var(--color-surface-3);
    color: var(--color-text-primary);
    border-color: var(--color-primary-500);
  }

  &.active {
    background: linear-gradient(135deg, var(--color-brand-gradient-start), var(--color-brand-gradient-end));
    color: var(--color-neutral-0);
    border-color: transparent;
    box-shadow: var(--shadow-glow-primary);
    font-weight: var(--font-weight-semibold);
  }

  .tab-icon {
    opacity: 0.8;
  }
}

.discovery-body {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
}

.filter-sidebar {
  width: 220px;
  flex-shrink: 0;
  padding: var(--spacing-5);
  border-right: 1px solid var(--color-border-primary);
  background: var(--color-surface-1);
  overflow-y: auto;
}

.filter-section {
  margin-bottom: var(--spacing-6);
}

.filter-title {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: var(--letter-spacing-wider);
  margin-bottom: var(--spacing-3);
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-1);
}

.filter-tag {
  padding: var(--spacing-1) var(--spacing-3);
  border: 1px solid var(--color-border-primary);
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  cursor: pointer;
  transition: all var(--transition-fast) var(--ease-out);

  &:hover {
    background: var(--color-surface-3);
    color: var(--color-text-primary);
  }

  &.active {
    background: var(--color-primary-500);
    color: var(--color-neutral-0);
    border-color: transparent;
  }
}

.interest-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-1);
}

.interest-tag {
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, rgb(3 169 244 / 15%), rgb(0 188 212 / 15%));
  color: var(--color-primary-300);
  font-size: var(--font-size-xs);
  border: 1px solid rgb(3 169 244 / 20%);
}

.discovery-content {
  flex: 1;
  padding: var(--spacing-6);
  overflow-y: auto;
  scroll-behavior: smooth;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: var(--color-text-tertiary);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-surface-4);
  border-top-color: var(--color-primary-500);
  border-radius: var(--radius-full);
  animation: spin 0.8s linear infinite;
  margin-bottom: var(--spacing-4);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-4);
}

.empty-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-2);
}

.empty-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.guild-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--spacing-5);
}

.guild-card {
  border-radius: var(--radius-2xl);
  background: var(--color-surface-1);
  border: 1px solid var(--color-border-primary);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-normal) var(--ease-out);
  animation: cardFadeIn 0.4s var(--ease-out) both;

  &:hover {
    border-color: var(--color-primary-500);
    transform: translateY(-4px);
    box-shadow: var(--shadow-elevation-3), var(--shadow-glow-primary);

    .card-gradient {
      opacity: 1;
    }

    .card-dismiss {
      opacity: 1;
    }

    .join-btn:not(.joined) {
      background: linear-gradient(135deg, var(--color-brand-gradient-start), var(--color-brand-gradient-end)) !important;
      border-color: transparent !important;
      box-shadow: var(--shadow-glow-primary);
    }
  }
}

@keyframes cardFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-banner {
  position: relative;
  height: 80px;
  background: linear-gradient(135deg, var(--color-surface-3), var(--color-surface-2));
  overflow: hidden;
}

.card-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgb(3 169 244 / 20%), rgb(0 188 212 / 10%));
  opacity: 0;
  transition: opacity var(--transition-normal) var(--ease-out);
}

.card-badge {
  position: absolute;
  top: var(--spacing-2);
  left: var(--spacing-3);
  display: flex;
  align-items: center;
  gap: var(--spacing-1);
  padding: 2px var(--spacing-2);
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--color-accent-gradient-start), var(--color-accent-gradient-end));
  color: var(--color-neutral-0);
  font-size: 10px;
  font-weight: var(--font-weight-bold);
  box-shadow: var(--shadow-glow-accent);
}

.card-dismiss {
  position: absolute;
  top: var(--spacing-2);
  right: var(--spacing-2);
  width: 24px;
  height: 24px;
  border-radius: var(--radius-full);
  background: rgb(0 0 0 / 50%);
  border: none;
  color: var(--color-neutral-0);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: all var(--transition-fast) var(--ease-out);

  &:hover {
    background: var(--color-error-500);
  }
}

.card-body {
  padding: var(--spacing-4) var(--spacing-5) var(--spacing-5);
}

.card-icon-wrapper {
  position: relative;
  width: 52px;
  height: 52px;
  margin-top: -30px;
  margin-bottom: var(--spacing-3);
}

.card-icon {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-brand-gradient-start), var(--color-brand-gradient-end));
  border: 3px solid var(--color-surface-1);
  overflow: hidden;
  box-shadow: var(--shadow-md);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.icon-initial {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-neutral-0);
}

.online-indicator {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  border-radius: var(--radius-full);
  background: var(--color-online);
  border: 2px solid var(--color-surface-1);
  box-shadow: 0 0 6px rgb(76 175 80 / 50%);
}

.card-info {
  margin-bottom: var(--spacing-3);
}

.card-name {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  line-height: var(--line-height-snug);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0 0 var(--spacing-2);
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-1);
}

.card-tag {
  padding: 1px var(--spacing-2);
  border-radius: var(--radius-sm);
  background: var(--color-surface-3);
  color: var(--color-primary-300);
  font-size: 10px;
  font-weight: var(--font-weight-medium);
}

.card-meta {
  display: flex;
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-3);
  padding-bottom: var(--spacing-3);
  border-bottom: 1px solid var(--color-border-secondary);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-1);
  color: var(--color-text-tertiary);
  font-size: var(--font-size-xs);
}

.card-footer {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.relevance-bar {
  flex: 1;
  height: 3px;
  border-radius: var(--radius-full);
  background: var(--color-surface-4);
  overflow: hidden;
}

.relevance-fill {
  height: 100%;
  border-radius: var(--radius-full);
  background: linear-gradient(90deg, var(--color-brand-gradient-start), var(--color-brand-gradient-end));
  transition: width var(--transition-slow) var(--ease-out);
}

.relevance-label {
  font-size: 10px;
  color: var(--color-primary-400);
  font-weight: var(--font-weight-semibold);
  white-space: nowrap;
}

.source-label {
  font-size: 10px;
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

.join-btn {
  margin-left: auto;
  border-radius: var(--radius-full) !important;
  font-weight: var(--font-weight-semibold) !important;
  transition: all var(--transition-fast) var(--ease-out) !important;

  &.joined {
    opacity: 0.6;
  }
}

.load-more {
  display: flex;
  justify-content: center;
  padding: var(--spacing-8) 0;
}

.load-more-btn {
  border-radius: var(--radius-full) !important;
  padding: var(--spacing-3) var(--spacing-8) !important;
  background: var(--color-surface-2) !important;
  border: 1px solid var(--color-border-primary) !important;
  color: var(--color-text-secondary) !important;
  font-weight: var(--font-weight-medium) !important;
  transition: all var(--transition-fast) var(--ease-out) !important;

  &:hover {
    background: var(--color-surface-3) !important;
    color: var(--color-primary-400) !important;
    border-color: var(--color-primary-500) !important;
  }
}

@media only screen and (max-width: 768px) {
  .filter-sidebar {
    display: none;
  }

  .discovery-header {
    padding: var(--spacing-4);
  }

  .header-title {
    font-size: var(--font-size-2xl);
  }

  .guild-grid {
    grid-template-columns: 1fr;
  }

  .recommend-tabs {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;

    &::-webkit-scrollbar {
      display: none;
    }
  }
}
</style>
