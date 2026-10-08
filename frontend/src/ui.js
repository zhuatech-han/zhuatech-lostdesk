// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
import { ref } from "vue";
export const language = ref("zh");
/** 双语界面，不改写业务内容。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function t(zh, en) {
  return language.value === "zh" ? zh : en;
}
const states = {
  STORED: ["保管中", "Stored"],
  RESERVED: ["待领取", "Reserved"],
  HANDED_OVER: ["待本人收讫", "Awaiting receipt"],
  RETURNED: ["已归还失主", "Returned to owner"],
  DISPOSAL_PENDING: ["待处置复核", "Disposal review"],
  DISPOSAL_APPROVED: ["待处置执行", "Disposal approved"],
  DISPOSED: ["已处置", "Disposed"],
  VOID: ["已作废", "Voided"],
  OPEN: ["处理中", "Open"],
  MATCHING: ["认领办理中", "Claim in progress"],
  RESOLVED: ["已找回", "Resolved"],
  WITHDRAWN: ["已撤回", "Withdrawn"],
  PROPOSED: ["待补充特征", "Evidence needed"],
  AWAITING_REVIEW: ["待独立核验", "Awaiting review"],
  READY: ["可领取", "Ready for pickup"],
  DELIVERED: ["已交出待收讫", "Awaiting receipt"],
  CLOSED: ["已收讫", "Receipt confirmed"],
  REJECTED: ["未通过", "Rejected"],
  CANCELLED: ["已取消", "Cancelled"],
  EXPIRED: ["领取超期", "Pickup expired"],
  PENDING: ["待复核", "Pending review"],
  APPROVED: ["已批准", "Approved"],
  EXECUTED: ["已执行", "Executed"],
};
/** 当前服务端状态名称。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function stateName(code) {
  return states[code] ? t(...states[code]) : code;
}
const actions = {
  ITEM_RECEIVED: ["登记收存", "Item received"],
  ITEM_EDITED: ["更正收存资料", "Item details updated"],
  ITEM_MOVED: ["转移保管位置", "Storage location moved"],
  ITEM_VOID: ["作废登记", "Intake voided"],
  ITEM_HANDED_OVER: ["交出实物", "Physical handover"],
  REPORT_CREATED: ["提交报失", "Lost report submitted"],
  REPORT_EDITED: ["更新报失", "Lost report updated"],
  REPORT_WITHDRAWN: ["撤回报失", "Lost report withdrawn"],
  CLAIM_PROPOSED: ["提出认领候选", "Claim candidate proposed"],
  CLAIM_EVIDENCE: ["补充认领特征", "Ownership evidence submitted"],
  CLAIM_APPROVED: ["核验通过", "Claim approved"],
  CLAIM_REJECTED: ["核验未通过", "Claim rejected"],
  CLAIM_CANCELLED: ["取消认领", "Claim cancelled"],
  CLAIM_EXPIRED: ["领取超期释放", "Pickup expired and released"],
  CLAIM_RECEIVED: ["本人确认收讫", "Receipt confirmed by owner"],
  DISPOSAL_REQUESTED: ["申请到期处置", "Disposal requested"],
  DISPOSAL_APPROVED: ["批准处置", "Disposal approved"],
  DISPOSAL_REJECTED: ["处置复核未通过", "Disposal rejected"],
  DISPOSAL_EXECUTED: ["执行实物处置", "Physical disposal completed"],
  DISPOSAL_CANCELLED: ["取消处置", "Disposal cancelled"],
  PHOTO_UPLOAD: ["添加私有照片", "Private photo added"],
  PHOTO_REMOVED: ["移除私有照片", "Private photo removed"],
};
/** 将办理动作显示为当前语言，保留未知动作编号便于核对。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function actionName(code) {
  return actions[code] ? t(...actions[code]) : code;
}
const errors = {
  DATE_ORDER: [
    "拾获日期不能晚于实际收存日期。",
    "Found date cannot be later than the actual received date.",
  ],
  UNAUTHENTICATED: [
    "会话已失效，请重新登录。",
    "Session expired. Sign in again.",
  ],
  LOGIN_FAILED: [
    "账号或密码不正确，或账号已停用。",
    "Invalid credentials or disabled account.",
  ],
  LOGIN_THROTTLED: [
    "尝试过多，请五分钟后重试。",
    "Too many attempts. Retry in five minutes.",
  ],
  FORBIDDEN: ["没有此项操作权限。", "You do not have permission."],
  OUT_OF_SCOPE: [
    "此记录不在你的访问范围。",
    "This record is outside your scope.",
  ],
  VERSION_CONFLICT: [
    "记录已变更，请刷新核对后再操作。",
    "Record changed. Refresh and check before retrying.",
  ],
  RESULT_UNKNOWN: [
    "网络中断，提交结果未知。请刷新核对，勿重复提交。",
    "Connection interrupted; result unknown. Refresh and check before resubmitting.",
  ],
  NETWORK_ERROR: [
    "连接失败，请检查网络后刷新。",
    "Connection failed. Check your network and refresh.",
  ],
  INVALID_INPUT: [
    "字段格式不正确，请检查表单。",
    "Invalid fields. Check the form.",
  ],
  CODE_INVALID: [
    "编号需3–40位字母、数字及 _ . -。",
    "Code requires 3–40 letters, digits or _ . -.",
  ],
  CODE_DUPLICATE: [
    "此编号已使用，请核对现有记录。",
    "This code is already used. Check existing records.",
  ],
  ITEM_FROZEN: [
    "识别资料已进入认领记录，只能另行记录位置转移。",
    "Identification details are frozen by claim history. Use a location move instead.",
  ],
  ITEM_NOT_STORED: [
    "物品状态已变化，不能进行此操作。",
    "Item state changed. This action is unavailable.",
  ],
  LOCATION_INVALID: [
    "位置已停用或属于其他服务点。",
    "Location is disabled or belongs to another site.",
  ],
  LOCATION_BUSY: [
    "位置仍有保管物品，不能停用。",
    "Location still holds items and cannot be disabled.",
  ],
  RECORD_REFERENCED: [
    "已有业务历史，不能删除或作废。",
    "Business history prevents deletion or voiding.",
  ],
  CATEGORY_DISABLED: ["分类不存在或已停用。", "Category missing or disabled."],
  DATE_FUTURE: [
    "日期不能晚于服务点的今天。",
    "Date cannot be later than today at the service site.",
  ],
  REPORT_LIMIT: [
    "未结报失数量已达上限，请先整理现有记录。",
    "Open report limit reached. Review existing reports.",
  ],
  REPORT_BUSY: [
    "报失已进入认领流程，请先处理候选。",
    "Report is in a claim workflow. Handle existing candidates first.",
  ],
  REPORT_CLOSED: [
    "报失已关闭或正在领取，不能提出新候选。",
    "Report closed or reserved. No new candidates can be proposed.",
  ],
  PROPOSAL_DUPLICATE: [
    "此报失已有该物品的有效候选。",
    "An active candidate already exists for this report and item.",
  ],
  SITE_MISMATCH: [
    "物品和报失必须属于同一服务点。",
    "Item and report must belong to the same site.",
  ],
  SELF_REVIEW: [
    "需要另一位有权限的工作人员处理。",
    "A different authorized staff member must perform this action.",
  ],
  CLAIM_STATE: [
    "认领状态已变化，请刷新后核对。",
    "Claim state changed. Refresh and check.",
  ],
  EVIDENCE_REQUIRED: [
    "请填写足够明确的识别特征或核验记录（至少10字）。",
    "Provide specific identifying evidence or review notes (at least 10 characters).",
  ],
  CLAIMANT_DISABLED: [
    "失主账号停用或失去报失权限，不能交出。",
    "Claimant disabled or no longer permitted. Handover blocked.",
  ],
  PICKUP_EXPIRED: [
    "领取期限已过，请整理过期认领后重新办理。",
    "Pickup deadline passed. Process expiry before arranging a new claim.",
  ],
  PHYSICAL_CONFIRMATION: [
    "请确认实物已实际交接。",
    "Confirm that the physical handover occurred.",
  ],
  HANDOVER_COMPLETE: [
    "物品已交出，不能撤回报失或重新入库。",
    "Item handed over. Report cannot be withdrawn or restocked.",
  ],
  RETENTION_NOT_DUE: [
    "尚未达到该物品入库时确定的保管期限。",
    "Original retention period has not ended.",
  ],
  ACTIVE_CLAIMS: [
    "仍有有效认领，请先核实和处理。",
    "Active claims remain. Verify and handle them first.",
  ],
  DISPOSAL_STATE: [
    "处置状态已变化，不能重复执行。",
    "Disposal state changed. It cannot be repeated.",
  ],
  PHOTO_INVALID: [
    "只支持内容有效的PNG或JPEG照片。",
    "Only valid PNG or JPEG photos are accepted.",
  ],
  PHOTO_DIMENSIONS: [
    "照片边长最多2500像素，总像素最多300万。",
    "Photo limit: 2500 pixels per side and 3 million total pixels.",
  ],
  PHOTO_LIMIT: [
    "每条记录最多4张有效照片。",
    "At most four active photos per record.",
  ],
  FILE_TOO_LARGE: [
    "原照片上限2MiB，转换后上限4MiB。",
    "Original photo limit 2 MiB; normalized limit 4 MiB.",
  ],
  LAST_ADMIN: [
    "必须保留一个启用的完整管理员。",
    "Keep an enabled full administrator.",
  ],
  PASSWORD_WEAK: [
    "密码需12–72字节，含大小写字母及数字。",
    "Password requires 12–72 bytes with uppercase, lowercase and a digit.",
  ],
  OLD_PASSWORD_INVALID: ["原密码不正确。", "Current password is incorrect."],
  ACCOUNT_ASSIGNED: [
    "账号已有业务历史，不能更换服务点。",
    "Account has business history. Site cannot be changed.",
  ],
  DEPARTMENT_DISABLED: ["服务点已停用。", "Service site disabled."],
  IDENTITY_LOCKED: [
    "记录身份或编号不可更改。",
    "Record identity or code cannot be changed.",
  ],
  NOT_FOUND: ["记录不存在或已移除。", "Record not found."],
  CONFLICT: [
    "重复记录或数据仍被使用，请核对。",
    "Duplicate or referenced data. Check before saving.",
  ],
  RESOURCE_LIMIT: ["超过本实例的查询上限。", "Instance query limit exceeded."],
};
/** 失败影响和可理解操作提示。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function errorMessage(error) {
  return errors[error.message]
    ? t(...errors[error.message])
    : `${t("操作未完成", "Action failed")} (${error.message})`;
}
export const confirmation = ref(null);
/** 页面内确认，避免原生弹窗阻塞。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function ask(message) {
  if (confirmation.value) return Promise.resolve(false);
  return new Promise((resolve) => {
    confirmation.value = { message, resolve };
  });
}
/** 明确同意或取消后完成确认。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function answerConfirmation(ok) {
  const p = confirmation.value;
  confirmation.value = null;
  p?.resolve(ok);
}
/** 拷贝响应式API记录，表单不直接改列表。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function cloneRecord(row) {
  return JSON.parse(JSON.stringify(row));
}
/** 整理日期仅用于显示，不替代服务端服务点时区判定。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function dateTime(value) {
  if (!value) return "—";
  return new Intl.DateTimeFormat(language.value === "zh" ? "zh-CN" : "en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}
