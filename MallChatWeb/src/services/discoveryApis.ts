import type {
  DiscoveryPageRespType,
  DiscoveryReqType,
  RecommendationMetricsRespType,
  UserInterestRespType,
  TagCategoryRespType,
} from './discoveryTypes'
import { alovaIns } from './request'

const prefix = import.meta.env.PROD ? import.meta.env.VITE_API_PREFIX : ''

export default {
  getRecommendations: (params: DiscoveryReqType) =>
    alovaIns.Get<DiscoveryPageRespType>(`${prefix}/capi/discovery/recommend`, {
      params,
    }),

  trackClick: (guildId: number, recommendType: number) =>
    alovaIns.Post<void>(`${prefix}/capi/discovery/track/click`, {
      guildId,
      recommendType,
    }),

  trackJoin: (guildId: number) =>
    alovaIns.Post<void>(`${prefix}/capi/discovery/track/join/${guildId}`),

  dismissRecommendation: (guildId: number, reason?: string) =>
    alovaIns.Post<void>(`${prefix}/capi/discovery/dismiss`, {
      guildId,
      reason,
    }),

  getTags: () =>
    alovaIns.Get<TagCategoryRespType[]>(`${prefix}/capi/discovery/tags`),

  getUserInterest: () =>
    alovaIns.Get<UserInterestRespType>(`${prefix}/capi/discovery/interest`),

  updatePrivacy: (privacyLevel: number, diversityPreference?: number) =>
    alovaIns.Put<void>(`${prefix}/capi/discovery/interest/privacy`, {
      privacyLevel,
      diversityPreference,
    }),

  updateInterestTags: (tagIds: number[]) =>
    alovaIns.Put<void>(`${prefix}/capi/discovery/interest/tags`, {
      tagIds,
    }),

  buildUserProfile: () =>
    alovaIns.Post<void>(`${prefix}/capi/discovery/profile/build`),

  getMetrics: () =>
    alovaIns.Get<RecommendationMetricsRespType>(`${prefix}/capi/discovery/metrics`),
}
