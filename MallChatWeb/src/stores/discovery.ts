import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  RecommendedGuildType,
  TagCategoryRespType,
  UserInterestRespType,
  DiscoveryReqType,
} from '@/services/discoveryTypes'
import { RecommendTypeEnum } from '@/services/discoveryTypes'
import discoveryApis from '@/services/discoveryApis'
import guildApis from '@/services/guildApis'
import { useGuildStore } from '@/stores/guild'

export const useDiscoveryStore = defineStore('discovery', () => {
  const recommendations = ref<RecommendedGuildType[]>([])
  const categories = ref<TagCategoryRespType[]>([])
  const userInterest = ref<UserInterestRespType | null>(null)
  const loading = ref(false)
  const isLast = ref(false)
  const currentType = ref<RecommendTypeEnum>(RecommendTypeEnum.PERSONALIZED)
  const currentPage = ref(1)
  const selectedCategory = ref<string | undefined>(undefined)
  const selectedActivityLevel = ref<number | undefined>(undefined)
  const searchKeyword = ref('')

  const filteredRecommendations = computed(() => {
    let list = recommendations.value
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      list = list.filter(
        (g) =>
          g.name.toLowerCase().includes(keyword) ||
          (g.description && g.description.toLowerCase().includes(keyword)) ||
          g.tags.some((t) => t.toLowerCase().includes(keyword)),
      )
    }
    return list
  })

  const typeLabels: Record<RecommendTypeEnum, string> = {
    [RecommendTypeEnum.PERSONALIZED]: '为你推荐',
    [RecommendTypeEnum.POPULAR]: '热门',
    [RecommendTypeEnum.NEWEST]: '最新',
    [RecommendTypeEnum.TRENDING]: '趋势',
  }

  const fetchRecommendations = async (reset = true) => {
    if (loading.value) return
    if (!reset && isLast.value) return

    loading.value = true
    try {
      const params: DiscoveryReqType = {
        recommendType: currentType.value,
        category: selectedCategory.value,
        activityLevel: selectedActivityLevel.value,
        page: reset ? 1 : currentPage.value + 1,
        pageSize: 20,
      }

      const data = await discoveryApis.getRecommendations(params)
      if (data) {
        if (reset) {
          recommendations.value = data.list || []
          currentPage.value = 1
        } else {
          recommendations.value = [...recommendations.value, ...(data.list || [])]
          currentPage.value++
        }
        isLast.value = data.isLast
        if (data.categories) {
          categories.value = data.categories
        }
        if (data.userInterest) {
          userInterest.value = data.userInterest
        }
      }
    } catch (error) {
      console.error('获取推荐失败', error)
    } finally {
      loading.value = false
    }
  }

  const setRecommendType = (type: RecommendTypeEnum) => {
    currentType.value = type
    fetchRecommendations(true)
  }

  const setCategory = (category: string | undefined) => {
    selectedCategory.value = category
    fetchRecommendations(true)
  }

  const setActivityLevel = (level: number | undefined) => {
    selectedActivityLevel.value = level
    fetchRecommendations(true)
  }

  const joinGuild = async (guildId: number) => {
    try {
      await guildApis.joinGuild(guildId)
      await discoveryApis.trackJoin(guildId)
      const guild = recommendations.value.find((g) => g.id === guildId)
      if (guild) {
        guild.isJoined = true
      }
      const guildStore = useGuildStore()
      await guildStore.fetchGuilds()
      return true
    } catch (error) {
      console.error('加入服务器失败', error)
      return false
    }
  }

  const dismissGuild = async (guildId: number, reason?: string) => {
    try {
      await discoveryApis.dismissRecommendation(guildId, reason)
      recommendations.value = recommendations.value.filter((g) => g.id !== guildId)
      return true
    } catch (error) {
      console.error('操作失败', error)
      return false
    }
  }

  const trackClick = async (guildId: number) => {
    try {
      await discoveryApis.trackClick(guildId, currentType.value)
    } catch (error) {
      console.error('追踪点击失败', error)
    }
  }

  const updatePrivacy = async (privacyLevel: number, diversityPreference?: number) => {
    try {
      await discoveryApis.updatePrivacy(privacyLevel, diversityPreference)
      if (userInterest.value) {
        userInterest.value.privacyLevel = privacyLevel
        if (diversityPreference !== undefined) {
          userInterest.value.diversityPreference = diversityPreference
        }
      }
    } catch (error) {
      console.error('更新隐私设置失败', error)
    }
  }

  const updateInterestTags = async (tagIds: number[]) => {
    try {
      await discoveryApis.updateInterestTags(tagIds)
      await fetchRecommendations(true)
    } catch (error) {
      console.error('更新兴趣标签失败', error)
    }
  }

  const buildProfile = async () => {
    try {
      await discoveryApis.buildUserProfile()
    } catch (error) {
      console.error('构建用户画像失败', error)
    }
  }

  return {
    recommendations,
    filteredRecommendations,
    categories,
    userInterest,
    loading,
    isLast,
    currentType,
    currentPage,
    selectedCategory,
    selectedActivityLevel,
    searchKeyword,
    typeLabels,
    fetchRecommendations,
    setRecommendType,
    setCategory,
    setActivityLevel,
    joinGuild,
    dismissGuild,
    trackClick,
    updatePrivacy,
    updateInterestTags,
    buildProfile,
  }
})
