import type { VoiceSignalType } from '@/services/voiceTypes'
import { VoiceSignalTypeEnum } from '@/services/voiceTypes'
import wsIns from '@/utils/websocket'
import { WsRequestMsgType } from '@/utils/wsType'

const ICE_SERVERS = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' },
]

export class VoiceChat {
  private peerConnections: Map<number, RTCPeerConnection> = new Map()
  private pendingCandidates: Map<number, RTCIceCandidateInit[]> = new Map()
  private localStream: MediaStream | null = null
  private voiceRoomId: number | null = null
  private uid: number | null = null
  private onRemoteStreamCallback: ((uid: number, stream: MediaStream) => void) | null = null
  private onRemoteStreamRemovedCallback: ((uid: number) => void) | null = null
  private onConnectionStateChangeCallback: ((uid: number, state: RTCPeerConnectionState) => void) | null = null
  private muted: boolean = false
  private deafened: boolean = false

  async init(): Promise<MediaStream> {
    this.localStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
      },
      video: false,
    })
    return this.localStream
  }

  setCallbacks(
    onRemoteStream: (uid: number, stream: MediaStream) => void,
    onRemoteStreamRemoved: (uid: number) => void,
    onConnectionStateChange?: (uid: number, state: RTCPeerConnectionState) => void,
  ) {
    this.onRemoteStreamCallback = onRemoteStream
    this.onRemoteStreamRemovedCallback = onRemoteStreamRemoved
    this.onConnectionStateChangeCallback = onConnectionStateChange || null
  }

  joinRoom(voiceRoomId: number, uid: number) {
    this.voiceRoomId = voiceRoomId
    this.uid = uid
  }

  leaveRoom() {
    this.peerConnections.forEach((pc, uid) => {
      pc.close()
      this.onRemoteStreamRemovedCallback?.(uid)
    })
    this.peerConnections.clear()
    this.pendingCandidates.clear()
    
    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => track.stop())
      this.localStream = null
    }
    
    this.voiceRoomId = null
    this.uid = null
    this.muted = false
    this.deafened = false
  }

  closePeerConnection(targetUid: number) {
    const pc = this.peerConnections.get(targetUid)
    if (pc) {
      pc.close()
      this.peerConnections.delete(targetUid)
      this.pendingCandidates.delete(targetUid)
      this.onRemoteStreamRemovedCallback?.(targetUid)
    }
  }

  async createOffer(targetUid: number) {
    const pc = await this.createPeerConnection(targetUid)
    
    const offer = await pc.createOffer({
      offerToReceiveAudio: true,
      offerToReceiveVideo: false,
    })
    await pc.setLocalDescription(offer)
    
    this.sendSignal({
      type: VoiceSignalTypeEnum.OFFER,
      voiceRoomId: this.voiceRoomId!,
      targetUid,
      sdp: offer.sdp,
    })
  }

  async handleSignal(signal: VoiceSignalType) {
    if (!this.voiceRoomId || signal.voiceRoomId !== this.voiceRoomId) return

    switch (signal.type) {
      case VoiceSignalTypeEnum.OFFER:
        await this.handleOffer(signal)
        break
      case VoiceSignalTypeEnum.ANSWER:
        await this.handleAnswer(signal)
        break
      case VoiceSignalTypeEnum.CANDIDATE:
        await this.handleCandidate(signal)
        break
    }
  }

  private async handleOffer(signal: VoiceSignalType) {
    if (!signal.fromUid || !signal.sdp) return
    
    const pc = await this.createPeerConnection(signal.fromUid)
    
    await pc.setRemoteDescription({
      type: 'offer',
      sdp: signal.sdp,
    })
    
    await this.flushPendingCandidates(signal.fromUid, pc)
    
    const answer = await pc.createAnswer()
    await pc.setLocalDescription(answer)
    
    this.sendSignal({
      type: VoiceSignalTypeEnum.ANSWER,
      voiceRoomId: this.voiceRoomId!,
      targetUid: signal.fromUid,
      sdp: answer.sdp,
    })
  }

  private async handleAnswer(signal: VoiceSignalType) {
    if (!signal.fromUid || !signal.sdp) return
    
    const pc = this.peerConnections.get(signal.fromUid)
    if (pc) {
      await pc.setRemoteDescription({
        type: 'answer',
        sdp: signal.sdp,
      })
      
      await this.flushPendingCandidates(signal.fromUid, pc)
    }
  }

  private async handleCandidate(signal: VoiceSignalType) {
    if (!signal.fromUid || !signal.candidate) return
    
    const pc = this.peerConnections.get(signal.fromUid)
    if (pc && pc.remoteDescription) {
      try {
        await pc.addIceCandidate(JSON.parse(signal.candidate))
      } catch (e) {
        console.warn('添加ICE Candidate失败:', e)
      }
    } else {
      let candidates = this.pendingCandidates.get(signal.fromUid)
      if (!candidates) {
        candidates = []
        this.pendingCandidates.set(signal.fromUid, candidates)
      }
      candidates.push(JSON.parse(signal.candidate))
    }
  }

  private async flushPendingCandidates(uid: number, pc: RTCPeerConnection) {
    const candidates = this.pendingCandidates.get(uid)
    if (!candidates || candidates.length === 0) return
    
    for (const candidate of candidates) {
      try {
        await pc.addIceCandidate(candidate)
      } catch (e) {
        console.warn('刷新缓冲ICE Candidate失败:', e)
      }
    }
    this.pendingCandidates.delete(uid)
  }

  private async createPeerConnection(targetUid: number): Promise<RTCPeerConnection> {
    if (this.peerConnections.has(targetUid)) {
      return this.peerConnections.get(targetUid)!
    }

    const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS })

    pc.onicecandidate = (event) => {
      if (event.candidate) {
        this.sendSignal({
          type: VoiceSignalTypeEnum.CANDIDATE,
          voiceRoomId: this.voiceRoomId!,
          targetUid,
          candidate: JSON.stringify(event.candidate.toJSON()),
        })
      }
    }

    pc.ontrack = (event) => {
      if (event.streams && event.streams[0]) {
        this.onRemoteStreamCallback?.(targetUid, event.streams[0])
      }
    }

    pc.onconnectionstatechange = () => {
      this.onConnectionStateChangeCallback?.(targetUid, pc.connectionState)
      
      if (pc.connectionState === 'failed') {
        console.warn(`PeerConnection与用户${targetUid}连接失败，尝试重建`)
        pc.close()
        this.peerConnections.delete(targetUid)
        this.onRemoteStreamRemovedCallback?.(targetUid)
      } else if (pc.connectionState === 'disconnected') {
        console.warn(`PeerConnection与用户${targetUid}连接断开`)
      } else if (pc.connectionState === 'connected') {
        console.log(`PeerConnection与用户${targetUid}已建立`)
      }
    }

    pc.oniceconnectionstatechange = () => {
      if (pc.iceConnectionState === 'failed') {
        console.warn(`ICE连接与用户${targetUid}失败`)
        pc.close()
        this.peerConnections.delete(targetUid)
        this.onRemoteStreamRemovedCallback?.(targetUid)
      }
    }

    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => {
        if (this.localStream) {
          pc.addTrack(track, this.localStream)
        }
      })
    }

    this.peerConnections.set(targetUid, pc)
    return pc
  }

  private sendSignal(signal: Omit<VoiceSignalType, 'fromUid'>) {
    wsIns.send({
      type: WsRequestMsgType.VoiceSignal,
      data: JSON.stringify(signal),
    })
  }

  setMuted(muted: boolean) {
    this.muted = muted
    if (this.localStream) {
      this.localStream.getAudioTracks().forEach((track) => {
        track.enabled = !muted
      })
    }
  }

  setDeafened(deafened: boolean) {
    this.deafened = deafened
  }

  getMuted(): boolean {
    return this.muted
  }

  getDeafened(): boolean {
    return this.deafened
  }

  getLocalStream(): MediaStream | null {
    return this.localStream
  }

  getPeerConnections(): Map<number, RTCPeerConnection> {
    return this.peerConnections
  }

  getVoiceRoomId(): number | null {
    return this.voiceRoomId
  }
}

export const voiceChat = new VoiceChat()
