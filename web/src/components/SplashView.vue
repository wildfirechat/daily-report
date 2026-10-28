<template>
    <div class="state-view splash-view">
        <span class="splash-spinner" aria-hidden="true"></span>
        <p>正在安全登录，请稍候...</p>
    </div>
</template>

<script>
export default {
    name: "SplashView"
}
</script>

<style scoped>
.splash-view {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 60vh;
    gap: 16px;
}

.splash-spinner {
    width: 26px;
    height: 26px;
    border: 2px solid #dbe4f3;
    border-top-color: #2f6fed;
    border-radius: 50%;
    animation: splash-spin .8s linear infinite;
}

.splash-view p {
    color: #667085;
    font-size: 14px;
}

@keyframes splash-spin {
    to {
        transform: rotate(360deg);
    }
}

@media (prefers-reduced-motion: reduce) {
    .splash-spinner {
        animation: none;
        border-top-color: #2f6fed;
    }
}
</style>
