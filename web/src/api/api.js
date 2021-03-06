import axios from './axios.config'

export default {
    // 授权认证相关
    async login(params) {
        return axios.post('/user/login', params)
    },
    async getAccount() {
        return axios.get('/account')
    },
    async getConfigData() {
        return axios.get('/user/config')
    },

    // 业务相关api
    async report(param) {
        return axios.put('/user/report', param)
    },

    async deleteReport(day) {
        return axios.delete('/user/report/' + day)
    },
    async getReportList(count, offset) {
        return axios.get('/user/report_list' + `?count=${count}&offset=${offset}`)
    },
    async getReport(userId, day) {
        return axios.get('/user/report/' + userId + '/' + day)
    }

    // TODO more
}
