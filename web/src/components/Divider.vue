<template>
    <div class="divider" :style="{height:height, margin: margin}">

    </div>
</template>

<script>
export default {
    name: "Divider",
    props: {
        height: {
            type: String,
            required: false,
            default: '1px',
        },
        margin: {
            type: String,
            required: false,
            default: '0 10px'
        }
    }
}
</script>

<style scoped>

.divider {
    background-color: lightgrey;
}

</style>