import eventBus from '@/utils/eventBus'
import apis from '@/services/apis'
import type { MsgReadUnReadCountType } from '@/services/types'
import type { Method } from 'alova'
import type { FetchRequestInit } from 'alova/GlobalFetch'
import type { Ref } from 'vue'

type RequestType = Method<
  Ref<unknown>,
  Ref<unknown>,
  MsgReadUnReadCountType[],
  unknown,
  FetchRequestInit,
  Response,
  Headers
>

const queueByRoom = new Map<number, Set<number>>()
let timer: number | null = null
const requests = new Map<number, RequestType>()

const onAddReadCountTask = ({ msgId, roomId }: { msgId: number; roomId: number }) => {
  if (!queueByRoom.has(roomId)) {
    queueByRoom.set(roomId, new Set())
  }
  queueByRoom.get(roomId)!.add(msgId)
}

const onRemoveReadCountTask = ({ msgId, roomId }: { msgId: number; roomId: number }) => {
  queueByRoom.get(roomId)?.delete(msgId)
  if (queueByRoom.get(roomId)?.size === 0) {
    queueByRoom.delete(roomId)
  }
}

const task = () => {
  requests.forEach((req) => req?.abort())
  requests.clear()

  queueByRoom.forEach((msgIds, roomId) => {
    if (msgIds.size > 0) {
      const request = apis.getMsgReadCount({ params: { msgIds: [...msgIds] } })
      requests.set(roomId, request)
      request
        .send()
        .then((res) => {
          const result = new Map<number, MsgReadUnReadCountType>()
          res.forEach((item) => result.set(item.msgId, item))
          eventBus.emit('onGetReadCount', result)
          requests.delete(roomId)
        })
        .catch(() => {
          requests.delete(roomId)
        })
    }
  })
}

export const initListener = () => {
  eventBus.on('onAddReadCountTask', onAddReadCountTask)
  eventBus.on('onRemoveReadCountTask', onRemoveReadCountTask)
  clearQueue()
}

export const clearListener = () => {
  eventBus.off('onAddReadCountTask', onAddReadCountTask)
  eventBus.off('onRemoveReadCountTask', onRemoveReadCountTask)
  timer && clearInterval(timer)
}

export const clearQueue = () => {
  queueByRoom.clear()
  timer && clearInterval(timer)
}

export const readCountQueue = () => {
  task()
  timer = setInterval(task, 10000)
}
