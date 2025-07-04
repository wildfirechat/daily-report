# 野火日报服务
野火日报服务是[野火开放平台](https://github.com/wildfirechat/open-platform)中的一个demo应用，用来演示如何开发或则对接第三方应用系统。

## 创建应用
在野火开放平台创建应用，填写预先分配给*应用地址*，包括移动端地址、桌面端地址、回调/服务端地址（如果不配置的话，需要认证的工作台应用，将不能正常登录），创建完成后得到appid和appsecret。


## Demo前端的编译
进入到web目录，执行如下命令：
```
npm install
npm run build
```
编译成功后会自动放入到后端的static目录，不用手动拷贝。

## Demo后端的编译
进入到server目录，执行如下命令：
```
mvn clean package
```

## 配置
编译后的软件包在target目录下的```daily-report-0.1.jar```，包中已经包含了前后端的代码。运行时需要把server目录中的config目录拷贝到此jar的同级目录。
```
# IM服务地址，注意端口是80，不是管理API的18080.
im.url=http://wildfirechat.net
# 应用ID
application.id=xxxxxxxx
# 应用密钥
application.secret=123456
```
此外还有其它配置，比如端口和数据库可以配置。默认使用的是h2数据库，上线前请切换到MySQL或则其它关系型数据库。

## 运行
执行命令
```
nohup java -jar daily-report-0.1.jar 2>&1 &
```

## 验证
手机打开工作页面，点击创建的应用，进入应用测试。
