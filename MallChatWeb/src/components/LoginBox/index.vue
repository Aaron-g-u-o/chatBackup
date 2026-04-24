<script setup lang="ts">
import { computed, watchEffect } from 'vue'
import { useWsLoginStore, LoginStatus } from '@/stores/ws'
import QrCode from 'qrcode.vue'

const loginStore = useWsLoginStore()
const visible = computed({
  get() {
    return loginStore.showLogin
  },
  set(value) {
    loginStore.showLogin = value
  },
})

const loginQrCode = computed(() => loginStore.loginQrCode)
const loginStatus = computed(() => loginStore.loginStatus)

watchEffect(() => {
  if (visible.value && !loginQrCode.value) {
    loginStore.getLoginQrCode()
  }
})
</script>

<template>
  <ElDialog 
    class="login-box-modal" 
    :width="400" 
    v-model="visible" 
    center
    :show-close="true"
    :close-on-click-modal="false"
  >
    <div class="login-box">
      <div class="login-qrcode-container">
        <div class="login-qrcode-wrapper" v-loading="!loginQrCode">
          <div class="qrcode-frame">
            <div class="qrcode-corner top-left"></div>
            <div class="qrcode-corner top-right"></div>
            <div class="qrcode-corner bottom-left"></div>
            <div class="qrcode-corner bottom-right"></div>
          </div>
          <QrCode
            class="login-qrcode"
            v-if="loginQrCode"
            :value="loginQrCode"
            :size="240"
            :margin="0"
            level="H"
          />
        </div>
      </div>

      <div class="login-status">
        <transition name="fade-slide" mode="out-in">
          <div v-if="loginStatus === LoginStatus.Waiting" class="login-desc success">
            <div class="status-icon success">
              <ElIcon :size="24"><IEpSuccessFilled /></ElIcon>
            </div>
            <div class="status-text">
              <span class="status-title">扫码成功</span>
              <span class="status-subtitle">请在手机上点击"登录"继续</span>
            </div>
          </div>
          <div v-else class="login-desc">
            <div class="status-icon">
              <Icon icon="weixin" :size="24" />
            </div>
            <div class="status-text">
              <span class="status-title">微信扫码登录</span>
              <span class="status-subtitle">打开微信扫一扫，快速登录</span>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </ElDialog>
</template>

<style lang="scss" scoped>
:global(.login-box-modal) {
  border-radius: var(--radius-3xl) !important;
  background: linear-gradient(145deg, var(--color-surface-1), var(--color-surface-2)) !important;
  border: 1px solid var(--color-border-primary) !important;
  box-shadow: 
    0 25px 50px -12px rgb(0 0 0 / 50%),
    0 0 0 1px rgb(255 255 255 / 5%) inset !important;
  overflow: hidden;
}

:global(.login-box-modal .el-dialog__header) {
  display: none;
}

:global(.login-box-modal .el-dialog__body) {
  padding: var(--spacing-8) !important;
}

:global(.login-box-modal .el-dialog__headerbtn) {
  top: var(--spacing-4) !important;
  right: var(--spacing-4) !important;
  width: 36px !important;
  height: 36px !important;
  border-radius: var(--radius-full) !important;
  background: var(--color-surface-2) !important;
  transition: all var(--transition-fast) var(--ease-out) !important;
  
  &:hover {
    background: var(--color-surface-3) !important;
    transform: rotate(90deg);
  }
  
  .el-dialog__close {
    color: var(--color-text-secondary) !important;
    font-size: 18px !important;
  }
  
  &:hover .el-dialog__close {
    color: var(--color-text-primary) !important;
  }
}

.login-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-6);
}

