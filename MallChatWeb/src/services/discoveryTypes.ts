export enum RecommendTypeEnum {
  PERSONALIZED = 0,
  POPULAR = 1,
  NEWEST = 2,
  TRENDING = 3,
}

export enum PrivacyLevelEnum {
  FULL_PERSONALIZED = 0,
  JOIN_HISTORY_ONLY = 1,
  POPULAR_ONLY = 2,
}

export type RecommendedGuildType = {
  id: number
  name: string
  icon: string | null
  description: string | null
  category: string | null
  tags: string[]
  memberCount: number
  language: string | null
  activityLevel: number | null
  isJoined: boolean
  relevanceScore: number
  recommendSource: number
  recommendSourceDesc: string
  onlineCount: number
}

export type TagRespType = {
  id: number
  name: string
  category: string
  weight: number
}

export type TagCategoryRespType = {
  category: string
  tags: TagRespType[]
}

export type UserInterestTagRespType = {
  tagId: number
  tagName: string
  category: string
  weight: number
  source: number
}

export type UserInterestRespType = {
  privacyLevel: number
  diversityPreference: number
  interestTags: UserInterestTagRespType[]
}

export type DiscoveryPageRespType = {
  list: RecommendedGuildType[]
  isLast: boolean
  categories: TagCategoryRespType[]
  userInterest: UserInterestRespType | null
}

export type RecommendationMetricsRespType = {
  totalRecommendations: number
  totalClicks: number
  totalJoins: number
  clickThroughRate: number
  conversionRate: number
  avgRelevanceScore: number
}

export type DiscoveryReqType = {
  recommendType?: RecommendTypeEnum
  category?: string
  minMembers?: number
  maxMembers?: number
  language?: string
  activityLevel?: number
  page?: number
  pageSize?: number
}
