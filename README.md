[中文](README.md) | [English](README.en.md)

# LostDesk 失物保管与认领管理系统 · Java 21 / Spring Boot / Vue 3

**知华科技（上海如静知华信息科技有限公司）** · [官网](https://www.zhuatech.cn/)

LostDesk 面向园区、学校、商场和场馆的失物保管/客服团队，管理“实际收存—本人报失—人工候选—特征核验—实物交出—本人收讫”的办理过程。它将工作人员的私有保管库存与失主的本人记录分开，避免通过公开完整照片和隐藏特征来认领物品。

**公开源码学习版／非商业源码版。** 仅限个人学习交流，未经上海如静知华信息科技有限公司书面授权不得商用。源码公开不代表MIT/Apache免费商用许可，详见[LICENSE](LICENSE)。

## 如何办理

1. 保管员登记实物编号、分类、拾获地点/日期、保管位置和私有识别特征，可上传私有PNG/JPEG照片。
2. 失主登录填写本人报失与私有特征，可查看自己的记录。库存不对普通失主开放。
3. 工作人员按服务点/分类/日期筛选人工候选，提出认领。失主补充能证明是本人所有的特征；另一位核验员填写核验依据及对失主的结果说明。
4. 实际收存日期可补录历史日期，拾获日期不能晚于收存日期；收存日期建立后不可变更，保管截止按登记时策略计算。批准只预留物品，保管员实际交出后，失主本人确认收讫，认领和报失才关闭。提出人不可自审，核验人不可兼作同件物品的交出人。
5. 无有效认领且达到登记时确定的保管期限的物品可申请处置；独立批准与实际执行分别记录。

人工候选不是自动图像识别或所有权判定；收讫记录不是电子签名或法定身份认证。到期是软件申请门禁，机构自行确定适用保管与处置规则。

## 已实现功能

| 端 / 职责 | 当前能力 |
| --- | --- |
| 保管员 | 收存、编号唯一性、分类/位置、未引用资料更正、位置转移、错误收存作废、候选提出、实物交出、领取到期整理、处置申请与执行 |
| 核验员 | 私有特征与失主证明核验、独立批准/拒绝、处置独立复核 |
| 失主端 | 本人报失和私有照片、未办理报失更正/撤回、本人认领特征、领取期限、取消未交出候选、本人收讫 |
| 管理端 | 账号/角色/权限/菜单/服务点、分类字典、时区与参数、密码重置、启停、最后管理员保护 |
| 统计与审计 | 授权范围实际指标、保管台账CSV、状态过滤/搜索/分页/排序、保管动作与账号审计 |

MySQL持久化业务、历史和私有照片；登录会话、CSRF、角色、服务点、本人记录、版本与状态由服务端检查。账号/服务点停用或密码重置撤销旧会话。普通失主不会收到隐藏特征、保管照片、其他失主信息或内部核验笔记。

新收存默认保管30天，批准后领取72小时，每名失主最多10条未结报失；既有物品/认领保留期限快照。GET不改变状态；保管员“整理过期认领”显式释放到期保留，没有自动定时任务或通知。

## 运行页面

以下截图是本系统真实运行的TEST验收数据，不是客户资料或客户案例。空库安装没有这些失物、报失或测试账号。

### 登录

私有账号登录，不提供共享口令。

![登录](docs/screenshots/01-login.jpg)

### 保管库存

编号、分类、位置和真实保管状态。

![保管库存](docs/screenshots/02-items.jpg)

### 物品详情

工作人员私有识别信息、保管照片与办理历史。

![物品详情](docs/screenshots/03-item.jpg)

### 报失处理

服务点工作人员处理实际报失记录。

![报失处理](docs/screenshots/04-reports.jpg)

### 认领候选

候选提出保留信息快照，不自动确认所有权。

![认领候选](docs/screenshots/05-proposal.jpg)

### 独立核验

隐藏特征、失主证据和独立核验记录。

![独立核验](docs/screenshots/06-review.jpg)

### 实物交接

保管员确认交出，失主本人确认收讫。

![实物交接](docs/screenshots/07-handover.jpg)

### 失主端

本人报失、认领、领取期限及收讫状态。

![失主端](docs/screenshots/08-claimant.jpg)

### 保管位置

服务点位置目录与占用/历史保护。

![保管位置](docs/screenshots/09-locations.jpg)

### 账号管理

管理员维护账号、角色、服务点与启停。

![账号管理](docs/screenshots/10-accounts.jpg)

### 角色权限

保管、核验、本人记录和服务点数据范围。

![角色权限](docs/screenshots/11-roles.jpg)

### 服务点与策略

分类、时区、新收存保管期与领取窗口。

![服务点与策略](docs/screenshots/12-settings.jpg)

### 统计台账

真实授权范围统计及不含隐藏特征的导出。

![统计台账](docs/screenshots/13-reports.jpg)

### 手机端报失

响应式本人操作，宽表格在容器内滚动。

![手机端报失](docs/screenshots/14-mobile.jpg)

### 英文界面

界面语言切换，不改变实际业务数据。

![英文界面](docs/screenshots/15-english.jpg)

### 到期处置

期满申请、独立复核、执行与撤销记录。

![到期处置](docs/screenshots/16-disposals.jpg)

## 架构与目录

Java 21、Spring Boot 4.0.7、Spring Security、JPA/Hibernate、Flyway；Vue 3.5.43、Vite 8.1.5、Lucide；MySQL 8.4、Nginx、Docker Compose。浏览器通过同源/api访问，照片由受鉴权端点读取且no-store。应用为单实例，数据库行锁串行化写入，防止重复批准和交出。BCrypt 12轮、HttpOnly/SameSite会话及CSRF独立执行。

```text
backend/src/main/java/cn/zhuatech/lostdesk/ # identity, custody, claims, photos
backend/src/main/resources/db/migration/  # V1 identity / V2 custody
backend/src/test/java/                    # real HTTP, boundary and concurrent tests
frontend/src/                            # staff and claimant Vue UI
frontend/public/brand/                   # original logo/contact assets
docs/                                   # operating/deployment/API/license notes
scripts/                                # private QA, backup/restore and release checks
compose.yaml                            # MySQL, backend and same-origin Nginx
.env.example                            # configuration names, no credentials
```

## 安装启动

需要Docker Engine/Desktop与Compose V2，可访问官方Maven/npm/Docker仓库。开发建议至少4GB可用内存。源码开发需要JDK21、Maven3.9+、Node.js24.19.0+、npm及Python3.10+；Compose自带MySQL，无需已有本机数据库。

```sh
python3 scripts/init-env.py
docker compose -p lostdesk config --quiet
docker compose -p lostdesk up -d --build --wait --wait-timeout 240
```

访问[http://127.0.0.1:8128/](http://127.0.0.1:8128/)，[健康检查](http://127.0.0.1:8128/actuator/health)。首次空库账号`admin`，从私有`.env`读取`ADMIN_PASSWORD`。初始化脚本随机生成口令、文件权限0600并拒绝覆盖。修改环境变量不重设已有数据库账号；不要上传`.env`。

首次操作先创建独立的保管员、核验员和失主账号，再维护实际服务点/位置、登记失物并提交本人报失。详细流程见[操作手册](docs/USER_GUIDE.md)。没有匿名报失或自主注册；机构管理员负责账号开通。

## 数据库与配置

`V1__identity.sql`建立身份/目录/参数/审计9张表；`V2__custody.sql`建立位置、收存、报失、认领、处置、保管事件、私有照片7张表。共16张应用表及Flyway历史表。迁移自动按版本执行；Hibernate验证结构，不重写数据库。空库初始化一个管理员、一个服务点、五个角色、13项权限、11菜单、四分类、三个参数及一个可编辑的保管位置目录，不创建业务或客户数据。

| 变量 | 用途 / 默认 |
| --- | --- |
| MYSQL_ROOT_PASSWORD | 必填，独立数据库root口令 |
| DATABASE_PASSWORD | 必填，独立应用数据库口令 |
| ADMIN_USERNAME / ADMIN_PASSWORD | 首次空库管理员；`admin`/必填随机口令，12–72字节且含大小写数字 |
| WEB_PORT / BIND_ADDRESS | `8128` / `127.0.0.1` |
| COOKIE_SECURE | 本机HTTP为`false`；HTTPS部署设置`true` |
| DATABASE_URL / DATABASE_USER | 外部MySQL可选；内置账号`lostdesk` |

配置名称见[.env.example](.env.example)。外部MySQL需最小权限、`sslMode=VERIFY_IDENTITY`及正确CA；备份脚本只支持内置数据库。端口冲突时用`WEB_PORT=18128 docker compose -p lostdesk up -d --build --wait`，不得停止其他项目。普通停机不要删除数据库卷。升级先备份、保留已应用迁移校验值，新增版本迁移。

## 测试与部署

```sh
mvn -B -f backend/pom.xml spotless:check test package
cd frontend
npm ci --no-audit --no-fund
npm run format:check
npm run lint
npm test
npm run build
cd ..
python3 scripts/release-check.py
git diff --check
```

后端37项真实MockMvc/JPA/Flyway集成测试及17项业务/输入/图片边界测试；前端11项请求、错误、双语与表单隔离测试。后端集成测试使用隔离H2 MySQL模式，完整部署另用全新MySQL8.4数据卷验收。Docker Maven构建执行测试，不跳过。

仅在专用本机空库运行以下流程，会创建TEST账号和业务记录，非空目录拒绝继续。私有口令和快照保存在已忽略的output/。

```sh
python3 -m venv .venv
.venv/bin/pip install -r scripts/requirements-quality.txt
.venv/bin/black --check scripts
.venv/bin/python scripts/quality.py --base http://127.0.0.1:8128
# Restart this dedicated test instance before persistence verification.
.venv/bin/python scripts/verify-persistence.py --base http://127.0.0.1:8128
.venv/bin/python scripts/backup.py --project lostdesk --output private-backups/lostdesk.zip
# Create private .env.restore with a different port, e.g. 28128.
.venv/bin/python scripts/restore.py private-backups/lostdesk.zip --project lostdesk-restore --env-file .env.restore
.venv/bin/python scripts/verify-persistence.py --base http://127.0.0.1:28128
```

流程验证实际报失→候选→本人特征→独立批准→交出→收讫，检查重复/并发认领、范围与字段泄露、照片读取/冻结、保管位置、CSV、历史以及重启和独立恢复的照片/文件哈希。领取到期和处置期限由受控时钟集成测试覆盖。

公网另需HTTPS、域名/证书、安全口令、账号控制、备份与监控，详见[部署与备份](docs/DEPLOYMENT.md)、[接口范围](docs/API.md)。本仓库的本机Compose验收不代表已部署公网域名或达到生产容量。

## 限制、故障与反馈

- 单实例学习版，每类实体查询上限10000条；部分列表/统计在内存计算，没有大规模场馆容量或多节点部署压测。
- 没有公开库存、匿名报失、自助注册、自动图像识别/相似匹配、后台到期清理、邮件/短信/微信通知、快递、支付、SSO或法定身份认证集成。基本流程不依赖付费第三方服务。
- 暂不处理交出后的争议调查、实物损坏赔偿或自动回库；现场异常按机构流程处理。记录不替代实际核验、实体保管和适用制度。
- 照片重新编码为PNG，不保留原始文件/元数据；照片与描述存数据库，需机构配置备份、访问与保留政策，不声明法规合规认证。
- 页面时间戳按浏览器时区显示；业务日期按服务点时区，领取截止按绝对时间。保管与领取期限是创建时快照。
- 启动失败检查Compose日志、数据库健康和配置字段；迁移错误应核对版本与备份，不跳过迁移校验。没有菜单时核对账号角色与启用菜单；403核对服务点和职责，409刷新后核对当前版本/状态，网络结果未知不重复提交。

反馈功能问题请使用仓库Issues，提供脱敏步骤、版本与错误码；贡献前先测试现有流程，不提交客户资料、真实照片、账号口令或API密钥。安全漏洞请通过下方邮箱私下联系，不在公开Issue发布敏感细节。第三方版权许可见[说明](docs/THIRD_PARTY.md)。软件按现状提供，许可免责声明以LICENSE为准。

## 授权与联系知华科技

本项目由知华科技（上海如静知华信息科技有限公司）提供公开源码学习版本，主要用于个人学习、技术研究与非商业交流。未经书面授权不得商用。本许可不是OSI标准开源许可证；第三方许可不被替换。[LICENSE](LICENSE)与本说明一致。

**商业授权或深度定制开发请联系知华科技。** 商业源码授权、私有化部署、台账迁移、账号体系适配、定制开发与系统集成咨询：[官网](https://www.zhuatech.cn/)，微信`zhuatech`、`zhuatech2`。安全问题私下邮件：[han@zhuatech.cn](mailto:han@zhuatech.cn)、[jack@zhuatech.cn](mailto:jack@zhuatech.cn)。

<table><tr><td align="center"><img src="docs/images/wechat-zhuatech.png" alt="微信 zhuatech" height="200"><br>微信 zhuatech</td><td align="center"><img src="docs/images/wechat-zhuatech2.png" alt="微信 zhuatech2" height="200"><br>微信 zhuatech2</td></tr></table>