.login-qrcode-container {
  position: relative;
  padding: var(--spacing-4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-qrcode-wrapper {
  position: relative;
  width: 260px;
  height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-neutral-0);
  border-radius: var(--radius-2xl);
  box-shadow: 
    0 4px 16px rgb(0 0 0 / 20%),
    0 0 0 1px rgb(255 255 255 / 5%) inset;
  overflow: visible;
  padding: 10px;
}

.qrcode-frame {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10;
}

.qrcode-corner {
  position: absolute;
  width: 28px;
  height: 28px;
  
  &::before,
  &::after {
    content: '';
    position: absolute;
    background: linear-gradient(135deg, var(--color-brand-gradient-start), var(--color-brand-gradient-end));
    border-radius: 3px;
    box-shadow: 0 2px 4px rgb(0 0 0 / 20%);
  }
  
  &.top-left {
    top: 0;
    left: 0;
    
    &::before {
      top: 0;
      left: 0;
      width: 100%;
      height: 4px;
    }
    
    &::after {
      top: 0;
      left: 0;
      width: 4px;
      height: 100%;
    }
  }
  
  &.top-right {
    top: 0;
    right: 0;
    
    &::before {
      top: 0;
      right: 0;
      width: 100%;
      height: 4px;
    }
    
    &::after {
      top: 0;
      right: 0;
      width: 4px;
      height: 100%;
    }
  }
  
  &.bottom-left {
    bottom: 0;
    left: 0;
    
    &::before {
      bottom: 0;
      left: 0;
      width: 100%;
      height: 4px;
    }
    
    &::after {
      bottom: 0;
      left: 0;
      width: 4px;
      height: 100%;
    }
  }
  
  &.bottom-right {
    bottom: 0;
    right: 0;
    
    &::before {
      bottom: 0;
      right: 0;
      width: 100%;
      height: 4px;
    }
    
    &::after {
      bottom: 0;
      right: 0;
      width: 4px;
      height: 100%;
    }
  }
}

.login-qrcode {
  border-radius: var(--radius-lg);
  display: block;
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
}

.login-status {
  width: 100%;
  min-height: 64px;
}

.login-desc {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  padding: var(--spacing-4) var(--spacing-5);
  background: var(--color-surface-2);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border-primary);
  transition: all var(--transition-normal) var(--ease-out);
  
  &.success {
    background: linear-gradient(135deg, rgb(76 175 80 / 10%), rgb(76 175 80 / 5%));
    border-color: var(--color-success-500);
  }
}

.status-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: var(--radius-xl);
  background: var(--color-surface-3);
  color: var(--color-text-secondary);
  flex-shrink: 0;
  
  &.success {
    background: linear-gradient(135deg, var(--color-success-400), var(--color-success-600));
    color: var(--color-neutral-0);
    animation: pulse 2s ease-in-out infinite;
  }
}

.status-text {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-1);
}

.status-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.status-subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all var(--transition-normal) var(--ease-out);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

@media only screen and (max-width: 480px) {
  :global(.login-box-modal) {
    width: 95% !important;
    max-width: 360px !important;
    margin: 0 auto !important;
  }
  
  :global(.login-box-modal .el-dialog__body) {
    padding: var(--spacing-6) !important;
  }
  
  .login-qrcode-container {
    padding: var(--spacing-2);
  }
  
  .login-qrcode-wrapper {
    width: 220px;
    height: 220px;
    padding: 8px;
  }
  
  .qrcode-corner {
    width: 24px;
    height: 24px;
    
    &::before {
      height: 3px;
    }
    
    &::after {
      width: 3px;
    }
  }
  
  .login-desc {
    padding: var(--spacing-3) var(--spacing-4);
  }
  
  .status-icon {
    width: 40px;
    height: 40px;
  }
}

@media only screen and (max-width: 360px) {
  :global(.login-box-modal) {
    width: 98% !important;
    max-width: 340px !important;
  }
  
  .login-qrcode-wrapper {
    width: 200px;
    height: 200px;
    padding: 6px;
  }
  
  .qrcode-corner {
    width: 20px;
    height: 20px;
    
    &::before {
      height: 2px;
    }
    
    &::after {
      width: 2px;
    }
  }
}

@media only screen and (min-width: 768px) {
  .login-qrcode-wrapper {
    width: 280px;
    height: 280px;
    padding: 12px;
  }
  
  .qrcode-corner {
    width: 32px;
    height: 32px;
    
    &::before {
      height: 5px;
    }
    
    &::after {
      width: 5px;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .status-icon.success {
    animation: none;
  }
  
  * {
    transition-duration: 0.01ms !important;
  }
}

@supports not (aspect-ratio: 1) {
  .login-qrcode-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .login-qrcode {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}
</style>
