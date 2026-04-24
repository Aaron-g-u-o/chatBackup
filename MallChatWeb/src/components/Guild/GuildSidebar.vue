<template>
  <div class="guild-sidebar">
    <div class="guild-list">
      <div
        v-for="guild in guildStore.guilds"
        :key="guild.id"
        :class="['guild-item', { active: guildStore.currentGuild?.id === guild.id }]"
        @click="selectGuild(guild)"
      >
        <div class="guild-icon">
          <img v-if="guild.icon" :src="guild.icon" :alt="guild.name" />
          <span v-else class="guild-name-initial">{{ guild.name.charAt(0) }}</span>
        </div>
        <div class="guild-indicator"></div>
      </div>
      
      <div class="guild-item add-guild" @click="showAddDialog = true">
        <div class="guild-icon">
          <el-icon :size="24"><Plus /></el-icon>
        </div>
      </div>
      
      <div class="guild-item explore-guild" @click="showExploreDialog = true">
        <div class="guild-icon">
          <el-icon :size="24"><Compass /></el-icon>
        </div>
      </div>
    </div>
    
    <el-dialog v-model="showAddDialog" title="添加服务器" width="480px">
      <div class="add-options">
        <div
          :class="['option-card', { active: addMode === 'create' }]"
          @click="addMode = 'create'"
        >
          <el-icon :size="32"><FolderAdd /></el-icon>
          <span class="option-title">创建服务器</span>
          <span class="option-desc">创建一个新的服务器</span>
        </div>
        <div
          :class="['option-card', { active: addMode === 'join' }]"
          @click="addMode = 'join'"
        >
          <el-icon :size="32"><Link /></el-icon>
          <span class="option-title">加入服务器</span>
          <span class="option-desc">通过邀请码加入服务器</span>
        </div>
      </div>
      
      <template v-if="addMode === 'create'">
        <el-form :model="createForm" label-width="80px" class="create-form">
          <el-form-item label="服务器名称">
            <el-input v-model="createForm.name" placeholder="请输入服务器名称" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input
              v-model="createForm.description"
              type="textarea"
              placeholder="服务器描述（可选）"
            />
          </el-form-item>
          <el-form-item label="服务器类型">
            <el-radio-group v-model="createForm.isPublic">
              <el-radio :label="1">公开</el-radio>
              <el-radio :label="0">私密</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </template>
      
      <template v-else>
        <el-form :model="joinForm" label-width="80px" class="join-form">
          <el-form-item label="邀请码">
            <el-input
              v-model="joinForm.inviteCode"
              placeholder="请输入服务器邀请码"
              maxlength="8"
              clearable
              @paste="handlePaste"
            />
          </el-form-item>
        </el-form>
      </template>
      
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button
          v-if="addMode === 'create'"
          type="primary"
          @click="handleCreateGuild"
          :loading="creating"
        >
          创建
        </el-button>
        <el-button
          v-else
          type="primary"
          @click="handleJoinGuild"
          :loading="joining"
        >
          加入
        </el-button>
      </template>
    </el-dialog>
    
    <el-dialog v-model="showExploreDialog" title="探索社区" width="600px">
      <div class="explore-content">
        <div v-if="loadingPublic" class="loading-container">
          <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        </div>
        <div v-else-if="publicGuilds.length === 0" class="empty-container">
          <p>暂无公开服务器</p>
        </div>
        <div v-else class="guild-grid">
          <div
            v-for="guild in publicGuilds"
            :key="guild.id"
            class="guild-card"
          >
            <div class="guild-card-icon">
              <img v-if="guild.icon" :src="guild.icon" :alt="guild.name" />
              <span v-else>{{ guild.name.charAt(0) }}</span>
            </div>
            <div class="guild-card-info">
              <div class="guild-card-name">{{ guild.name }}</div>
              <div class="guild-card-desc">{{ guild.description || '暂无描述' }}</div>
              <div class="guild-card-meta">
                <span>{{ guild.memberCount }} 成员</span>
              </div>
            </div>
            <el-button
              type="primary"
              size="small"
              @click="joinPublicGuild(guild.id)"
              :loading="joiningGuildId === guild.id"
            >
              加入
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
    
    <el-dialog v-model="showInviteDialog" title="邀请好友" width="400px">
      <div class="invite-content">
        <p class="invite-tip">分享邀请码给好友，即可邀请他们加入服务器</p>
        <div class="invite-code-box">
          <span class="invite-code">{{ guildStore.currentGuild?.inviteCode || '暂无邀请码' }}</span>
          <el-button 
            type="primary" 
            size="small" 
            @click="copyInviteCode"
            :disabled="!guildStore.currentGuild?.inviteCode"
          >
            复制
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Compass, Loading } from '@element-plus/icons-vue'
import { useGuildStore } from '@/stores/guild'
import type { GuildType } from '@/services/guildTypes'
import guildApis from '@/services/guildApis'

