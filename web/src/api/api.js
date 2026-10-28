import axios from './axios.config'

export default {
    async login(params) {
        return axios.post('/user/login', params)
    },
    async getAccount() {
        return axios.get('/account')
    },
    async getConfigData() {
        return axios.get('/user/config')
    },
    async report(param) {
        return axios.put('/user/report', param)
    },
    async deleteReport(day) {
        return axios.delete('/user/report/' + day)
    },
    async getReportList(count, offset, filters = {}) {
        return axios.get('/user/report_list', {
            params: {count, offset, paged: true, ...filters}
        })
    },
    async getReceivedReports(count, offset, filters = {}) {
        return axios.get('/user/received_list', {
            params: {count, offset, ...filters}
        })
    },
    async getReportFeed(count, filters = {}) {
        return axios.get('/user/report_feed', {params: {count, ...filters}})
    },
    async getReceivedFeed(count, filters = {}) {
        return axios.get('/user/received_feed', {params: {count, ...filters}})
    },
    async getReportStats() {
        return axios.get('/user/report_stats')
    },
    async getReport(userId, day) {
        return axios.get('/user/report/' + encodeURIComponent(userId) + '/' + day)
    }
}
