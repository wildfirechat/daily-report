import Axios from 'axios'
// axios实例
const instance = Axios.create({
    // 针对实际情况进行修改
    //baseURL: 'https://report.wildfirechat.cn/api',
    baseURL: '/api',
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json;charset=utf-8',
        'Accept': 'application/json;charset=utf-8',
        'authToken': localStorage.getItem('authToken'),
    }
})
instance.interceptors.request.use(request => {
    request.headers['authToken'] = localStorage.getItem('authToken');
    console.log('request', request);
    return request;
})

instance.interceptors.response.use(response => {
    let {code, message, result} = response.data
    if (code === 0) {
        if (response.config.url === '/user/login') {
            let authToken = response.headers['authtoken'] ? response.headers['authtoken'] : response.headers['authToken']
            localStorage.setItem('authToken', authToken);
        }
    } else {
        console.log('request error', code, message)
        return Promise.reject({code, message});
    }
    return result
});
export default instance