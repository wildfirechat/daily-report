export const TOAST_KEY = 'daily-report:toast'
export const TOAST_EVENT = 'daily-report:toast'
const TOAST_TTL_MS = 10000

function getStorage() {
    try {
        if (typeof localStorage !== 'undefined') {
            return localStorage
        }
    } catch (e) {
        // Fallback to sessionStorage when localStorage is blocked.
    }
    return sessionStorage
}

export function queueToast(message, type = 'success') {
    const payload = JSON.stringify({message, type, expiresAt: Date.now() + TOAST_TTL_MS})
    try {
        getStorage().setItem(TOAST_KEY, payload)
    } catch (e) {
        try {
            sessionStorage.setItem(TOAST_KEY, payload)
        } catch (err) {
            // Storage may be unavailable in restricted WebViews; the toast is non-critical.
        }
    }
    try {
        if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
            window.dispatchEvent(new CustomEvent(TOAST_EVENT))
        }
    } catch (e) {
        // Ignore in environments without CustomEvent.
    }
}

function readAndClear(storage) {
    try {
        const raw = storage.getItem(TOAST_KEY)
        if (!raw) {
            return null
        }
        storage.removeItem(TOAST_KEY)
        const parsed = JSON.parse(raw)
        if (parsed && parsed.expiresAt && Date.now() > parsed.expiresAt) {
            return null
        }
        return parsed
    } catch (e) {
        return null
    }
}

export function consumeToast() {
    return readAndClear(getStorage()) || readAndClear(sessionStorage)
}
