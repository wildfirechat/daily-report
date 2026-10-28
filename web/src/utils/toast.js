const TOAST_KEY = 'daily-report:toast'

export function queueToast(message, type = 'success') {
    try {
        sessionStorage.setItem(TOAST_KEY, JSON.stringify({message, type}))
    } catch (e) {
        // Storage may be unavailable in restricted WebViews; the toast is non-critical.
    }
}

export function consumeToast() {
    try {
        const raw = sessionStorage.getItem(TOAST_KEY)
        if (!raw) {
            return null
        }
        sessionStorage.removeItem(TOAST_KEY)
        return JSON.parse(raw)
    } catch (e) {
        return null
    }
}
