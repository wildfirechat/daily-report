const webpack = require('webpack')

module.exports = {
    productionSourceMap: false,
    configureWebpack: {
        plugins: [
            // 构建时间，启动时打印到控制台，便于确认客户端加载的是哪一版页面
            new webpack.DefinePlugin({
                __BUILD_TIME__: JSON.stringify(new Date().toISOString())
            })
        ]
    },
    pages: {
        index: './src/main.js',
        myReports: './src/myReports.js',
        inbox: './src/inbox.js',
        reportList: './src/myReports.js',
        report: './src/report.js',
        newReport: './src/newReport.js'
    }
}