const guildStore = useGuildStore()

const showAddDialog = ref(false)
const showExploreDialog = ref(false)
const showInviteDialog = ref(false)
const addMode = ref<'create' | 'join'>('create')
const creating = ref(false)
const joining = ref(false)
const loadingPublic = ref(false)
const publicGuilds = ref<GuildType[]>([])
const joiningGuildId = ref<number | null>(null)
const createForm = ref({
  name: '',
  description: '',
  isPublic: 1,
})
const joinForm = ref({
  inviteCode: '',
})

onMounted(() => {
  guildStore.fetchGuilds()
})

const selectGuild = (guild: GuildType) => {
  guildStore.setCurrentGuild(guild)
}

const handlePaste = (event: ClipboardEvent) => {
  event.preventDefault()
  const pastedText = event.clipboardData?.getData('text')
  if (pastedText) {
    const cleanedText = pastedText.trim().substring(0, 8)
    joinForm.value.inviteCode = cleanedText
  }
}

const handleCreateGuild = async () => {
  if (!createForm.value.name) {
    ElMessage.warning('请输入服务器名称')
    return
  }
  
  creating.value = true
  try {
    const guild = await guildStore.createGuild(
      createForm.value.name,
      undefined,
      createForm.value.description,
      createForm.value.isPublic,
    )
    if (guild) {
      showAddDialog.value = false
      createForm.value = { name: '', description: '', isPublic: 1 }
      guildStore.setCurrentGuild(guild)
      ElMessage.success('服务器创建成功')
    }
  } catch (error) {
    console.error('创建服务器失败', error)
    ElMessage.error('创建服务器失败')
  } finally {
    creating.value = false
  }
}

const handleJoinGuild = async () => {
  if (!joinForm.value.inviteCode) {
    ElMessage.warning('请输入邀请码')
    return
  }
  
  joining.value = true
  try {
    const guild = await guildApis.joinGuildByInviteCode(joinForm.value.inviteCode)
    if (guild) {
      await guildStore.fetchGuilds()
      showAddDialog.value = false
      joinForm.value = { inviteCode: '' }
      guildStore.setCurrentGuild(guild)
      ElMessage.success('成功加入服务器')
    }
  } catch (error) {
    console.error('加入服务器失败', error)
    ElMessage.error('邀请码无效或服务器不存在')
  } finally {
    joining.value = false
  }
}

const copyInviteCode = async () => {
  const code = guildStore.currentGuild?.inviteCode
  if (!code) {
    ElMessage.warning('暂无邀请码')
    return
  }
  
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(code)
    } else {
      const textArea = document.createElement('textarea')
      textArea.value = code
      textArea.style.position = 'fixed'
      textArea.style.left = '-9999px'
      document.body.appendChild(textArea)
      textArea.focus()
      textArea.select()
      document.execCommand('copy')
      document.body.removeChild(textArea)
    }
    ElMessage.success('邀请码已复制: ' + code)
  } catch (error) {
    console.error('复制失败', error)
    ElMessage.error('复制失败，请手动复制: ' + code)
  }
}

const fetchPublicGuilds = async () => {
  loadingPublic.value = true
  try {
    publicGuilds.value = await guildApis.getPublicGuilds()
  } catch (error) {
    console.error('获取公开服务器失败', error)
  } finally {
    loadingPublic.value = false
  }
}

const joinPublicGuild = async (guildId: number) => {
  joiningGuildId.value = guildId
  try {
    await guildApis.joinGuild(guildId)
    ElMessage.success('成功加入服务器')
    await guildStore.fetchGuilds()
    showExploreDialog.value = false
    const guild = guildStore.guilds.find(g => g.id === guildId)
    if (guild) {
      guildStore.setCurrentGuild(guild)
    }
  } catch (error) {
    console.error('加入服务器失败', error)
    ElMessage.error('加入服务器失败')
  } finally {
    joiningGuildId.value = null
  }
}

watch(showExploreDialog, (val) => {
  if (val) {
    fetchPublicGuilds()
  }
})

defineExpose({
  showInviteDialog,
})
</script>

