import Axios from 'axios'

const instance = Axios.create({
    baseURL: '/api',
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json;charset=utf-8',
        'Accept': 'application/json;charset=utf-8',
        'authToken': localStorage.getItem('authToken')
    }
})

instance.interceptors.request.use(request => {
    request.headers['authToken'] = localStorage.getItem('authToken')
    return request
})

instance.interceptors.response.use(response => {
    const {code, message, result} = response.data
    if (code === 0) {
        if (response.config.url === '/user/login') {
            const authToken = response.headers['authtoken'] || response.headers['authToken']
            localStorage.setItem('authToken', authToken)
        }
        return result
    }
    return Promise.reject({code, message})
}, error => {
    const data = error.response && error.response.data
    return Promise.reject({
        code: data && data.code,
        message: (data && data.message) || '网络连接失败，请检查网络后重试'
    })
})

export default instance
