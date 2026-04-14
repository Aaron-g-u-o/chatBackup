import { createAlova } from 'alova'
import GlobalFetch from 'alova/GlobalFetch'
import VueHook from 'alova/vue'
import { ElMessage } from 'element-plus'

function getToken() {
  let tempToken = ''
  return {
    get() {
      if (tempToken) return tempToken
      const token = localStorage.getItem('TOKEN')
      if (token) {
        tempToken = token
      }
      return tempToken
    },
    clear() {
      tempToken = ''
    },
  }
}

export const computedToken = getToken()

export const alovaIns = createAlova({
  baseURL: '',
  statesHook: VueHook,
  requestAdapter: GlobalFetch(),
  timeout: 30000,
  
  beforeRequest({ config }) {
    config.headers.Authorization = `Bearer ${computedToken.get()}`
    config.headers['Content-Type'] = 'application/json; charset=utf-8'
  },

  responsed: async (response) => {
    const json = await response.json()
    if (response.status !== 200 || !json.success) {
      if (json.errMsg) {
        if (!computedToken.get() && response.status === 401) {
          //
        } else {
          ElMessage.error(json.errMsg)
        }
        throw new Error(json.errMsg)
      } else {
        throw new Error(json.message)
      }
    }
    return json.data
  },
})
