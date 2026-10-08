# LostDesk 部署与备份 / Deployment and backup

知华科技（上海如静知华信息科技有限公司） · [官网](https://www.zhuatech.cn/) · 商业授权/定制微信 zhuatech、zhuatech2。

ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.) · [han@zhuatech.cn](mailto:han@zhuatech.cn), [jack@zhuatech.cn](mailto:jack@zhuatech.cn), [WhatsApp +86 17521234993](https://wa.me/8617521234993).

## 本机部署 / Local deployment

执行README中的init-env、Compose校验和完整build/up。MySQL与后端不暴露主机端口；Nginx默认仅127.0.0.1:8128。后端、前端为非root运行。健康、登录和CSRF引导允许匿名访问；业务接口和照片需要登录及对象范围。数据库卷包含业务记录、账号密码散列和私有照片。Docker Maven打包执行完整测试，不跳过。

Follow README: private environment generation, Compose validation and full build/up. MySQL/backend have no host ports. Nginx defaults to localhost:8128; frontend/backend run non-root. Health, sign-in and CSRF bootstrap are anonymous; business endpoints/photos require authenticated object scope. The database volume includes business data, password hashes and private photos. Backend Docker packaging runs all tests.

不要把down --volumes用于普通停机：它会删除指定数据库卷。重启使用docker compose -p lostdesk restart，再核查健康、登录和记录。升级前备份，保留已应用迁移校验值，新增版本迁移，不修改既有已应用脚本。

Do not use down --volumes for routine shutdown: it deletes that database. Restart services, then verify health/sign-in/data. Back up before upgrades, preserve applied migration checksums and add versioned migrations.

## 公网部署 / Internet deployment

另行配置授权服务器、HTTPS反向代理、域名/证书、COOKIE_SECURE=true、访问控制与监控。保持同源，限制登录和上载入口，制定机构数据保留与账号规则。外部MySQL使用最小权限账号、sslMode=VERIFY_IDENTITY与正确CA。未验证公网部署、生产容量、多节点会话或组织合规认证。

Configure an authorized server, HTTPS proxy/domain/certificate, secure cookies, access controls and monitoring. Keep same-origin routing and operate organizational retention/account policies. External MySQL requires least-privilege access, VERIFY_IDENTITY and a trusted CA. Internet deployment, production capacity, multi-node sessions and organizational compliance certification are not validated.

## 私有备份与恢复 / Private backup and restore

scripts/backup.py仅支持内置数据库：明确指定Compose项目，暂停其后端写入，单事务导出数据库，再重启；私有0600 ZIP包含SQL（账号散列、识别描述和照片）及SHA256 manifest。它不是可公开的源码包，不得上传Git。

scripts/restore.py只接受可信本机LostDesk备份，先检查成员、大小、产品和哈希；仅创建不存在的独立项目，拒绝覆盖已有容器/数据卷/网络。私有.env.restore设置独立端口和随机数据库口令。原数据库管理员口令仍来自原数据库；新bootstrap变量不会重置它。恢复后核查健康、登录、迁移、物品/认领/历史、照片哈希和CSV。

Backup stops writes in the exact named project, dumps the bundled database transactionally and restarts its backend. Private 0600 ZIP includes SQL (password hashes, private identifying descriptions/photos) plus a SHA256 manifest. It must never be published as source. Restore validates trusted local LostDesk archives and creates only an absent isolated project; existing containers/volumes/networks are rejected. Use private restore configuration with another port/random DB passwords. Restored administrator credentials remain unchanged. Verify health, sign-in, migrations, records/history, photo hashes and CSV.
