<template>
    <div class="state-view login-error">
        <strong>自动登录失败</strong>
        <p>请确认已在野火客户端中打开应用，然后重试。</p>
        <button class="primary-button" @click="$emit('retry')">重新登录</button>
    </div>
</template>

<script>
export default {
    name: "ErrorView",
    emits: ['retry']
}
</script>

<style scoped>
.login-error p { margin: 12px 0 22px; font-size: 13px; }
</style>
