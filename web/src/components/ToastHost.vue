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
import {consumeToast} from '@/utils/toast'

export default {
    name: 'ToastHost',
    data() {
        return {
            visible: false,
            message: '',
            type: 'success',
            timer: null
        }
    },
    mounted() {
        const toast = consumeToast()
        if (!toast || !toast.message) {
            return
        }
        this.message = toast.message
        this.type = toast.type || 'success'
        this.visible = true
        this.timer = window.setTimeout(() => {
            this.visible = false
        }, 3200)
    },
    beforeUnmount() {
        if (this.timer) {
            window.clearTimeout(this.timer)
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
