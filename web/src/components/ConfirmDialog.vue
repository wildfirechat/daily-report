<template>
    <Teleport to="body">
        <Transition name="dialog">
            <div v-if="visible" class="dialog-overlay" @click.self="onCancel" @keydown.esc="onCancel">
                <div class="dialog-card" role="dialog" aria-modal="true"
                     aria-labelledby="dialog-title" aria-describedby="dialog-message">
                    <h2 id="dialog-title">{{ title }}</h2>
                    <p id="dialog-message">{{ message }}</p>
                    <div class="dialog-actions">
                        <button ref="cancelButton" class="secondary-button" type="button"
                                :disabled="loading" @click="onCancel">{{ cancelText }}</button>
                        <button class="primary-button" :class="{danger: danger}" type="button"
                                :disabled="loading" @click="onConfirm">
                            {{ loading ? loadingText : confirmText }}
                        </button>
                    </div>
                </div>
            </div>
        </Transition>
    </Teleport>
</template>

<script>
export default {
    name: 'ConfirmDialog',
    props: {
        visible: {type: Boolean, default: false},
        title: {type: String, default: '请确认'},
        message: {type: String, default: ''},
        confirmText: {type: String, default: '确定'},
        cancelText: {type: String, default: '取消'},
        loadingText: {type: String, default: '处理中...'},
        danger: {type: Boolean, default: false},
        loading: {type: Boolean, default: false}
    },
    emits: ['confirm', 'cancel'],
    watch: {
        visible(value) {
            if (value) {
                document.body.style.overflow = 'hidden'
                this.$nextTick(() => {
                    if (this.$refs.cancelButton) {
                        this.$refs.cancelButton.focus()
                    }
                })
            } else {
                document.body.style.overflow = ''
            }
        }
    },
    beforeUnmount() {
        document.body.style.overflow = ''
    },
    methods: {
        onCancel() {
            if (!this.loading) {
                this.$emit('cancel')
            }
        },
        onConfirm() {
            if (!this.loading) {
                this.$emit('confirm')
            }
        }
    }
}
</script>

<style scoped>
.dialog-overlay {
    position: fixed;
    z-index: 3000;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    background: rgba(16, 24, 40, .52);
}

.dialog-card {
    width: min(400px, 100%);
    padding: 24px;
    border-radius: 14px;
    background: #fff;
    box-shadow: 0 24px 64px rgba(16, 24, 40, .24);
    text-align: left;
}

.dialog-card h2 {
    color: #1f2937;
    font-size: 18px;
    line-height: 1.4;
}

.dialog-card p {
    margin-top: 10px;
    color: #667085;
    font-size: 14px;
    line-height: 1.7;
}

.dialog-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 24px;
}

.dialog-actions button {
    min-width: 88px;
}

.dialog-actions .primary-button.danger {
    border-color: #d92d20;
    background: #d92d20;
    color: #fff;
}

.dialog-actions .primary-button.danger:hover {
    border-color: #b42318;
    background: #b42318;
}

.dialog-enter-active,
.dialog-leave-active {
    transition: opacity .18s ease;
}

.dialog-enter-active .dialog-card,
.dialog-leave-active .dialog-card {
    transition: transform .18s ease, opacity .18s ease;
}

.dialog-enter-from,
.dialog-leave-to {
    opacity: 0;
}

.dialog-enter-from .dialog-card,
.dialog-leave-to .dialog-card {
    opacity: 0;
    transform: translateY(6px) scale(.98);
}

@media (prefers-reduced-motion: reduce) {
    .dialog-enter-active,
    .dialog-leave-active,
    .dialog-enter-active .dialog-card,
    .dialog-leave-active .dialog-card {
        transition: none;
    }
}
</style>
