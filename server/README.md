# 服务模版
## 登陆
curl -H "Content-Type: application/json" -X POST  --data '{"account":"admin","password":"admin123"}' -v  http://127.0.0.1:8880/api/login

## 获取账号
curl -X POST -H "authToken:6d4be0cab0644bd39d68d98f34ce7fa0" -v http://127.0.0.1:8880/api/account
> authToken为第一步登陆成功返回的header中的authToken，如果用cookies也是可以的。

## 打开首页
浏览器打开 http://127.0.0.1:8880/
