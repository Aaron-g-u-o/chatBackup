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
  private localStream: MediaStream | null = null
  private voiceRoomId: number | null = null
  private uid: number | null = null
  private onRemoteStreamCallback: ((uid: number, stream: MediaStream) => void) | null = null
  private onRemoteStreamRemovedCallback: ((uid: number) => void) | null = null
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
  ) {
    this.onRemoteStreamCallback = onRemoteStream
    this.onRemoteStreamRemovedCallback = onRemoteStreamRemoved
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
    
    if (this.localStream) {
      this.localStream.getTracks().forEach((track) => track.stop())
      this.localStream = null
    }
    
    this.voiceRoomId = null
    this.uid = null
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
    }
  }

  private async handleCandidate(signal: VoiceSignalType) {
    if (!signal.fromUid || !signal.candidate) return
    
    const pc = this.peerConnections.get(signal.fromUid)
    if (pc) {
      await pc.addIceCandidate(JSON.parse(signal.candidate))
    }
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
      if (event.streams[0]) {
        this.onRemoteStreamCallback?.(targetUid, event.streams[0])
      }
    }

    pc.onconnectionstatechange = () => {
      if (pc.connectionState === 'disconnected' || pc.connectionState === 'failed') {
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
}

export const voiceChat = new VoiceChat()
