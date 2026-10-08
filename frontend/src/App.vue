<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed, onMounted } from "vue";
import {
  Package,
  Search,
  Plus,
  ArrowLeft,
  X,
  Menu,
  LogOut,
  KeyRound,
  ExternalLink,
  ClipboardList,
  Shield,
  Users,
  Settings,
  History,
  ChartColumn,
  MapPin,
} from "@lucide/vue";
import { api, resetApi, download } from "./api.js";
import {
  language,
  t,
  stateName,
  actionName,
  errorMessage,
  cloneRecord,
  ask,
  confirmation,
  answerConfirmation,
  dateTime,
} from "./ui.js";
import AdminPanel from "./components/AdminPanel.vue";
const profile = ref(null),
  ready = ref(false),
  pending = ref(false),
  loading = ref(false),
  view = ref("my"),
  mobileNav = ref(false),
  about = ref(false),
  passwordForm = ref(null),
  notice = ref(null),
  loginForm = ref({ username: "", password: "" });
const opts = ref({ sites: [], categories: [], spots: [] }),
  list = ref({ items: [], total: 0 }),
  q = ref(""),
  state = ref(""),
  sort = ref("latest"),
  page = ref(1),
  selection = ref(null),
  myTab = ref("reports"),
  metrics = ref({}),
  editor = ref(null),
  editorInitial = ref(""),
  adminRef = ref(null),
  candidates = ref([]);
const has = (code) => profile.value?.permissions.includes(code),
  busy = computed(() => pending.value || loading.value),
  menus = computed(() => profile.value?.menus || []);
const names = {
  items: () => t("保管物品", "Stored items"),
  reportsdesk: () => t("报失处理", "Lost reports"),
  claims: () => t("认领核验", "Claims"),
  disposals: () => t("到期处置", "Disposals"),
  spots: () => t("保管位置", "Storage locations"),
  my: () => t("我的报失与认领", "My lost reports"),
  dashboard: () => t("统计台账", "Reports"),
  audit: () => t("操作记录", "Audit trail"),
};
const icons = {
  items: Package,
  reportsdesk: ClipboardList,
  claims: Shield,
  disposals: Package,
  spots: MapPin,
  my: ClipboardList,
  dashboard: ChartColumn,
  users: Users,
  roles: Shield,
  settings: Settings,
  audit: History,
};
const mode = computed(() =>
    view.value === "my"
      ? myTab.value === "reports"
        ? "reportsdesk"
        : "claims"
      : view.value,
  ),
  current = computed(
    () =>
      selection.value?.data?.item ||
      selection.value?.data?.report ||
      selection.value?.data?.claim ||
      selection.value?.data?.disposal,
  ),
  pages = computed(() => Math.max(1, Math.ceil(list.value.total / 15)));
const statuses = {
  items: [
    "STORED",
    "RESERVED",
    "HANDED_OVER",
    "RETURNED",
    "DISPOSAL_PENDING",
    "DISPOSAL_APPROVED",
    "DISPOSED",
    "VOID",
    "DUE",
  ],
  reportsdesk: ["OPEN", "MATCHING", "RESOLVED", "WITHDRAWN"],
  claims: [
    "PROPOSED",
    "AWAITING_REVIEW",
    "READY",
    "DELIVERED",
    "CLOSED",
    "REJECTED",
    "CANCELLED",
    "EXPIRED",
  ],
  disposals: ["PENDING", "APPROVED", "REJECTED", "EXECUTED", "CANCELLED"],
};
const metricsNames = {
  STORED: () => t("保管中", "Stored"),
  RESERVED: () => t("待领取", "Reserved"),
  HANDED_OVER: () => t("待本人收讫", "Awaiting receipt"),
  RETURNED: () => t("已找回", "Returned"),
  DISPOSED: () => t("已处置", "Disposed"),
  retentionDue: () => t("保管到期", "Retention due"),
  verificationDue: () => t("待核验", "Review due"),
  disposalDue: () => t("待处置复核", "Disposal review due"),
  openReports: () => t("我的未结报失", "My open reports"),
  ready: () => t("可以领取", "Ready for pickup"),
  receiptDue: () => t("待确认收讫", "Receipt due"),
  closed: () => t("已收讫", "Receipt confirmed"),
};
const siteName = (id) =>
    opts.value.sites.find((x) => x.id === id)?.name || "#" + id,
  spotName = (id) =>
    opts.value.spots.find((x) => x.id === id)?.name || "#" + id,
  categoryName = (code) => {
    const c = opts.value.categories.find((x) => x.code === code);
    return c ? (language.value === "zh" ? c.name : c.nameEn) : code;
  };
