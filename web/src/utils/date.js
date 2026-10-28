export function toDateInput(timestamp) {
    const date = new Date(timestamp || Date.now())
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${date.getFullYear()}-${month}-${day}`
}

export function fromDateInput(value) {
    if (!value) {
        return null
    }
    const [year, month, day] = value.split('-').map(Number)
    return new Date(year, month - 1, day).getTime()
}

export function formatDate(timestamp) {
    if (!timestamp) {
        return ''
    }
    const date = new Date(timestamp)
    const datePart = date.toLocaleDateString('zh-CN', {
        year: 'numeric', month: 'long', day: 'numeric'
    })
    const weekday = date.toLocaleDateString('zh-CN', {weekday: 'short'})
    return `${datePart} · ${weekday}`
}

export function formatDateTime(timestamp) {
    if (!timestamp) {
        return ''
    }
    return new Date(timestamp).toLocaleString('zh-CN', {
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', hour12: false
    })
}
