<template>
    <Teleport to="body">
        <Transition name="toast">
            <div v-if="visible" class="toast-host" :class="`toast-${type}`" role="status" aria-live="polite">
                <span class="toast-icon" aria-hidden="true">{{ type === 'error' ? '!' : '✓' }}</span>
                <span>{{ message }}</span>
            </div>
        </Transition>
    </Teleport>
</template>

<script>
import {TOAST_EVENT, TOAST_KEY, consumeToast} from '@/utils/toast'

export default {
    name: 'ToastHost',
    props: {
        ready: {type: Boolean, default: true}
    },
    data() {
        return {
            visible: false,
            message: '',
            type: 'success',
            timer: null
        }
    },
    watch: {
        ready: {
            immediate: true,
            handler(isReady) {
                if (isReady) {
                    this.checkToast()
                }
            }
        }
    },
    mounted() {
        document.addEventListener('visibilitychange', this.handleVisibilityChange)
        window.addEventListener('focus', this.checkToast)
        window.addEventListener(TOAST_EVENT, this.checkToast)
        window.addEventListener('storage', this.handleStorage)
    },
    beforeUnmount() {
        document.removeEventListener('visibilitychange', this.handleVisibilityChange)
        window.removeEventListener('focus', this.checkToast)
        window.removeEventListener(TOAST_EVENT, this.checkToast)
        window.removeEventListener('storage', this.handleStorage)
        if (this.timer) {
            window.clearTimeout(this.timer)
        }
    },
    methods: {
        handleStorage(event) {
            if (event && event.key === TOAST_KEY && event.newValue) {
                this.checkToast()
            }
        },
        handleVisibilityChange() {
            if (document.visibilityState === 'visible') {
                this.checkToast()
            }
        },
        checkToast() {
            if (!this.ready) {
                return
            }
            const toast = consumeToast()
            if (!toast || !toast.message) {
                return
            }
            this.message = toast.message
            this.type = toast.type || 'success'
            this.visible = true
            if (this.timer) {
                window.clearTimeout(this.timer)
            }
            this.timer = window.setTimeout(() => {
                this.visible = false
                this.timer = null
            }, 3200)
        }
    }
}
</script>

<style scoped>
.toast-host {
    position: fixed;
    z-index: 4000;
    left: 50%;
    bottom: calc(20px + env(safe-area-inset-bottom));
    display: flex;
    align-items: center;
    gap: 9px;
    max-width: calc(100vw - 32px);
    padding: 11px 16px;
    border-radius: 10px;
    background: #101828;
    box-shadow: 0 10px 30px rgba(16, 24, 40, .24);
    color: #fff;
    font-size: 14px;
    line-height: 1.4;
    text-align: left;
    transform: translateX(-50%);
}

.toast-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 18px;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: rgba(255, 255, 255, .16);
    font-size: 12px;
    font-weight: 700;
}

.toast-error .toast-icon {
    background: #f97066;
}

.toast-enter-active,
.toast-leave-active {
    transition: opacity .2s ease, transform .2s ease;
}

.toast-enter-from,
.toast-leave-to {
    opacity: 0;
    transform: translate(-50%, 8px);
}

@media (prefers-reduced-motion: reduce) {
    .toast-enter-active,
    .toast-leave-active {
        transition: none;
    }
}
</style>