function fail(e) {
  notice.value = { type: "error", text: errorMessage(e) };
  if (e.status === 401) {
    profile.value = null;
    selection.value = null;
    editor.value = null;
    passwordForm.value = null;
    resetApi();
  }
}
/** 所有写入禁止自动重放，错误保留表单和未知结果提示。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function perform(path, method, body) {
  if (pending.value) return null;
  pending.value = true;
  notice.value = null;
  try {
    const v = await api(path, { method, body });
    notice.value = { type: "success", text: t("已保存。", "Saved.") };
    return v;
  } catch (e) {
    fail(e);
    return null;
  } finally {
    pending.value = false;
  }
}
async function read(fn) {
  loading.value = true;
  try {
    return await fn();
  } catch (e) {
    fail(e);
    return null;
  } finally {
    loading.value = false;
  }
}
async function initialize() {
  opts.value = await api("/options");
  view.value = menus.value[0]?.code || "none";
  await load();
}
onMounted(async () => {
  await read(async () => {
    try {
      profile.value = await api("/auth/me");
      await initialize();
    } catch (e) {
      if (e.status !== 401) throw e;
    }
  });
  ready.value = true;
});
async function signIn() {
  const p = await perform("/auth/login", "POST", loginForm.value);
  if (p) {
    profile.value = p;
    loginForm.value.password = "";
    notice.value = null;
    await read(initialize);
  }
}
async function canLeave() {
  if (busy.value) return false;
  if (
    editor.value &&
    JSON.stringify(editor.value.body) !== editorInitial.value &&
    !(await ask(t("放弃尚未保存的修改？", "Discard unsaved changes?")))
  )
    return false;
  if (adminRef.value?.canLeave && !(await adminRef.value.canLeave()))
    return false;
  editor.value = null;
  return true;
}
async function navigate(code) {
  if (!(await canLeave())) return;
  view.value = code;
  selection.value = null;
  candidates.value = [];
  q.value = "";
  state.value = "";
  sort.value = "latest";
  page.value = 1;
  mobileNav.value = false;
  notice.value = null;
  await read(load);
}
async function signOut() {
  if (!(await canLeave())) return;
  const v = await perform("/auth/logout", "POST", {});
  if (v) {
    profile.value = null;
    selection.value = null;
    resetApi();
    notice.value = null;
  }
}
/** 页面读取真实有界API；不在客户端拼造物品或认领记录。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function load() {
  if (["users", "roles", "settings", "none"].includes(view.value)) return;
  if (view.value === "dashboard") {
    metrics.value = await api("/dashboard");
    return;
  }
  if (view.value === "audit") {
    const rows = await api("/audit");
    const filtered = rows.filter((x) =>
      (x.actor + " " + x.action + " " + x.objectId)
        .toLowerCase()
        .includes(q.value.toLowerCase()),
    );
    list.value = {
      items: filtered.slice((page.value - 1) * 15, page.value * 15),
      total: filtered.length,
    };
    return;
  }
  if (view.value === "spots") {
    const rows = await api("/spots");
    const filtered = rows.filter((x) =>
      (x.code + " " + x.name).toLowerCase().includes(q.value.toLowerCase()),
    );
    list.value = {
      items: filtered.slice((page.value - 1) * 15, page.value * 15),
      total: filtered.length,
    };
    return;
  }
  if (view.value === "my") metrics.value = await api("/dashboard?mine=true");
  const endpoint = {
    items: "/items",
    reportsdesk: "/lost-reports",
    claims: "/claims",
    disposals: "/disposals",
  }[mode.value];
  if (endpoint)
    list.value = await api(
      endpoint +
        "?q=" +
        encodeURIComponent(q.value) +
        "&state=" +
        state.value +
        "&page=" +
        page.value +
        "&size=15&sort=" +
        sort.value +
        (view.value === "my" ? "&mine=true" : ""),
    );
}
async function search() {
  page.value = 1;
  await read(load);
}
async function changePage(n) {
  page.value = n;
  await read(load);
}
async function switchMy(tab) {
  if (!(await canLeave())) return;
  myTab.value = tab;
  selection.value = null;
  q.value = "";
  state.value = "";
  sort.value = "latest";
  page.value = 1;
  await read(load);
}
async function show(kind, row) {
  if (busy.value) return;
  notice.value = null;
  candidates.value = [];
  await read(async () => {
    selection.value = {
      kind,
      id: kind === "disposals" ? row.disposal.id : row.id,
      data:
        kind === "disposals"
          ? row
          : await api(
              "/" +
                {
                  items: "items",
                  reportsdesk: "lost-reports",
                  claims: "claims",
                }[kind] +
                "/" +
                row.id +
                (view.value === "my" ? "?mine=true" : ""),
            ),
    };
  });
}
async function back() {
  if (!(await canLeave())) return;
  selection.value = null;
  candidates.value = [];
  await read(load);
}
async function refresh() {
  await read(async () => {
    await load();
    opts.value = await api("/options");
    if (selection.value) {
      const s = selection.value;
      if (s.kind === "disposals") {
        selection.value = null;
      } else
        s.data = await api(
          "/" +
            { items: "items", reportsdesk: "lost-reports", claims: "claims" }[
              s.kind
            ] +
            "/" +
            s.id +
            (view.value === "my" ? "?mine=true" : ""),
        );
    }
  });
}
const field = (key, zh, en, type = "text", options = [], required = true) => ({
  key,
  zh,
  en,
  type,
  options,
  required,
  max: type === "textarea" ? 2000 : 200,
});
const fVersion = (row) => ({ version: row.version });
const siteOpts = () =>
    opts.value.sites.map((x) => ({ value: x.id, label: x.name })),
  catOpts = () =>
    opts.value.categories.map((x) => ({
      value: x.code,
      label: categoryName(x.code),
    }));
function start(title, path, method, body, fields, hint = "") {
  notice.value = null;
  editor.value = { title, path, method, body: cloneRecord(body), fields, hint };
  editorInitial.value = JSON.stringify(editor.value.body);
}
function intake(row = null) {
  const site = row?.departmentId || opts.value.sites[0]?.id;
  const b = row || {
    departmentId: site,
    storageId:
      opts.value.spots.find((x) => x.departmentId === site && x.enabled)?.id ||
      "",
    code: "",
    title: "",
    category: opts.value.categories[0]?.code || "",
    color: "",
    foundDate: new Date().toISOString().slice(0, 10),
    receivedDate: new Date().toISOString().slice(0, 10),
    foundPlace: "",
    privateMarks: "",
    note: "",
  };
  start(
    t(
      row ? "编辑收存资料" : "登记收存物品",
      row ? "Edit intake" : "Register found item",
    ),
    "/items" + (row ? "/" + row.id : ""),
    row ? "PUT" : "POST",
    b,
    [
      {
        ...field(
          "departmentId",
          "服务点",
          "Service site",
          "select",
          siteOpts(),
        ),
        locked: !!row,
      },
      field(
        "storageId",
        "保管位置",
        "Storage location",
        "select",
        opts.value.spots
          .filter((x) => x.enabled)
          .map((x) => ({
            value: x.id,
            label: siteName(x.departmentId) + " / " + x.name,
          })),
      ),
      { ...field("code", "保管编号", "Custody code"), locked: !!row, max: 40 },
      field(
        "title",
        "对失主可显示的物品名称",
        "Name that may be shown to claimant",
      ),
      field("category", "物品分类", "Category", "select", catOpts()),
      field("color", "颜色", "Color", "text", [], false),
      field("foundDate", "拾获日期", "Date found", "date"),
      {
        ...field(
          "receivedDate",
          "实际收存日期",
          "Actual received date",
          "date",
        ),
        locked: !!row,
      },
      field("foundPlace", "拾获地点", "Place found"),
      field(
        "privateMarks",
        "隐藏识别特征（仅工作人员）",
        "Hidden identifying marks (staff only)",
        "textarea",
      ),
      field("note", "保管备注", "Custody note", "textarea", [], false),
    ],
  );
}
function reportEditor(row = null) {
  start(
    t(
      row ? "编辑本人报失" : "提交报失",
      row ? "Edit my lost report" : "Report a lost item",
    ),
    "/lost-reports" + (row ? "/" + row.id : ""),
    row ? "PUT" : "POST",
    row || {
      title: "",
      category: opts.value.categories[0]?.code || "",
      color: "",
      lostDate: new Date().toISOString().slice(0, 10),
      lostPlace: "",
      description: "",
    },
    [
      field("title", "丢失物品名称", "Lost item name"),
      field("category", "物品分类", "Category", "select", catOpts()),
      field("color", "颜色", "Color", "text", [], false),
      field("lostDate", "丢失日期", "Date lost", "date"),
      field("lostPlace", "丢失地点", "Place lost"),
      field(
        "description",
        "能识别物品的私有特征",
        "Private identifying description",
        "textarea",
      ),
    ],
  );
}
async function propose(r, item = null) {
  await read(async () => {
    const rows = (await api("/items?state=STORED&size=100")).items.filter(
      (x) => x.departmentId === r.departmentId,
    );
    start(
      t("提出认领候选", "Propose a claim"),
      "/claims",
      "POST",
      { reportId: r.id, itemId: item?.id || "", message: "" },
      [
        field(
          "itemId",
          "候选物品",
          "Candidate item",
          "select",
          rows.map((x) => ({ value: x.id, label: x.code + " / " + x.title })),
        ),
        field("message", "发送给失主的说明", "Message to claimant", "textarea"),
      ],
      t(
        "请勿把隐藏特征填写在对失主的说明中。",
        "Keep hidden identifying marks out of the claimant message.",
      ),
    );
  });
}
async function findCandidates() {
  await read(async () => {
    candidates.value = await api(
      "/lost-reports/" + current.value.id + "/candidates",
    );
  });
}
function claimAction(action) {
  const c = current.value,
    base = "/claims/" + c.id + "/" + action,
    b = fVersion(c);
  if (action === "evidence")
    start(
      t("补充认领特征", "Provide identifying evidence"),
      base,
      "POST",
      { ...b, evidence: c.evidence || "" },
      [
        field(
          "evidence",
          "只有失主了解的特征或证明",
          "Distinctive identifying evidence",
          "textarea",
        ),
      ],
    );
  else if (action === "review")
    start(
      t("独立认领核验", "Independent claim review"),
      base,
      "POST",
      { ...b, approve: true, reviewNote: "", message: "" },
      [
        field("approve", "核验决定", "Review decision", "select", [
          { value: true, label: t("批准领取", "Approve pickup") },
          { value: false, label: t("拒绝认领", "Reject claim") },
        ]),
        field(
          "reviewNote",
          "内部核验依据（仅工作人员）",
          "Private verification notes",
          "textarea",
        ),
        field(
          "message",
          "发送给失主的结果说明",
          "Decision message to claimant",
          "textarea",
        ),
      ],
    );
  else if (action === "handover" || action === "receipt")
    start(
      t(
        action === "handover" ? "确认实物交出" : "本人确认收讫",
        action === "handover" ? "Confirm physical handover" : "Confirm receipt",
      ),
      base,
      "POST",
      { ...b, note: "", physicalConfirmed: false },
      [
        field(
          "note",
          action === "handover" ? "交出记录" : "收讫备注",
          action === "handover" ? "Handover record" : "Receipt note",
          "textarea",
          [],
          action === "handover",
        ),
        field(
          "physicalConfirmed",
          action === "handover"
            ? "物品已实际交给核验通过的失主"
            : "我已实际收到这件物品",
          action === "handover"
            ? "I physically handed the item to the verified claimant"
            : "I physically received this item",
          "checkbox",
        ),
      ],
    );
  else
    start(
      t("取消尚未交出的认领", "Cancel claim before handover"),
      base,
      "POST",
      { ...b, note: "" },
      [field("note", "取消原因", "Cancellation reason", "textarea")],
    );
}
function reportWithdraw() {
  const r = current.value;
  start(
    t("撤回本人报失", "Withdraw my lost report"),
    "/lost-reports/" + r.id + "/withdraw",
    "POST",
    { version: r.version, note: "" },
    [field("note", "撤回原因", "Withdrawal reason", "textarea")],
  );
}
function itemAction(action) {
  const x = current.value,
    b = fVersion(x);
  if (action === "move")
    start(
      t("转移保管位置", "Move storage location"),
      "/items/" + x.id + "/move",
      "POST",
      { ...b, storageId: x.storageId, note: "" },
      [
        field(
          "storageId",
          "新保管位置",
          "New storage location",
          "select",
          opts.value.spots
            .filter((s) => s.departmentId === x.departmentId && s.enabled)
            .map((s) => ({ value: s.id, label: s.name })),
        ),
        field("note", "转移记录", "Move record", "textarea"),
      ],
    );
  else if (action === "void")
    start(
      t("作废错误收存记录", "Void incorrect intake record"),
      "/items/" + x.id + "/void",
      "POST",
      { ...b, note: "" },
      [field("note", "作废原因", "Voiding reason", "textarea")],
    );
  else
    start(
      t("申请到期处置", "Request retention disposal"),
      "/disposals",
      "POST",
      { ...b, itemId: x.id, method: "RECYCLE", reason: "" },
      [
        field(
          "method",
          "处理方式",
          "Handling method",
          "select",
          ["DONATE", "RECYCLE", "DESTROY", "TRANSFER"].map((value) => ({
            value,
            label: disposalMethod(value),
          })),
        ),
        field(
          "reason",
          "机构依据与申请原因",
          "Organizational basis and reason",
          "textarea",
        ),
      ],
    );
}
const disposalMethod = (code) =>
  ({
    DONATE: t("捐赠", "Donate"),
    RECYCLE: t("回收", "Recycle"),
    DESTROY: t("销毁", "Destroy"),
    TRANSFER: t("移交指定单位", "Transfer to designated entity"),
  })[code] || code;
function disposalAction(action) {
  const d = current.value,
    b = fVersion(d),
    fields = [];
  if (action === "review") {
    b.approve = true;
    fields.push(
      field("approve", "复核决定", "Review decision", "select", [
        { value: true, label: t("批准", "Approve") },
        { value: false, label: t("拒绝", "Reject") },
      ]),
    );
  }
  if (action === "execute") {
    b.physicalConfirmed = false;
    fields.push(
      field(
        "physicalConfirmed",
        "物品已按批准方式实际处理",
        "The physical disposal was completed as approved",
        "checkbox",
      ),
    );
  }
  b.note = "";
  fields.push(
    field(
      "note",
      action === "execute" ? "实际处理记录与凭据" : "复核或撤销依据",
      action === "execute"
        ? "Execution record and receipt"
        : "Review or cancellation notes",
      "textarea",
    ),
  );
  start(
    t(
      action === "review"
        ? "独立处置复核"
        : action === "execute"
          ? "记录实际处置"
          : "撤销未执行处置",
      action === "review"
        ? "Independent disposal review"
        : action === "execute"
          ? "Record physical disposal"
          : "Cancel pending disposal",
    ),
    "/disposals/" + d.id + "/" + action,
    "POST",
    b,
    fields,
  );
}
function spotEditor(row = null) {
  start(
    t(
      row ? "编辑保管位置" : "新增保管位置",
      row ? "Edit storage location" : "Add storage location",
    ),
    "/spots" + (row ? "/" + row.id : ""),
    row ? "PUT" : "POST",
    row || {
      departmentId: opts.value.sites[0]?.id || "",
      code: "",
      name: "",
      enabled: true,
    },
    [
      {
        ...field(
          "departmentId",
          "服务点",
          "Service site",
          "select",
          siteOpts(),
        ),
        locked: !!row,
      },
      { ...field("code", "位置编号", "Location code"), locked: !!row, max: 40 },
      field("name", "位置名称", "Location name"),
      field("enabled", "启用", "Enabled", "checkbox", [], false),
    ],
  );
}
async function deleteSpot(row) {
  if (
    !(await ask(t("删除未使用的保管位置？", "Delete unused storage location?")))
  )
    return;
  const v = await perform(
    "/spots/" + row.id + "?version=" + row.version,
    "DELETE",
  );
  if (v) await refresh();
}
async function saveEditor() {
  const e = editor.value;
  if (!e) return;
  const v = await perform(e.path, e.method, e.body);
  if (v) {
    editor.value = null;
    await refresh();
  }
}
async function closeEditor() {
  if (pending.value) return;
  if (
    JSON.stringify(editor.value.body) !== editorInitial.value &&
    !(await ask(t("放弃尚未保存的修改？", "Discard unsaved changes?")))
  )
    return;
  editor.value = null;
  notice.value = null;
}
async function uploadPhoto(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  const data = new FormData();
  data.append("file", file);
  const s = selection.value,
    v = await perform(
      "/" +
        (s.kind === "items" ? "items" : "lost-reports") +
        "/" +
        s.id +
        "/photos",
      "POST",
      data,
    );
  if (v) {
    event.target.value = "";
    await refresh();
  }
}
async function removePhoto(p) {
  if (
    !(await ask(
      t(
        "移除尚未进入认领证据的照片？",
        "Remove photo before it becomes claim evidence?",
      ),
    ))
  )
    return;
  const v = await perform(
    "/photos/" + p.id + "?version=" + p.version,
    "DELETE",
  );
  if (v) await refresh();
}
async function expire() {
  const v = await perform("/claims/expire", "POST", {});
  if (v) {
    await refresh();
    notice.value = {
      type: "success",
      text: t("已整理到期认领：", "Expired claims processed: ") + v.expired,
    };
  }
}
async function exportCsv() {
  await read(() => download("/reports/export", "lostdesk-inventory.csv"));
}
async function changePassword() {
  const v = await perform("/auth/password", "POST", passwordForm.value);
  if (v) {
    passwordForm.value = null;
    profile.value = null;
    resetApi();
    notice.value = {
      type: "success",
      text: t("密码已修改，请重新登录。", "Password changed. Sign in again."),
    };
  }
}
</script>
<template>
  <div v-if="!ready" class="startup">{{ t("正在连接…", "Connecting…") }}</div>
  <div v-else-if="!profile" class="login-page">
    <header class="login-top">
      <a
        class="brand"
        href="https://www.zhuatech.cn/"
        target="_blank"
        rel="noopener"
        ><img src="/brand/logo.jpg" alt="知华科技" /><span
          >LostDesk<small>{{
            t("知华失物保管与认领", "ZhiHua lost property & claims")
          }}</small></span
        ></a
      ><button @click="language = language === 'zh' ? 'en' : 'zh'">
        {{ language === "zh" ? "English" : "中文" }}
      </button>
    </header>
    <main class="login-card">
      <div class="eyebrow">LOST PROPERTY / CUSTODY</div>
      <h1>{{ t("登录失物服务台", "Sign in to LostDesk") }}</h1>
      <p>
        {{
          t(
            "收存、报失、认领与实物交接。",
            "Found items, lost reports, claims and handover.",
          )
        }}
      </p>
      <form @submit.prevent="signIn">
        <label
          >{{ t("账号", "Username")
          }}<input
            v-model="loginForm.username"
            autocomplete="username"
            required
            maxlength="60" /></label
        ><label
          >{{ t("密码", "Password")
          }}<input
            v-model="loginForm.password"
            type="password"
            autocomplete="current-password"
            required
            maxlength="128"
        /></label>
        <p v-if="notice" class="form-error" role="alert">{{ notice.text }}</p>
        <button class="primary full" :disabled="busy">
          {{ busy ? t("登录中…", "Signing in…") : t("登录", "Sign in") }}
        </button>
      </form>
    </main>
    <footer class="login-footer">
      {{
        t(
          "知华科技（上海如静知华信息科技有限公司）",
          "ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)",
        )
      }}<br />{{
        t(
          "公开源码学习版 · 非商业使用",
          "Public source learning edition · Non-commercial use",
        )
      }}<button class="link" @click="about = true">
        {{ t("商业授权与联系", "Licensing & contact") }}
      </button>
    </footer>
  </div>
  <div v-else class="app-shell">
    <aside class="sidebar" :class="{ expanded: mobileNav }">
      <a
        class="brand"
        href="https://www.zhuatech.cn/"
        target="_blank"
        rel="noopener"
        ><img src="/brand/logo.jpg" alt="知华科技" /><span
          >LostDesk<small>{{
            t("知华失物保管与认领", "ZhiHua lost property & claims")
          }}</small></span
        ></a
      >
      <div class="nav-caption">
        {{ t("失物服务台", "LOST PROPERTY WORKSPACE") }}
      </div>
      <nav>
        <button
          v-for="m in menus"
          :key="m.code"
          :disabled="busy"
          :class="{ active: view === m.code }"
          @click="navigate(m.code)"
        >
          <component :is="icons[m.code] || ClipboardList" :size="18" />{{
            language === "zh" ? m.name : m.nameEn
          }}
        </button>
      </nav>
      <div class="sidebar-bottom">
        <button @click="about = true">
          <ExternalLink :size="16" />{{
            t("联系知华科技", "Contact ZhiHua")
          }}</button
        ><small>{{
          t("公开源码学习版 · 非商业使用", "Public source · Non-commercial")
        }}</small>
      </div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <button
          class="mobile-menu"
          :aria-label="t('打开菜单', 'Open menu')"
          @click="mobileNav = !mobileNav"
        >
          <Menu :size="19" /></button
        ><span
          >{{ siteName(profile.departmentId)
          }}<small>{{
            t(
              profile.scope === "ALL"
                ? "全部服务点范围"
                : profile.scope === "ASSIGNED"
                  ? "本人记录范围"
                  : "本服务点范围",
              profile.scope === "ALL"
                ? "All-site scope"
                : profile.scope === "ASSIGNED"
                  ? "Own-record scope"
                  : "This-site scope",
            )
          }}</small></span
        >
        <div class="actions">
          <button
            class="language-button"
            @click="language = language === 'zh' ? 'en' : 'zh'"
          >
            {{ language === "zh" ? "EN" : "中文" }}</button
          ><span class="account-name"
            >{{ profile.displayName }}<small>{{ profile.role }}</small></span
          ><button
            :aria-label="t('修改密码', 'Change password')"
            :disabled="busy"
            @click="passwordForm = { oldPassword: '', newPassword: '' }"
          >
            <KeyRound :size="17" /></button
          ><button
            :aria-label="t('退出登录', 'Sign out')"
            :disabled="busy"
            @click="signOut"
          >
            <LogOut :size="17" />
          </button>
        </div>
      </header>
      <main class="main-content">
        <div v-if="notice" class="notice" :class="notice.type" role="alert">
          {{ notice.text }}
        </div>
        <p v-if="loading" class="muted">{{ t("正在读取…", "Loading…") }}</p>
        <AdminPanel
          v-if="['users', 'roles', 'settings'].includes(view)"
          ref="adminRef"
          :section="view"
          :pending="busy"
          :perform="perform"
          :message="notice?.type === 'error' ? notice.text : ''"
          @error="fail"
        />
        <section v-else-if="view === 'none'">
          <h1>{{ t("暂无业务入口", "No business access") }}</h1>
          <p>
            {{
              t(
                "请联系服务点管理员分配角色。",
                "Contact your site administrator for a role.",
              )
            }}
          </p>
        </section>
        <section v-else-if="view === 'dashboard'">
          <header class="page-heading">
            <div>
              <div class="eyebrow">CUSTODY / REPORTS</div>
              <h1>{{ t("统计台账", "Reports") }}</h1>
              <p class="muted">
                {{
                  t(
                    "当前授权服务点的真实记录。",
                    "Actual records within your authorized sites.",
                  )
                }}
              </p>
            </div>
            <div class="actions">
              <button :disabled="busy" @click="refresh">
                {{ t("刷新", "Refresh") }}</button
              ><button :disabled="busy" @click="exportCsv">
                {{ t("导出保管台账 CSV", "Export inventory CSV") }}
              </button>
            </div>
          </header>
          <dl class="metrics">
            <div v-for="(value, key) in metrics" :key="key">
              <dt>{{ metricsNames[key]?.() || key }}</dt>
              <dd>{{ value }}</dd>
            </div>
          </dl>
        </section>
        <section v-else-if="selection && current">
          <button class="link back" :disabled="busy" @click="back">
            <ArrowLeft :size="17" />{{ t("返回列表", "Back to list") }}
          </button>
          <header class="page-heading">
            <div>
              <div class="eyebrow">
                {{
                  selection.kind === "items"
                    ? current.code
                    : selection.kind === "claims"
                      ? "CLAIM #" + current.id
                      : selection.kind === "disposals"
                        ? "DISPOSAL #" + current.id
                        : "LOST REPORT #" + current.id
                }}
              </div>
              <h1>
                {{
                  current.title || current.itemTitle || selection.data.itemTitle
                }}
              </h1>
              <span class="pill" :class="current.status">{{
                stateName(current.status)
              }}</span>
            </div>
            <button :disabled="busy" @click="refresh">
              {{ t("刷新", "Refresh") }}
            </button>
          </header>
          <template v-if="selection.kind === 'items'"
            ><div class="actions detail-actions">
              <button
                v-if="has('intake') && selection.data.canEdit"
                :disabled="busy"
                @click="intake(current)"
              >
                {{ t("编辑收存资料", "Edit intake") }}</button
              ><button
                v-if="
                  has('intake') &&
                  ['STORED', 'RESERVED'].includes(current.status)
                "
                :disabled="busy"
                @click="itemAction('move')"
              >
                {{ t("转移位置", "Move location") }}</button
              ><button
                v-if="has('intake') && selection.data.canEdit"
                :disabled="busy"
                @click="itemAction('void')"
              >
                {{ t("作废错误录入", "Void incorrect entry") }}</button
              ><button
                v-if="
                  has('intake') &&
                  current.status === 'STORED' &&
                  selection.data.retentionDue
                "
                :disabled="busy"
                @click="itemAction('dispose')"
              >
                {{ t("申请到期处置", "Request disposal") }}
              </button>
            </div>
            <div class="detail-grid">
              <section class="panel">
                <h2>{{ t("收存资料", "Intake details") }}</h2>
                <dl class="detail-list">
                  <dt>{{ t("分类 / 颜色", "Category / color") }}</dt>
                  <dd>
                    {{ categoryName(current.category) }} /
                    {{ current.color || "—" }}
                  </dd>
                  <dt>{{ t("拾获日期 / 地点", "Found date / place") }}</dt>
                  <dd>{{ current.foundDate }} / {{ current.foundPlace }}</dd>
                  <dt>{{ t("服务点 / 保管位置", "Site / location") }}</dt>
                  <dd>
                    {{ siteName(current.departmentId) }} /
                    {{ spotName(current.storageId) }}
                  </dd>
                  <dt>
                    {{ t("收存 / 保管截止", "Received / retention date") }}
                  </dt>
                  <dd>
                    {{ current.receivedDate }} / {{ current.retainUntil }}
                  </dd>
                </dl>
              </section>
              <section class="panel">
                <h2>{{ t("私有识别信息", "Private identification") }}</h2>
                <p class="preserve">{{ current.privateMarks }}</p>
                <p class="preserve muted">{{ current.note || "—" }}</p>
              </section>
            </div></template
          >
          <template v-if="selection.kind === 'reportsdesk'"
            ><div class="actions detail-actions">
              <button
                v-if="
                  has('request') &&
                  current.reporterId === profile.id &&
                  current.status === 'OPEN' &&
                  !selection.data.claims.some((x) =>
                    [
                      'PROPOSED',
                      'AWAITING_REVIEW',
                      'READY',
                      'DELIVERED',
                    ].includes(x.status),
                  )
                "
                :disabled="busy"
                @click="reportEditor(current)"
              >
                {{ t("编辑本人报失", "Edit my report") }}</button
              ><button
                v-if="
                  has('request') &&
                  current.reporterId === profile.id &&
                  ['OPEN', 'MATCHING'].includes(current.status)
                "
                :disabled="busy"
                @click="reportWithdraw"
              >
                {{ t("撤回报失", "Withdraw report") }}</button
              ><button
                v-if="
                  has('propose') && has('items') && current.status === 'OPEN'
                "
                :disabled="busy"
                @click="findCandidates"
              >
                {{ t("查找候选", "Find candidates") }}</button
              ><button
                v-if="
                  has('propose') && has('items') && current.status === 'OPEN'
                "
                :disabled="busy"
                @click="propose(current)"
              >
                {{ t("提出认领候选", "Propose claim") }}
              </button>
            </div>
            <section class="panel">
              <dl class="detail-list">
                <dt>{{ t("分类 / 颜色", "Category / color") }}</dt>
                <dd>
                  {{ categoryName(current.category) }} /
                  {{ current.color || "—" }}
                </dd>
                <dt>{{ t("丢失日期 / 地点", "Lost date / place") }}</dt>
                <dd>{{ current.lostDate }} / {{ current.lostPlace }}</dd>
                <dt>{{ t("私有识别描述", "Private description") }}</dt>
                <dd class="preserve">{{ current.description }}</dd>
              </dl>
            </section>
            <section v-if="candidates.length" class="panel">
              <h2>{{ t("人工候选列表", "Candidates for manual review") }}</h2>
              <div
                v-for="candidate in candidates"
                :key="candidate.id"
                class="candidate-row"
              >
                <span
                  ><strong>{{ candidate.title }}</strong
                  ><small
                    >{{ candidate.code }} · {{ candidate.foundDate }} ·
                    {{ candidate.foundPlace }}</small
                  ></span
                ><button :disabled="busy" @click="propose(current, candidate)">
                  {{ t("提出认领", "Propose claim") }}
                </button>
              </div>
            </section>
            <section v-if="selection.data.claims.length" class="panel">
              <h2>{{ t("相关认领", "Related claims") }}</h2>
              <div
                v-for="c in selection.data.claims"
                :key="c.id"
                class="candidate-row"
              >
                <span
                  >{{ c.itemTitle }}
                  <small>{{ stateName(c.status) }}</small></span
                ><button :disabled="busy" @click="show('claims', c)">
                  {{ t("查看认领", "View claim") }}
                </button>
              </div>
            </section></template
          >
          <template v-if="selection.kind === 'claims'"
            ><div class="actions detail-actions">
              <button
                v-if="
                  has('request') &&
                  ['PROPOSED', 'AWAITING_REVIEW'].includes(current.status)
                "
                :disabled="busy"
                @click="claimAction('evidence')"
              >
                {{ t("补充认领特征", "Provide evidence") }}</button
              ><button
                v-if="has('review') && current.status === 'AWAITING_REVIEW'"
                :disabled="
                  busy ||
                  current.proposedBy === profile.id ||
                  current.claimantId === profile.id
                "
                @click="claimAction('review')"
              >
                {{ t("独立核验", "Review claim") }}</button
              ><button
                v-if="has('handover') && current.status === 'READY'"
                :disabled="
                  busy ||
                  current.pickupExpired ||
                  current.reviewedBy === profile.id ||
                  current.claimantId === profile.id
                "
                @click="claimAction('handover')"
              >
                {{ t("确认实物交出", "Confirm handover") }}</button
              ><button
                v-if="has('request') && current.status === 'DELIVERED'"
                :disabled="busy"
                @click="claimAction('receipt')"
              >
                {{ t("本人确认收讫", "Confirm my receipt") }}</button
              ><button
                v-if="
                  (has('request') || has('propose')) &&
                  ['PROPOSED', 'AWAITING_REVIEW', 'READY'].includes(
                    current.status,
                  )
                "
                :disabled="busy"
                @click="claimAction('cancel')"
              >
                {{ t("取消认领", "Cancel claim") }}</button
              ><button
                v-if="has('items') && current.itemId"
                :disabled="busy"
                @click="show('items', { id: current.itemId })"
              >
                {{ t("查看保管物品", "View private item") }}
              </button>
            </div>
            <p v-if="current.pickupExpired" class="form-error">
              {{
                t(
                  "领取期限已过，请联系服务点重新安排。",
                  "Pickup deadline passed. Contact the service site.",
                )
              }}
            </p>
            <div class="detail-grid">
              <section class="panel">
                <h2>{{ t("认领办理", "Claim details") }}</h2>
                <p class="preserve">{{ current.message }}</p>
                <dl class="detail-list">
                  <dt>{{ t("创建时间", "Created") }}</dt>
                  <dd>{{ dateTime(current.createdAt) }}</dd>
                  <dt>{{ t("领取截止", "Pickup deadline") }}</dt>
                  <dd>{{ dateTime(current.pickupUntil) }}</dd>
                  <dt>{{ t("实际交出", "Physical handover") }}</dt>
                  <dd>{{ dateTime(current.handedAt) }}</dd>
                  <dt>{{ t("本人收讫", "Claimant receipt") }}</dt>
                  <dd>{{ dateTime(current.receivedAt) }}</dd>
                </dl>
              </section>
              <section class="panel">
                <h2>{{ t("报失与特征", "Report and evidence") }}</h2>
                <p class="preserve">{{ current.reportDescription }}</p>
                <h3>{{ t("本人补充", "Claimant evidence") }}</h3>
                <p class="preserve">
                  {{
                    current.evidence ||
                    t("尚未补充。", "No evidence submitted.")
                  }}
                </p>
                <p class="preserve muted">{{ current.receiptNote }}</p>
              </section>
            </div>
            <section
              v-if="has('lost_reports') && current.itemMarks"
              class="panel private-record"
            >
              <h2>{{ t("工作人员核验区", "Staff verification") }}</h2>
              <p>
                {{ t("失主", "Claimant") }}：{{ current.claimantName }} · #{{
                  current.claimantId
                }}
              </p>
              <dl class="detail-list">
                <dt>{{ t("提出时隐藏特征", "Hidden marks at proposal") }}</dt>
                <dd class="preserve">{{ current.itemMarks }}</dd>
                <dt>{{ t("内部核验记录", "Private review record") }}</dt>
                <dd class="preserve">{{ current.reviewNote || "—" }}</dd>
                <dt>{{ t("实际交出记录", "Handover record") }}</dt>
                <dd class="preserve">{{ current.handoverNote || "—" }}</dd>
              </dl>
            </section></template
          >
          <template v-if="selection.kind === 'disposals'"
            ><div class="actions detail-actions">
              <button
                v-if="has('dispose') && current.status === 'PENDING'"
                :disabled="busy || current.requestedBy === profile.id"
                @click="disposalAction('review')"
              >
                {{ t("独立处置复核", "Review disposal") }}</button
              ><button
                v-if="has('intake') && current.status === 'APPROVED'"
                :disabled="busy || current.reviewedBy === profile.id"
                @click="disposalAction('execute')"
              >
                {{ t("记录实际处置", "Record disposal execution") }}</button
              ><button
                v-if="
                  has('intake') &&
                  ['PENDING', 'APPROVED'].includes(current.status)
                "
                :disabled="busy"
                @click="disposalAction('cancel')"
              >
                {{ t("撤销未执行处置", "Cancel pending disposal") }}
              </button>
            </div>
            <section class="panel">
              <dl class="detail-list">
                <dt>{{ t("物品编号", "Custody code") }}</dt>
                <dd>{{ selection.data.itemCode }}</dd>
                <dt>{{ t("处理方式", "Method") }}</dt>
                <dd>{{ disposalMethod(current.method) }}</dd>
                <dt>{{ t("申请依据", "Request basis") }}</dt>
                <dd class="preserve">{{ current.reason }}</dd>
                <dt>{{ t("独立复核记录", "Independent review") }}</dt>
                <dd class="preserve">{{ current.reviewNote || "—" }}</dd>
                <dt>{{ t("实际执行记录", "Execution record") }}</dt>
                <dd class="preserve">{{ current.executionNote || "—" }}</dd>
              </dl>
            </section></template
          >
          <section v-if="selection.data.photos" class="panel">
            <header class="page-heading">
              <div>
                <h2>{{ t("私有照片", "Private photos") }}</h2>
                <p class="muted">
                  {{
                    t(
                      "PNG/JPEG，单张2MiB以内；最多4张。",
                      "PNG/JPEG, up to 2 MiB each; at most four.",
                    )
                  }}
                </p>
              </div>
              <label
                v-if="
                  (selection.kind === 'items' &&
                    has('intake') &&
                    selection.data.canEdit) ||
                  (selection.kind === 'reportsdesk' &&
                    has('request') &&
                    current.reporterId === profile.id &&
                    current.status === 'OPEN' &&
                    selection.data.claims.length === 0)
                "
                class="photo-input"
                >{{ t("上传照片", "Upload photo")
                }}<input
                  type="file"
                  accept="image/png,image/jpeg"
                  :disabled="busy"
                  @change="uploadPhoto"
              /></label>
            </header>
            <div class="photo-grid">
              <figure v-for="p in selection.data.photos" :key="p.id">
                <img
                  :src="'/api/photos/' + p.id"
                  :alt="t('私有照片 #', 'Private photo #') + p.id"
                />
                <figcaption>
                  PHOTO-{{ p.id }}
                  <button
                    v-if="
                      (selection.kind === 'items' &&
                        has('intake') &&
                        selection.data.canEdit) ||
                      (selection.kind === 'reportsdesk' &&
                        has('request') &&
                        current.reporterId === profile.id &&
                        selection.data.claims.length === 0)
                    "
                    :disabled="busy"
                    @click="removePhoto(p)"
                  >
                    {{ t("移除", "Remove") }}
                  </button>
                </figcaption>
              </figure>
              <p v-if="!selection.data.photos.length" class="muted">
                {{ t("暂无照片。", "No photos.") }}
              </p>
            </div>
          </section>
          <section v-if="selection.data.history?.length" class="panel">
            <h2>{{ t("办理历史", "Activity history") }}</h2>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>{{ t("时间", "Time") }}</th>
                    <th>{{ t("动作", "Action") }}</th>
                    <th>{{ t("记录", "Record") }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="e in selection.data.history" :key="e.id">
                    <td>{{ dateTime(e.createdAt) }}</td>
                    <td>{{ actionName(e.action) }}</td>
                    <td class="preserve">{{ e.note || "—" }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </section>
        <section v-else>
          <header class="page-heading">
            <div>
              <div class="eyebrow">LOSTDESK / {{ view.toUpperCase() }}</div>
              <h1>{{ names[view]?.() || view }}</h1>
              <p class="muted">
                {{
                  view === "my"
                    ? t(
                        "只展示本人报失、认领和交接记录。",
                        "Only your lost reports, claims and receipts.",
                      )
                    : view === "items"
                      ? t(
                          "保管库存与识别资料仅向授权工作人员开放。",
                          "Inventory and identifying details are restricted to authorized staff.",
                        )
                      : view === "claims"
                        ? t(
                            "核验通过后再实物交接，由失主确认收讫。",
                            "Verify ownership before handover; the claimant confirms receipt.",
                          )
                        : view === "disposals"
                          ? t(
                              "到期申请、独立复核与实物执行分别留存。",
                              "Retention request, independent review and physical execution are recorded separately.",
                            )
                          : t(
                              "当前服务点权限范围内的记录。",
                              "Records within your authorized service sites.",
                            )
                }}
              </p>
            </div>
            <div class="actions">
              <button :disabled="busy" @click="refresh">
                {{ t("刷新", "Refresh") }}</button
              ><button
                v-if="view === 'items' && has('intake')"
                class="primary"
                :disabled="busy"
                @click="intake()"
              >
                <Plus :size="16" />{{
                  t("登记收存物品", "Register found item")
                }}</button
              ><button
                v-if="view === 'spots'"
                class="primary"
                :disabled="busy"
                @click="spotEditor()"
              >
                <Plus :size="16" />{{
                  t("新增保管位置", "Add storage location")
                }}</button
              ><button
                v-if="view === 'my' && has('request')"
                class="primary"
                :disabled="busy"
                @click="reportEditor()"
              >
                <Plus :size="16" />{{
                  t("提交报失", "Report lost item")
                }}</button
              ><button
                v-if="view === 'claims' && has('handover')"
                :disabled="busy"
                @click="expire"
              >
                {{ t("整理过期认领", "Process expired claims") }}
              </button>
            </div>
          </header>
          <template v-if="view === 'my'"
            ><dl class="metrics compact-metrics">
              <div v-for="(value, key) in metrics" :key="key">
                <dt>{{ metricsNames[key]?.() || key }}</dt>
                <dd>{{ value }}</dd>
              </div>
            </dl>
            <nav class="sub-tabs">
              <button
                :disabled="busy"
                :class="{ active: myTab === 'reports' }"
                @click="switchMy('reports')"
              >
                {{ t("我的报失", "My lost reports") }}</button
              ><button
                :disabled="busy"
                :class="{ active: myTab === 'claims' }"
                @click="switchMy('claims')"
              >
                {{ t("我的认领", "My claims") }}
              </button>
            </nav></template
          >
          <form class="toolbar" @submit.prevent="search">
            <div class="search-box">
              <Search :size="17" /><input
                v-model="q"
                :aria-label="t('搜索记录', 'Search records')"
                :placeholder="t('名称、编号或地点', 'Name, code or place')"
                maxlength="200"
              />
            </div>
            <select
              v-if="statuses[mode]"
              v-model="state"
              :aria-label="t('记录状态', 'Record state')"
            >
              <option value="">{{ t("全部状态", "All states") }}</option>
              <option v-for="s in statuses[mode]" :key="s" :value="s">
                {{
                  s === "DUE" ? t("保管到期", "Retention due") : stateName(s)
                }}
              </option></select
            ><select
              v-if="['items', 'claims', 'reportsdesk'].includes(mode)"
              v-model="sort"
              :aria-label="t('记录排序', 'Sort records')"
            >
              <option value="latest">{{ t("最新记录", "Latest") }}</option>
              <option
                :value="
                  mode === 'items'
                    ? 'due'
                    : mode === 'claims'
                      ? 'pickup'
                      : 'date'
                "
              >
                {{ t("日期顺序", "Date order") }}
              </option></select
            ><button :disabled="busy">{{ t("搜索", "Search") }}</button
            ><span class="count"
              >{{ list.total }} {{ t("条", "records") }}</span
            >
          </form>
          <div class="table-wrap">
            <table v-if="mode === 'items'">
              <thead>
                <tr>
                  <th>{{ t("物品 / 编号", "Item / code") }}</th>
                  <th>{{ t("分类 / 颜色", "Category / color") }}</th>
                  <th>{{ t("拾获 / 保管截止", "Found / retention") }}</th>
                  <th>{{ t("位置", "Location") }}</th>
                  <th>{{ t("状态", "State") }}</th>
                  <th>{{ t("操作", "Actions") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="x in list.items" :key="x.id">
                  <td>
                    <strong>{{ x.title }}</strong
                    ><small class="mono">{{ x.code }}</small>
                  </td>
                  <td>
                    {{ categoryName(x.category)
                    }}<small>{{ x.color || "—" }}</small>
                  </td>
                  <td>
                    {{ x.foundDate }}<small>{{ x.retainUntil }}</small>
                  </td>
                  <td>
                    {{ spotName(x.storageId)
                    }}<small>{{ siteName(x.departmentId) }}</small>
                  </td>
                  <td>
                    <span class="pill" :class="x.status">{{
                      stateName(x.status)
                    }}</span>
                  </td>
                  <td>
                    <button :disabled="busy" @click="show('items', x)">
                      {{ t("查看物品", "View item") }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
            <table v-else-if="mode === 'reportsdesk'">
              <thead>
                <tr>
                  <th>{{ t("丢失物品", "Lost item") }}</th>
                  <th>{{ t("丢失日期 / 地点", "Lost date / place") }}</th>
                  <th>{{ t("状态", "State") }}</th>
                  <th>{{ t("操作", "Actions") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in list.items" :key="r.id">
                  <td>
                    <strong>{{ r.title }}</strong
                    ><small
                      >{{ categoryName(r.category) }} / {{ r.color || "—" }} ·
                      #{{ r.id }}</small
                    >
                  </td>
                  <td>
                    {{ r.lostDate }}<small>{{ r.lostPlace }}</small>
                  </td>
                  <td>
                    <span class="pill" :class="r.status">{{
                      stateName(r.status)
                    }}</span>
                  </td>
                  <td>
                    <button :disabled="busy" @click="show('reportsdesk', r)">
                      {{ t("查看报失", "View report") }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
            <table v-else-if="mode === 'claims'">
              <thead>
                <tr>
                  <th>{{ t("认领物品", "Claim item") }}</th>
                  <th>{{ t("状态", "State") }}</th>
                  <th>
                    {{ t("创建 / 领取截止", "Created / pickup deadline") }}
                  </th>
                  <th>{{ t("操作", "Actions") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in list.items" :key="c.id">
                  <td>
                    <strong>{{ c.itemTitle }}</strong
                    ><small>#{{ c.id }} · {{ c.reportTitle }}</small>
                  </td>
                  <td>
                    <span class="pill" :class="c.status">{{
                      stateName(c.status)
                    }}</span
                    ><small v-if="c.pickupExpired">{{
                      t("领取期限已过", "Pickup expired")
                    }}</small>
                  </td>
                  <td>
                    {{ dateTime(c.createdAt)
                    }}<small>{{ dateTime(c.pickupUntil) }}</small>
                  </td>
                  <td>
                    <button :disabled="busy" @click="show('claims', c)">
                      {{ t("查看认领", "View claim") }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
            <table v-else-if="mode === 'disposals'">
              <thead>
                <tr>
                  <th>{{ t("物品", "Item") }}</th>
                  <th>{{ t("方式", "Method") }}</th>
                  <th>{{ t("状态", "State") }}</th>
                  <th>{{ t("申请时间", "Requested") }}</th>
                  <th>{{ t("操作", "Actions") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in list.items" :key="row.disposal.id">
                  <td>
                    {{ row.itemTitle }}<small>{{ row.itemCode }}</small>
                  </td>
                  <td>{{ disposalMethod(row.disposal.method) }}</td>
                  <td>
                    <span class="pill" :class="row.disposal.status">{{
                      stateName(row.disposal.status)
                    }}</span>
                  </td>
                  <td>{{ dateTime(row.disposal.createdAt) }}</td>
                  <td>
                    <button :disabled="busy" @click="show('disposals', row)">
                      {{ t("查看处置", "View disposal") }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
            <table v-else-if="view === 'spots'">
              <thead>
                <tr>
                  <th>{{ t("编号", "Code") }}</th>
                  <th>{{ t("保管位置", "Storage location") }}</th>
                  <th>{{ t("服务点", "Service site") }}</th>
                  <th>{{ t("状态", "State") }}</th>
                  <th>{{ t("操作", "Actions") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in list.items" :key="row.id">
                  <td class="mono">{{ row.code }}</td>
                  <td>{{ row.name }}</td>
                  <td>{{ siteName(row.departmentId) }}</td>
                  <td>
                    {{
                      row.enabled ? t("启用", "Enabled") : t("停用", "Disabled")
                    }}
                  </td>
                  <td class="actions">
                    <button :disabled="busy" @click="spotEditor(row)">
                      {{ t("编辑", "Edit") }}</button
                    ><button :disabled="busy" @click="deleteSpot(row)">
                      {{ t("删除未使用位置", "Delete unused location") }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
            <table v-else-if="view === 'audit'">
              <thead>
                <tr>
                  <th>{{ t("时间", "Time") }}</th>
                  <th>{{ t("账号", "Account") }}</th>
                  <th>{{ t("操作", "Action") }}</th>
                  <th>{{ t("对象", "Object") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in list.items" :key="row.id">
                  <td>{{ dateTime(row.createdAt) }}</td>
                  <td>{{ row.actor }}</td>
                  <td>{{ row.action }}</td>
                  <td>{{ row.objectId }}</td>
                </tr>
              </tbody>
            </table>
            <p v-if="!list.items.length && !loading" class="empty-row">
              {{ t("暂无符合条件的记录。", "No matching records.") }}
            </p>
          </div>
          <div class="pager">
            <span>{{ page }} / {{ pages }}</span
            ><button
              :disabled="busy || page <= 1"
              @click="changePage(page - 1)"
            >
              {{ t("上一页", "Previous") }}</button
            ><button
              :disabled="busy || page >= pages"
              @click="changePage(page + 1)"
            >
              {{ t("下一页", "Next") }}
            </button>
          </div>
        </section>
      </main>
    </div>
  </div>
  <div v-if="editor" class="modal-backdrop">
    <section
      class="modal"
      role="dialog"
      aria-modal="true"
      :aria-label="editor.title"
    >
      <header>
        <h2>{{ editor.title }}</h2>
        <button
          :disabled="pending"
          :aria-label="t('关闭表单', 'Close form')"
          @click="closeEditor"
        >
          <X :size="20" />
        </button>
      </header>
      <p v-if="editor.hint" class="muted">{{ editor.hint }}</p>
      <form @submit.prevent="saveEditor">
        <div class="form-grid">
          <label
            v-for="f in editor.fields"
            :key="f.key"
            :class="{
              wide: f.type === 'textarea',
              check: f.type === 'checkbox',
            }"
            >{{ language === "zh" ? f.zh : f.en
            }}<textarea
              v-if="f.type === 'textarea'"
              v-model="editor.body[f.key]"
              :required="f.required"
              :maxlength="f.max"
              rows="4"
              :disabled="pending" /><select
              v-else-if="f.type === 'select'"
              v-model="editor.body[f.key]"
              :required="f.required"
              :disabled="pending || f.locked"
            >
              <option disabled value="">{{ t("请选择", "Select") }}</option>
              <option
                v-for="o in f.options"
                :key="String(o.value)"
                :value="o.value"
              >
                {{ o.label }}
              </option></select
            ><input
              v-else-if="f.type === 'checkbox'"
              v-model="editor.body[f.key]"
              type="checkbox"
              :required="f.required"
              :disabled="pending" /><input
              v-else
              v-model="editor.body[f.key]"
              :type="f.type"
              :required="f.required"
              :maxlength="f.max"
              :disabled="pending || f.locked"
          /></label>
        </div>
        <p v-if="notice?.type === 'error'" class="form-error" role="alert">
          {{ notice.text }}
        </p>
        <div class="modal-actions">
          <button type="button" :disabled="pending" @click="closeEditor">
            {{ t("取消", "Cancel") }}</button
          ><button class="primary" :disabled="pending">
            {{ pending ? t("保存中…", "Saving…") : t("保存", "Save") }}
          </button>
        </div>
      </form>
    </section>
  </div>
  <div v-if="passwordForm" class="modal-backdrop">
    <section
      class="modal compact"
      role="dialog"
      aria-modal="true"
      :aria-label="t('修改密码', 'Change password')"
    >
      <header>
        <h2>{{ t("修改密码", "Change password") }}</h2>
        <button
          :disabled="pending"
          :aria-label="t('关闭密码表单', 'Close password form')"
          @click="passwordForm = null"
        >
          <X :size="20" />
        </button>
      </header>
      <form @submit.prevent="changePassword">
        <label
          >{{ t("原密码", "Current password")
          }}<input
            v-model="passwordForm.oldPassword"
            type="password"
            autocomplete="current-password"
            required /></label
        ><label
          >{{ t("新密码", "New password")
          }}<input
            v-model="passwordForm.newPassword"
            type="password"
            autocomplete="new-password"
            required
            minlength="12"
            maxlength="72" /></label
        ><small>{{
          t(
            "至少12字节，含大小写字母和数字；保存后重新登录。",
            "At least 12 bytes with uppercase, lowercase and a digit. Sign in again after saving.",
          )
        }}</small>
        <p v-if="notice?.type === 'error'" class="form-error">
          {{ notice.text }}
        </p>
        <button class="primary full" :disabled="pending">
          {{ t("保存新密码", "Save password") }}
        </button>
      </form>
    </section>
  </div>
  <div v-if="about" class="modal-backdrop">
    <section
      class="modal contact-modal"
      role="dialog"
      aria-modal="true"
      :aria-label="t('联系知华科技', 'Contact ZhiHua')"
    >
      <header>
        <h2>{{ t("联系知华科技", "Contact ZhiHua") }}</h2>
        <button
          :aria-label="t('关闭联系信息', 'Close contact information')"
          @click="about = false"
        >
          <X :size="20" />
        </button>
      </header>
      <img class="contact-logo" src="/brand/logo.jpg" alt="知华科技" />
      <p>
        {{
          t(
            "知华科技（上海如静知华信息科技有限公司）",
            "ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)",
          )
        }}
      </p>
      <p>
        {{
          t(
            "商业授权或深度定制开发请联系知华科技。",
            "Contact ZhiHua for commercial licensing, customization, deployment and integration.",
          )
        }}
      </p>
      <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
        >https://www.zhuatech.cn/</a
      >
      <div v-if="language === 'zh'" class="qr-grid">
        <figure>
          <img src="/brand/wechat-zhuatech.png" alt="微信 zhuatech" />
          <figcaption>微信 zhuatech</figcaption>
        </figure>
        <figure>
          <img src="/brand/wechat-zhuatech2.png" alt="微信 zhuatech2" />
          <figcaption>微信 zhuatech2</figcaption>
        </figure>
      </div>
      <div v-else class="contact-links">
        <a href="mailto:han@zhuatech.cn">han@zhuatech.cn</a
        ><a href="mailto:jack@zhuatech.cn">jack@zhuatech.cn</a
        ><a href="https://wa.me/8617521234993" target="_blank" rel="noopener"
          >WhatsApp +86 17521234993</a
        >
      </div>
      <p class="muted">
        {{
          t(
            "仅限个人学习交流；未经上海如静知华信息科技有限公司授权不得商用。",
            "Personal learning and exchange only. Commercial use requires authorization from Shanghai Rujing Zhihua Information Technology Co., Ltd.",
          )
        }}
      </p>
    </section>
  </div>
  <div v-if="confirmation" class="modal-backdrop confirmation-layer">
    <section
      class="modal compact"
      role="alertdialog"
      aria-modal="true"
      :aria-label="t('确认操作', 'Confirm action')"
    >
      <h2>{{ t("确认操作", "Confirm action") }}</h2>
      <p>{{ confirmation.message }}</p>
      <div class="modal-actions">
        <button @click="answerConfirmation(false)">
          {{ t("取消", "Cancel") }}</button
        ><button class="primary" @click="answerConfirmation(true)">
          {{ t("确认", "Confirm") }}
        </button>
      </div>
    </section>
  </div>
</template>
