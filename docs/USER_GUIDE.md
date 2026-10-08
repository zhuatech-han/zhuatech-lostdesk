# LostDesk 办理手册 / User guide

知华科技（上海如静知华信息科技有限公司） · [官网](https://www.zhuatech.cn/) · 商业授权/定制微信 zhuatech、zhuatech2。

ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.) · [han@zhuatech.cn](mailto:han@zhuatech.cn), [jack@zhuatech.cn](mailto:jack@zhuatech.cn), [WhatsApp +86 17521234993](https://wa.me/8617521234993).

## 从空库开始 / First workflow

管理员先在“服务点与设置”维护服务点、时区、分类和参数，在“登录账号”建立保管员、核验员和失主账号。职责分离是业务门禁：提出人不能批准本人候选，核验人不能兼任交出人，失主本人确认收讫。管理员拥有全部权限仍不能绕过这些限制。默认保管位置是目录配置，需按实际地点维护。

Create separate custodian, verifier and claimant accounts. Administrators configure sites/timezones, categories/policies and actual storage locations. Proposers cannot review their own candidates; reviewers cannot hand over the same claim. Only the claimant confirms receipt. Full administrators are subject to these business checks too.

保管员登记实际收到的物品、稳定编号、拾获日期/地点、保管位置和隐藏特征。对失主可显示的名称应为一般描述。内部识别特征和保管照片不向失主开放。照片仅PNG/JPEG，原文件最大2MiB、每边2500像素且总计300万像素，转换后PNG不超过4MiB，每记录最多4张。

Register actual received property, custody code, found date/place, location and hidden marks. Use a general description as the claimant-visible title. Inventory photos and identifying marks remain staff-only. Photos accept valid PNG/JPEG up to 2 MiB, 2500 pixels per side and 3 million total pixels, normalized to PNG up to 4 MiB; four active photos per record.

## 报失与核验 / Lost reports and verification

失主登录提交本人报失和私有描述，可上传本人报失照片。工作人员在报失详情按分类、日期及服务点查询候选，也可从同点保管物品选择候选。候选筛选不是图像识别或所有权确认。提出时保留物品隐藏特征与报失描述快照。原物品识别资料和已进入认领的照片冻结；仍可转移位置并留痕。

Claimants submit private lost reports. Staff filter candidates by site/category/date or select a stored item manually. Filtering is not image recognition or ownership determination. Proposals snapshot item marks and report descriptions. Item identification and photos used in claims are frozen; audited location moves remain possible.

失主在“我的认领”补充能证明所有权的特征。独立核验员根据现场核验填写内部依据与对失主的结果说明；两者分开存储。批准仅预留物品，其他同物品或同报失有效候选被关闭。失主收到说明、状态和领取期限，不收到库存、隐藏特征、其他失主信息或内部核验笔记。

Claimants submit identifying evidence. A different verifier records private review notes and a separate claimant message after actual verification. Approval reserves the item and closes competing candidates for the same item/report. Claimants see their status/message/deadline, not inventory, hidden marks, other claimant data or internal review notes.

## 实物交接 / Physical handover

保管员在期限内按批准记录核对实际来领者，确认实物交出。此时状态为“已交出待收讫”，物品不可再分配或取消回库。失主本人登录确认已实际收讫，认领关闭，报失变为已找回。记录不是电子签名、法定所有权证明或自动身份认证。现场异常需按机构流程调查；系统不自动撤销已交出的实物记录。

The custodian verifies the actual collecting person and confirms physical handover within the deadline. The record then awaits claimant receipt and cannot be cancelled/restocked. Only the claimant confirms actual receipt, closing the claim/report. Records are not electronic signatures, legal ownership certificates or automated identity checks. Investigate physical exceptions under organizational procedures; delivered records are not automatically reversed.

## 到期和处置 / Expiry and retention disposal

默认新收存保管30天、批准领取72小时、每名失主最多10条未结报失。可补录历史实际收存日期，拾获不能晚于收存，建档后收存日期不改；截止取登记时策略快照；修改参数不回写既有期限。服务点时区用于日期，领取期限使用绝对时刻。领取截止等于当前时刻就不能交出。保管员点击“整理过期认领”释放到期保留；没有后台定时清理或通知，GET无状态副作用。

Defaults: 30 retention days, 72 pickup hours and ten open reports per claimant. Historic intake dates are supported; found dates cannot follow receipt and received dates are immutable. Retention uses the registration-time policy snapshot. Existing items/ready claims retain policy deadlines. Site timezone determines dates; pickup uses absolute time and is expired at the exact deadline. Custodians explicitly process expired claims. There is no scheduler or automatic notification; GET has no state mutation.

到期且无有效认领的物品可申请处置，另一位核验员批准后，保管员记录实际执行和凭据；批准人不能同时执行。实际执行前可撤销，恢复保管状态。到期只是软件申请门禁，保管期限与处理方式须由机构依据适用制度配置；系统不自动判定法定义务或销毁物品。

Retention-due items with no active claims can be proposed for disposal. Another verifier approves; a separate custodian records actual execution/evidence. Pending/approved disposal can be cancelled before execution. Due dates are workflow gates: organizations configure periods/methods under their applicable policies. The software does not determine legal obligations or automatically destroy property.
