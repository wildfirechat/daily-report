# 野火日报服务

野火日报服务是[野火开放平台](https://github.com/wildfirechat/open-platform)中的应用示例，包含 Spring Boot 后端和 Vue 3 移动端页面，演示野火授权登录、联系人选择及消息通知。

## 功能

- 新建、编辑和删除日报，支持选择汇报日期、汇报对象和抄送对象。
- 未提交草稿自动保存在当前浏览器中，编辑时自动回填原日报。
- “我的日报”和“汇报箱”，支持关键词、日期范围筛选及分页加载。
- 今日提交状态、本月提交数、累计提交数和收到日报数统计。
- 作者、直接汇报对象和抄送对象可查看详情；只有作者可以修改或删除。
- 保存后异步发送野火富文本通知，通知服务异常不影响日报保存。
- 加载、空数据、失败重试、无权限及不存在等完整页面状态。

## 创建野火应用

在野火开放平台创建应用，填写应用地址，包括移动端地址、桌面端地址、回调/服务端地址。创建完成后得到 `appid` 和 `appsecret`。如果未配置回调/服务端地址，需要认证的工作台应用将不能正常登录。

## 配置

后端配置位于 `server/config/application.properties`：

```properties
# IM 服务地址，注意端口是 80，不是管理 API 的 18080。
im.url=http://wildfirechat.net
application.id=xxxxxxxx
application.secret=123456

# 服务对外访问地址，用于生成通知中的日报详情链接，末尾不要带 /。
report.base-url=https://report.example.com
# 默认开启；仅本地联调时关闭，避免向真实用户发送测试消息。
report.notification-enabled=true
# 有界通知线程池；队列满时丢弃通知并记录日志，避免阻塞请求线程。
report.notification.core-pool-size=4
report.notification.max-pool-size=8
report.notification.queue-capacity=1000

# CORS 允许的来源，逗号分隔；为空时仅允许同源。
cors.allowed-origins=
# 允许嵌入本应用的父页面来源（CSP frame-ancestors），默认仅同源。
# 如通过野火 Web 工作台 iframe 打开，请加入工作台的 origin。
security.frame-ancestors='self'
# 桌面端（Electron）工作台通过本机 WebSocket 与网页桥接，需要加入 CSP connect-src，
# 否则页面会被浏览器拦截、表现为一直“未登录/登录超时”。仅支持 ws:// / wss:// 前缀。
security.connect-src-extra=ws://127.0.0.1:7983 ws://localhost:7983
```

会话默认 30 分钟空闲超时，Cookie 为会话级；修改口令、退出登录会撤销服务端会话。

默认端口为 `8881`，默认使用文件型 H2 数据库。生产环境请切换到 MySQL，并妥善配置数据库账号和应用密钥。

### 桌面端（PC 客户端）说明

桌面端网页运行在 Electron 的 webview 中，登录依赖客户端注入的 `window.__wf_bridge_`（经本机 WebSocket `ws://127.0.0.1:7983` 与客户端主进程通信），因此：

- 必须配置 `security.connect-src-extra`，否则 CSP `connect-src` 会拦掉桥接 WebSocket；手机上通过原生 dsbridge 调用，不受影响。
- 客户端需使用正式打包版本（preload 不带 `eval`）；用 `eval` 方式打包的 preload 会被 CSP `script-src` 拦截，页面会提示“无法连接野火客户端”。
- 取 authCode 必须等客户端 `config` 成功（`ready` 事件）后再调用，本项目的 `BaseLayout` 已按此顺序处理。

`index.html` 等页面外壳已设置 `Cache-Control: no-cache`，并内置 `js/boot-check.js`：脚本加载失败时会显示具体失败地址，而不是白屏。升级部署后如果客户端仍白屏，通常是客户端缓存了旧 `index.html`（引用了已被删除的旧 hash 资源），强制刷新或清理客户端缓存一次即可；控制台的 `[daily-report] build <时间>` 可确认实际加载的版本。

## 构建

前端：

```bash
cd web
npm ci
npm run lint
npm run build
```

构建产物会自动复制到 `server/src/main/resources/static`。前端使用 Vue 3、Vue CLI 5 和 Webpack 5，支持当前 LTS 版本的 Node.js。

后端：

```bash
cd server
mvn clean package
```

生成的软件包为 `server/target/daily-report-0.1.0.jar`，包含前后端代码。

## 运行与升级

运行时把 `server/config` 目录复制到 jar 同级目录，然后执行：

```bash
java -jar daily-report-0.1.0.jar
```

也可以开发环境直接运行：

```bash
cd server
mvn spring-boot:run
```

本次升级为 `t_report` 增加了 `display_name`、`created_at`、`updated_at` 三列及作者查询索引，并新增 `t_report_recipient` 接收人关系表、`t_schema_migration` 迁移记录表。开发配置的 `spring.jpa.hibernate.ddl-auto=update` 会自动建表、补列和索引，已有日报仍可读取，旧记录的作者名回退为用户 ID。

生产升级前请备份数据库；如关闭自动 DDL，请先按实体定义手动创建上述可空列、关系表和索引。首次启动会按每批 500 篇把旧日报的 `report_to`、`cc_to` 数据迁移到接收人关系表，完成后写入 `report-recipient-v1` 标记，后续启动不会重复迁移。多实例部署首次升级时应先只启动一个实例，迁移完成后再启动其他实例。

## 性能设计

- 汇报箱和收到日报统计使用接收人关系表精确查询，不再扫描全部日报并模糊匹配逗号分隔字符串。
- 作者列表使用 `(user_id, day, updated_at)` 索引；接收人列表使用 `(recipient_id, report_day, report_user_id)` 索引。
- 新前端使用游标分页，加载更多不随页码增加而扫描越来越深；旧 offset 接口保留用于兼容。
- 关键词包含搜索仍可能需要扫描候选数据，数据量大时建议接入专用全文检索，并优先使用日期范围缩小结果。
- 通知在数据库事务提交后通过有界线程池发送，避免无限创建线程或积压任务。

## 主要接口

所有业务接口（登录和应用配置除外）都需要野火会话认证。

- `POST /api/logout`：退出登录并撤销当前会话。
- `PUT /api/user/report`：新建或更新当前用户的日报。
- `GET /api/user/report/{userId}/{day}`：按权限读取日报。
- `DELETE /api/user/report/{day}`：删除当前用户指定日期的日报。
- `GET /api/user/report_feed`：我的日报游标分页，下一页传 `cursorDay`。
- `GET /api/user/received_feed`：汇报箱游标分页，下一页传 `cursorDay`、`cursorUpdatedAt` 和 `cursorUserId`。
- `GET /api/user/report_list`：我的日报旧 offset 接口；传 `paged=true` 时返回 `items/total/hasMore`，否则保留原数组响应。
- `GET /api/user/received_list`：汇报箱旧 offset 分页接口。
- `GET /api/user/report_stats`：当前用户统计。

所有列表接口支持 `count`、`keyword`、`startDay`、`endDay`；旧接口另支持 `offset`，游标接口另支持上述游标参数。日期使用毫秒时间戳，`count` 最大为 100。游标接口返回 `items/hasMore/nextCursorDay/nextCursorUpdatedAt/nextCursorUserId`，其中 `total` 固定为 `-1`，表示未执行额外的全量计数。

## 验证

```bash
cd server
mvn test

cd ../web
npm run lint
npm run build
```

在手机或桌面野火客户端的工作台打开应用，验证登录、写日报、联系人选择、列表/详情、编辑删除、汇报箱和通知跳转。
