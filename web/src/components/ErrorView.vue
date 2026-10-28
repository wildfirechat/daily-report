<template>
    <div class="state-view login-error" role="alert">
        <span class="error-mark" aria-hidden="true">!</span>
        <strong>{{ copy.title }}</strong>
        <p>{{ copy.description }}</p>
        <button class="primary-button" type="button" @click="$emit('retry')">重新登录</button>
    </div>
</template>

<script>
const COPY = {
    timeout: {
        title: '登录超时',
        description: '未收到客户端响应，请确认在野火客户端中打开应用后重试。'
    },
    bridge: {
        title: '无法连接野火客户端',
        description: '请从工作台打开应用，或确认当前客户端版本支持该应用。'
    },
    auth: {
        title: '登录未完成',
        description: '授权已取消或登录失败，请重试。'
    },
    network: {
        title: '网络连接失败',
        description: '请检查网络连接后重试。'
    },
    unknown: {
        title: '自动登录失败',
        description: '请确认已在野火客户端中打开应用，然后重试。'
    }
}

export default {
    name: "ErrorView",
    props: {
        type: {type: String, default: 'unknown'}
    },
    emits: ['retry'],
    computed: {
        copy() {
            return COPY[this.type] || COPY.unknown
        }
    }
}
</script>

<style scoped>
.login-error {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-height: 60vh;
    justify-content: center;
}

.error-mark {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 42px;
    height: 42px;
    margin-bottom: 14px;
    border-radius: 50%;
    background: #fff1f0;
    color: #d92d20;
    font-size: 22px;
    font-weight: 700;
}

.login-error p {
    max-width: 320px;
    margin: 12px 0 22px;
    color: #667085;
    font-size: 14px;
    line-height: 1.7;
}
</style>
