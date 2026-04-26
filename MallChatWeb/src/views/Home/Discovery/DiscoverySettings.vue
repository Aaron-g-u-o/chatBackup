<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="推荐设置"
    width="480px"
    class="discovery-settings-dialog"
  >
    <div class="settings-content">
      <div class="settings-section">
        <h3 class="section-title">隐私控制</h3>
        <p class="section-desc">控制推荐系统使用你的哪些数据</p>
        <div class="privacy-options">
          <div
            v-for="option in privacyOptions"
            :key="option.value"
            :class="['privacy-option', { active: currentPrivacy === option.value }]"
            @click="handlePrivacyChange(option.value)"
          >
            <div class="option-header">
              <Icon :icon="option.icon" :size="20" class="option-icon" />
              <span class="option-name">{{ option.label }}</span>
              <div v-if="currentPrivacy === option.value" class="option-check">
                <el-icon :size="16"><Check /></el-icon>
              </div>
            </div>
            <p class="option-desc">{{ option.desc }}</p>
          </div>
        </div>
      </div>

      <div class="settings-section">
        <h3 class="section-title">推荐多样性</h3>
        <p class="section-desc">调整推荐结果的相关性与多样性平衡</p>
        <div class="diversity-slider">
          <span class="slider-label">精准匹配</span>
          <el-slider
            v-model="diversityValue"
            :min="0"
            :max="100"
            :step="10"
            :show-tooltip="false"
            @change="(val: number | number[]) => handleDiversityChange(Array.isArray(val) ? val[0] : val)"
          />
          <span class="slider-label">探索发现</span>
        </div>
      </div>

      <div class="settings-section">
        <h3 class="section-title">兴趣标签</h3>
        <p class="section-desc">选择你感兴趣的标签，帮助我们更好地推荐</p>
        <div class="interest-editor">
          <div
            v-for="cat in discoveryStore.categories"
            :key="cat.category"
            class="category-group"
          >
            <h4 class="category-name">{{ cat.category }}</h4>
            <div class="category-tags">
              <button
                v-for="tag in cat.tags"
                :key="tag.id"
                :class="['editable-tag', { selected: selectedTags.has(tag.id) }]"
                @click="toggleTag(tag.id)"
              >
                {{ tag.name }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="saveSettings" :loading="saving">保存设置</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Check } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useDiscoveryStore } from '@/stores/discovery'
import { PrivacyLevelEnum } from '@/services/discoveryTypes'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const discoveryStore = useDiscoveryStore()
const saving = ref(false)
const currentPrivacy = ref(0)
const diversityValue = ref(50)
const selectedTags = ref<Set<number>>(new Set())

const privacyOptions = [
  {
    value: PrivacyLevelEnum.FULL_PERSONALIZED,
    label: '完全个性化',
    desc: '基于你的加入记录、活跃行为和消息内容进行推荐',
    icon: 'sparkles',
  },
  {
    value: PrivacyLevelEnum.JOIN_HISTORY_ONLY,
    label: '仅加入记录',
    desc: '仅基于你加入的服务器记录进行推荐，不分析行为和消息',
    icon: 'shield',
  },
  {
    value: PrivacyLevelEnum.POPULAR_ONLY,
    label: '仅热门推荐',
    desc: '不使用个人数据，仅展示热门服务器',
    icon: 'globe',
  },
]

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      currentPrivacy.value = discoveryStore.userInterest?.privacyLevel ?? PrivacyLevelEnum.FULL_PERSONALIZED
      diversityValue.value = Math.round((discoveryStore.userInterest?.diversityPreference ?? 0.5) * 100)
      selectedTags.value = new Set(
        discoveryStore.userInterest?.interestTags.map((t) => t.tagId) ?? [],
      )
    }
  },
)

const handlePrivacyChange = (level: number) => {
  currentPrivacy.value = level
}

const handleDiversityChange = (val: number) => {
  diversityValue.value = val
}

const toggleTag = (tagId: number) => {
  const newSet = new Set(selectedTags.value)
  if (newSet.has(tagId)) {
    newSet.delete(tagId)
  } else {
    newSet.add(tagId)
  }
  selectedTags.value = newSet
}

const saveSettings = async () => {
  saving.value = true
  try {
    await discoveryStore.updatePrivacy(currentPrivacy.value, diversityValue.value / 100)
    await discoveryStore.updateInterestTags(Array.from(selectedTags.value))
    ElMessage.success('设置已保存')
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error('保存设置失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.settings-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6);
}

.settings-section {
  .section-title {
    font-size: var(--font-size-base);
    font-weight: var(--font-weight-semibold);
    color: var(--color-text-primary);
    margin: 0 0 var(--spacing-1);
  }

  .section-desc {
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
    margin: 0 0 var(--spacing-4);
  }
}

.privacy-options {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.privacy-option {
  padding: var(--spacing-3) var(--spacing-4);
  border: 1px solid var(--color-border-primary);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all var(--transition-fast) var(--ease-out);

  &:hover {
    background: var(--color-surface-2);
    border-color: var(--color-primary-500);
  }

  &.active {
    background: linear-gradient(135deg, rgb(3 169 244 / 10%), rgb(0 188 212 / 5%));
    border-color: var(--color-primary-500);
    box-shadow: var(--shadow-glow-primary);
  }
}

.option-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-1);
}

.option-icon {
  color: var(--color-primary-400);
}

.option-name {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
}

.option-check {
  margin-left: auto;
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  background: var(--color-primary-500);
  color: var(--color-neutral-0);
  display: flex;
  align-items: center;
  justify-content: center;
}

.option-desc {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  margin: 0;
  padding-left: 28px;
}

.diversity-slider {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  padding: var(--spacing-3) 0;

  .slider-label {
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
    white-space: nowrap;
    min-width: 60px;

    &:first-child {
      text-align: right;
    }
  }

  :deep(.el-slider) {
    flex: 1;

    .el-slider__runway {
      background: var(--color-surface-4);
      height: 6px;
      border-radius: var(--radius-full);
    }

    .el-slider__bar {
      background: linear-gradient(90deg, var(--color-brand-gradient-start), var(--color-brand-gradient-end));
      height: 6px;
      border-radius: var(--radius-full);
    }

    .el-slider__button {
      width: 18px;
      height: 18px;
      border: 3px solid var(--color-primary-500);
      background: var(--color-neutral-0);
      box-shadow: var(--shadow-glow-primary);
    }
  }
}

.interest-editor {
  max-height: 300px;
  overflow-y: auto;
  padding-right: var(--spacing-2);
}

.category-group {
  margin-bottom: var(--spacing-4);
}

.category-name {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: var(--letter-spacing-wider);
  margin: 0 0 var(--spacing-2);
}

.category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-1);
}

.editable-tag {
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

  &.selected {
    background: linear-gradient(135deg, var(--color-brand-gradient-start), var(--color-brand-gradient-end));
    color: var(--color-neutral-0);
    border-color: transparent;
    box-shadow: var(--shadow-glow-primary);
  }
}
</style>