<style scoped lang="scss">
.guild-sidebar {
  width: 72px;
  height: 100%;
  background: linear-gradient(180deg, var(--color-surface-0) 0%, var(--color-surface-1) 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-3) 0;
  border-right: 1px solid var(--color-border-secondary);
  box-shadow: 2px 0 8px rgb(0 0 0 / 5%);
}

.guild-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
  width: 100%;
  padding: 0 var(--spacing-3);
}

.guild-item {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--transition-fast) var(--ease-out);
  animation: fadeInUp var(--transition-normal) var(--ease-out);
  
  &:hover {
    border-radius: var(--radius-xl);
    transform: scale(1.05);
    
    .guild-indicator {
      height: 20px;
    }
  }
  
  &.active {
    .guild-icon {
      border-radius: var(--radius-xl);
      background: linear-gradient(135deg, var(--color-brand-gradient-start), var(--color-brand-gradient-end));
      box-shadow: var(--shadow-glow-primary);
    }
    
    .guild-indicator {
      height: 40px;
      background: linear-gradient(180deg, var(--color-brand-gradient-start), var(--color-brand-gradient-end));
    }
  }
  
  &.add-guild {
    .guild-icon {
      background-color: var(--color-surface-2);
      color: var(--color-success-400);
      border: 2px dashed var(--color-success-500);
      
      &:hover {
        background-color: var(--color-success-500);
        color: var(--color-neutral-0);
        border-color: var(--color-success-500);
        box-shadow: var(--shadow-glow-success);
      }
    }
  }
}

.guild-icon {
  width: 100%;
  height: 100%;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--color-surface-2);
  overflow: hidden;
  transition: all var(--transition-fast) var(--ease-out);
  border: 2px solid transparent;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.guild-name-initial {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.guild-indicator {
  position: absolute;
  left: -16px;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 8px;
  background-color: var(--color-text-primary);
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  transition: height var(--transition-fast) var(--ease-out);
}

.add-options {
  display: flex;
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-5);
}

.option-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-5);
  border: 2px solid var(--color-border-primary);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all var(--transition-fast) var(--ease-out);
  
  &:hover {
    border-color: var(--color-primary-400);
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }
  
  &.active {
    border-color: var(--color-primary-500);
    background: linear-gradient(135deg, rgb(3 169 244 / 10%), rgb(3 169 244 / 5%));
    box-shadow: var(--shadow-glow-primary);
  }
  
  .option-title {
    margin-top: var(--spacing-3);
    font-weight: var(--font-weight-semibold);
    color: var(--color-text-primary);
  }
  
  .option-desc {
    margin-top: var(--spacing-1);
    font-size: var(--font-size-xs);
    color: var(--color-text-tertiary);
  }
}

.create-form,
.join-form {
  margin-top: var(--spacing-4);
}

.invite-content {
  text-align: center;
}

.invite-tip {
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-4);
}

.invite-code-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-3);
  padding: var(--spacing-4);
  background: linear-gradient(135deg, var(--color-surface-2), var(--color-surface-3));
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border-primary);
}

.invite-code {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  letter-spacing: var(--letter-spacing-wider);
  background: linear-gradient(135deg, var(--color-brand-gradient-start), var(--color-brand-gradient-end));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.explore-guild {
  .guild-icon {
    background-color: var(--color-surface-2);
    color: var(--color-primary-400);
    border: 2px solid var(--color-primary-500);
    
    &:hover {
      background: linear-gradient(135deg, var(--color-brand-gradient-start), var(--color-brand-gradient-end));
      color: var(--color-neutral-0);
      border-color: transparent;
      box-shadow: var(--shadow-glow-primary);
    }
  }
}

.explore-content {
  min-height: 200px;
}

.loading-container,
.empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: var(--color-text-tertiary);
}

.guild-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-3);
}

.guild-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  padding: var(--spacing-3);
  background: var(--color-surface-2);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border-primary);
  transition: all var(--transition-fast) var(--ease-out);
  
  &:hover {
    background: var(--color-surface-3);
    border-color: var(--color-primary-500);
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }
}

.guild-card-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-brand-gradient-start), var(--color-brand-gradient-end));
  color: var(--color-neutral-0);
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-lg);
  overflow: hidden;
  flex-shrink: 0;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.guild-card-info {
  flex: 1;
  min-width: 0;
}

.guild-card-name {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guild-card-desc {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: var(--spacing-1);
}

.guild-card-meta {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-top: var(--spacing-1);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
