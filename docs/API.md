# LostDesk 接口与权限 / API and scope

知华科技（上海如静知华信息科技有限公司） · [官网](https://www.zhuatech.cn/) · 商业授权/定制微信 zhuatech、zhuatech2。

ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.) · [han@zhuatech.cn](mailto:han@zhuatech.cn), [jack@zhuatech.cn](mailto:jack@zhuatech.cn), [WhatsApp +86 17521234993](https://wa.me/8617521234993).

同源/api，写入使用会话与CSRF请求头；修改携带当前version。图片返回no-store。错误code：400格式/日期/照片，401会话，403权限/范围，404不存在，409状态/版本/重复，413资源上限。客户端不自动重放写入。

Same-origin /api, session + CSRF for writes, current version for changes. Photos use no-store. Errors: 400 input/date/photo, 401 session, 403 permission/scope, 404 absent, 409 state/version/duplicate, 413 resource limit. Writes are not automatically replayed.

| Endpoint | Permissions / behavior |
| --- | --- |
| GET /options | Authenticated minimal sites/categories; storage options only authorized staff |
| GET /items, /items/{id} | items, non-ASSIGNED staff, site scope; private inventory |
| POST /items, PUT /items/{id} | intake, site; identifiers immutable; identification frozen after claim/disposal history |
| POST /items/{id}/move, /void | intake + version/state; audited location move or unused-entry void |
| GET/POST/PUT/DELETE /spots | intake; site, version; history/occupied-location protection |
| GET /lost-reports, /lost-reports/{id} | own request or scoped lost_reports staff |
| POST/PUT /lost-reports | request; actual current owner/site; no forged author |
| POST /lost-reports/{id}/withdraw | actual owner + request; blocks after delivery |
| GET /lost-reports/{id}/candidates | propose staff; same-site/category/date manual candidates |
| GET /claims, /claims/{id} | own request or scoped lost_reports staff; separate privacy projection |
| POST /claims | propose; snapshot evidence, same-site item/report |
| POST /claims/{id}/evidence | actual owner + request; not staff-submitted evidence |
| POST /claims/{id}/review | review; not proposer/claimant; private note/public message |
| POST /claims/{id}/handover | handover; not reviewer/claimant; live deadline + physical confirmation |
| POST /claims/{id}/receipt | actual owner + request; delivered state + physical confirmation |
| POST /claims/{id}/cancel | owner or propose staff; before delivery only |
| POST /claims/expire | handover staff; explicit expiry processing |
| GET /disposals | items staff, site scope |
| POST /disposals | intake; retention due, no active claim, item version |
| POST /disposals/{id}/review | dispose; not requester; pending state/version |
| POST /disposals/{id}/execute | intake; not reviewer; physical confirmation |
| POST /disposals/{id}/cancel | intake; before execution |
| POST /items/{id}/photos | intake; unfrozen item; multipart file |
| POST /lost-reports/{id}/photos | actual report owner + request; before claim history |
| GET/DELETE /photos/{id} | parent object scope; removal only before evidence freeze |
| GET /dashboard | reports staff or request owner; actual authorized metrics |
| GET /reports/export | reports staff; CSV without identifying evidence |
| GET /audit | audit; site scope; ASSIGNED only own actor |
| GET /admin/options, /admin/{kind}, POST/PUT /admin/{kind} | users/roles/settings plus ALL scope |

本人入口对 /lost-reports、/claims 的列表/详情和 /dashboard 使用 mine=true；即使拥有ALL工作人员权限也只显示本人记录和最小认领字段。

Own-record views use mine=true on lost-reports/claims lists/details and dashboard. Even ALL staff roles receive only their own records and minimal claim projection.

管理员目录kind固定为users、roles、permissions、departments、dictionaries、settings、menus。账号、角色、服务点检查version；系统参数/菜单代码不变。只支持CATEGORY字典；各类查询限制10000条。角色ALL仍须持有对应权限；ASSIGNED不接受工作人员私有库存接口。密码重置或账号/服务点停用会撤销旧会话。没有公共失物库存、匿名报失、自助注册、OIDC、第三方识别、快递或支付接口。

Admin kinds are fixed; identities/roles/sites use versions and system codes stay immutable. CATEGORY only; queries bounded at 10,000 rows per entity. ALL scope does not bypass permissions; ASSIGNED cannot call private staff inventory. Reset/disable revokes sessions. No public inventory, anonymous reports/self-signup, OIDC, recognition, courier or payment integration.
