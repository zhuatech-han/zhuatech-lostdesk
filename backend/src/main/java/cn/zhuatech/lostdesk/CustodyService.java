// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 失物收存、报失、认领独立核验与双边交接；写入经同一数据库锁串行保护。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Service
@Transactional
public class CustodyService {
  final Store db;
  final AccessService access;
  final AdminService admin;
  final Clock clock;

  public CustodyService(Store d, AccessService a, AdminService m, Clock c) {
    db = d;
    access = a;
    admin = m;
    clock = c;
  }

  LocalDate today(Long site) {
    return LocalDate.ofInstant(clock.instant(), ZoneId.of(db.get(Department.class, site).zone));
  }

  void version(long v, Map<String, Object> b) {
    Rules.check(v == Rules.id(b.get("version")), "VERSION_CONFLICT");
  }

  void site(Long site, String perm) {
    access.staff(perm);
    access.library(site, perm);
  }

  void ownReport(LostReport r) {
    if (!staffReports()) access.require("request");
    if (!access.report(r)) throw new Problem(403, "OUT_OF_SCOPE");
  }

  void requester(LostReport r) {
    access.require("request");
    if (!Objects.equals(access.current().id, r.reporterId)) throw new Problem(403, "OUT_OF_SCOPE");
    access.library(r.departmentId, "request");
  }

  void claimRead(ClaimCase c) {
    ownReport(db.get(LostReport.class, c.reportId));
  }

  boolean staffReports() {
    return access.has("lost_reports") && !access.role().scope.equals("ASSIGNED");
  }

  void event(
      String action, Long site, Long item, Long report, Long claim, Long disposal, String note) {
    var e = new CustodyEvent();
    e.departmentId = site;
    e.itemId = item;
    e.reportId = report;
    e.claimId = claim;
    e.disposalId = disposal;
    e.actorId = access.current().id;
    e.action = action;
    e.note = note;
    e.createdAt = clock.instant();
    db.save(e);
    access.audit(action, e.id, site);
  }

  List<ClaimCase> activeClaims() {
    return db.query(
        ClaimCase.class,
        "from ClaimCase where status in ('PROPOSED','AWAITING_REVIEW','READY','DELIVERED')");
  }

  void category(String c) {
    Rules.check(
        !db.query(
                DictionaryEntry.class,
                "from DictionaryEntry where type='CATEGORY' and code=?1 and enabled=true",
                c)
            .isEmpty(),
        "CATEGORY_DISABLED");
  }

  LocalDate past(Object s, Long site) {
    var d = LocalDate.parse(Rules.text(s, 10, true));
    if (d.isAfter(today(site))) throw new Problem(400, "DATE_FUTURE");
    return d;
  }

  List<PhotoAsset> photos(Long item, Long report) {
    return item != null
        ? db.query(PhotoAsset.class, "from PhotoAsset where itemId=?1 and active=true", item)
        : db.query(PhotoAsset.class, "from PhotoAsset where reportId=?1 and active=true", report);
  }

  /** 当前最小配置；没有业务权限的账号不会得到私有保管数据。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object options() {
    access.current();
    var out = new LinkedHashMap<String, Object>();
    out.put(
        "sites",
        db.all(Department.class).stream()
            .filter(x -> x.enabled && access.department(x.id))
            .toList());
    out.put("categories", db.all(DictionaryEntry.class).stream().filter(x -> x.enabled).toList());
    out.put(
        "spots",
        access.has("items") && !access.role().scope.equals("ASSIGNED")
            ? db.all(StorageSpot.class).stream()
                .filter(x -> access.department(x.departmentId))
                .toList()
            : List.of());
    return out;
  }

  /** 保管位置目录，不向失主暴露物品所在柜位。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object spots() {
    access.staff("intake");
    return db.all(StorageSpot.class).stream()
        .filter(x -> access.department(x.departmentId))
        .toList();
  }

  /** 新增或更新位置；停用现用柜位被拒绝。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public StorageSpot saveSpot(Long id, Map<String, Object> b) {
    access.staff("intake");
    admin.lock();
    var x = id == null ? new StorageSpot() : db.get(StorageSpot.class, id);
    if (id == null) x.departmentId = Rules.id(b.get("departmentId"));
    else {
      version(x.version, b);
      Rules.check(x.departmentId.equals(Rules.id(b.get("departmentId"))), "IDENTITY_LOCKED");
      x.version++;
    }
    site(x.departmentId, "intake");
    Rules.check(db.get(Department.class, x.departmentId).enabled, "DEPARTMENT_DISABLED");
    String code = CustodyPolicy.code(b.get("code"));
    if (id != null) Rules.check(code.equals(x.code), "IDENTITY_LOCKED");
    Rules.check(
        db
            .query(
                StorageSpot.class,
                "from StorageSpot where departmentId=?1 and code=?2",
                x.departmentId,
                code)
            .stream()
            .noneMatch(v -> !Objects.equals(v.id, id)),
        "CODE_DUPLICATE");
    x.code = code;
    x.name = Rules.text(b.get("name"), 200, true);
    x.enabled = Rules.flag(b.get("enabled"));
    if (!x.enabled)
      Rules.check(
          db.query(
                  FoundItem.class,
                  "from FoundItem where storageId=?1 and status not in ('RETURNED','DISPOSED','VOID')",
                  id == null ? -1L : id)
              .isEmpty(),
          "LOCATION_BUSY");
    if (id == null) db.save(x);
    access.audit("SPOT_SAVE", x.id, x.departmentId);
    return x;
  }

  /** 无历史引用的错误位置可删除；数据库外键继续保护历史。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public void deleteSpot(Long id, long v) {
    admin.lock();
    var x = db.get(StorageSpot.class, id);
    site(x.departmentId, "intake");
    Rules.check(x.version == v, "VERSION_CONFLICT");
    Rules.check(
        db.query(FoundItem.class, "from FoundItem where storageId=?1", id).isEmpty(),
        "RECORD_REFERENCED");
    access.audit("SPOT_DELETE", id, x.departmentId);
    db.delete(x);
  }

  /** 搜索保管目录；所有字段均为工作人员私有范围。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object items(String q, String state, int page, int size, String sort) {
    access.staff("items");
    CustodyPolicy.page(page, size);
    String term = Rules.text(q, 200, false).toLowerCase(Locale.ROOT);
    if (!Set.of("latest", "due", "code").contains(sort)) throw new Problem(400, "INVALID_INPUT");
    if (!Set.of(
            "",
            "STORED",
            "RESERVED",
            "HANDED_OVER",
            "RETURNED",
            "DISPOSAL_PENDING",
            "DISPOSAL_APPROVED",
            "DISPOSED",
            "VOID",
            "DUE")
        .contains(state)) throw new Problem(400, "INVALID_INPUT");
    var rows =
        db.all(FoundItem.class).stream()
            .filter(x -> access.department(x.departmentId))
            .filter(
                x ->
                    state.isEmpty()
                        || (state.equals("DUE")
                            ? x.status.equals("STORED")
                                && CustodyPolicy.due(x.retainUntil, today(x.departmentId))
                            : x.status.equals(state)))
            .filter(
                x ->
                    (x.title + " " + x.code + " " + x.color + " " + x.foundPlace)
                        .toLowerCase(Locale.ROOT)
                        .contains(term))
            .sorted(
                sort.equals("code")
                    ? Comparator.comparing((FoundItem x) -> x.code)
                    : sort.equals("due")
                        ? Comparator.comparing((FoundItem x) -> x.retainUntil)
                            .thenComparing(x -> x.id)
                        : Comparator.comparing((FoundItem x) -> x.id).reversed())
            .toList();
    return paged(rows, page, size);
  }

  Object paged(List<?> rows, int page, int size) {
    return Map.of(
        "items",
        rows.stream().skip((long) (page - 1) * size).limit(size).toList(),
        "total",
        rows.size(),
        "page",
        page,
        "size",
        size);
  }

  /** 查询物品以及私有照片元数据，不给普通失主提供保管库存。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object item(Long id) {
    var x = db.get(FoundItem.class, id);
    site(x.departmentId, "items");
    return Map.of(
        "item", x,
        "photos", photos(id, null),
        "history", historyRows("item", id, true),
        "canEdit",
            x.status.equals("STORED")
                && db.query(ClaimCase.class, "from ClaimCase where itemId=?1", id).isEmpty()
                && db.query(DisposalCase.class, "from DisposalCase where itemId=?1", id).isEmpty(),
        "retentionDue", CustodyPolicy.due(x.retainUntil, today(x.departmentId)));
  }

  /** 收存物品；保管期限取当前策略快照，有认领历史后原识别信息不可改写。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public FoundItem saveItem(Long id, Map<String, Object> b) {
    access.staff("intake");
    admin.lock();
    var x = id == null ? new FoundItem() : db.get(FoundItem.class, id);
    if (id == null) {
      x.departmentId = Rules.id(b.get("departmentId"));
      x.receivedDate =
          b.get("receivedDate") == null
              ? today(x.departmentId)
              : past(b.get("receivedDate"), x.departmentId);
      x.retentionDays = admin.setting("retention_days");
      x.retainUntil = x.receivedDate.plusDays(x.retentionDays);
      x.createdBy = access.current().id;
      x.createdAt = clock.instant();
    } else {
      version(x.version, b);
      Rules.check(x.status.equals("STORED"), "ITEM_NOT_STORED");
      Rules.check(
          db.query(ClaimCase.class, "from ClaimCase where itemId=?1", id).isEmpty()
              && db.query(DisposalCase.class, "from DisposalCase where itemId=?1", id).isEmpty(),
          "ITEM_FROZEN");
      Rules.check(x.departmentId.equals(Rules.id(b.get("departmentId"))), "IDENTITY_LOCKED");
      if (b.get("receivedDate") != null)
        Rules.check(
            x.receivedDate.equals(LocalDate.parse(Rules.text(b.get("receivedDate"), 10, true))),
            "IDENTITY_LOCKED");
      x.version++;
    }
    site(x.departmentId, "intake");
    Rules.check(db.get(Department.class, x.departmentId).enabled, "DEPARTMENT_DISABLED");
    String code = CustodyPolicy.code(b.get("code"));
    if (id != null) Rules.check(code.equals(x.code), "IDENTITY_LOCKED");
    Rules.check(
        db.query(FoundItem.class, "from FoundItem where code=?1", code).stream()
            .noneMatch(v -> !Objects.equals(v.id, id)),
        "CODE_DUPLICATE");
    x.code = code;
    x.title = Rules.text(b.get("title"), 200, true);
    x.category = Rules.text(b.get("category"), 80, true);
    category(x.category);
    x.color = Rules.text(b.get("color"), 80, false);
    x.foundDate = past(b.get("foundDate"), x.departmentId);
    if (x.foundDate.isAfter(x.receivedDate)) throw new Problem(400, "DATE_ORDER");
    x.foundPlace = Rules.text(b.get("foundPlace"), 200, true);
    x.privateMarks = Rules.paragraph(b.get("privateMarks"), 2000, true);
    x.note = Rules.paragraph(b.get("note"), 2000, false);
    x.storageId = Rules.id(b.get("storageId"));
    var spot = db.get(StorageSpot.class, x.storageId);
    Rules.check(spot.enabled && spot.departmentId.equals(x.departmentId), "LOCATION_INVALID");
    if (id == null) db.save(x);
    event(id == null ? "ITEM_RECEIVED" : "ITEM_EDITED", x.departmentId, x.id, null, null, null, "");
    return x;
  }

  /** 位置转移独立留痕，不改变冻结特征和保管期限。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public FoundItem move(Long id, Map<String, Object> b) {
    admin.lock();
    var x = db.get(FoundItem.class, id);
    site(x.departmentId, "intake");
    version(x.version, b);
    Rules.check(Set.of("STORED", "RESERVED").contains(x.status), "ITEM_NOT_STORED");
    var spot = db.get(StorageSpot.class, Rules.id(b.get("storageId")));
    Rules.check(spot.enabled && spot.departmentId.equals(x.departmentId), "LOCATION_INVALID");
    String note = Rules.paragraph(b.get("note"), 2000, true);
    x.storageId = spot.id;
    x.version++;
    event("ITEM_MOVED", x.departmentId, id, null, null, null, note);
    return x;
  }

  /** 作废错误收存记录；有认领或处置历史必须保留并不能作废。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public FoundItem voidItem(Long id, Map<String, Object> b) {
    admin.lock();
    var x = db.get(FoundItem.class, id);
    site(x.departmentId, "intake");
    version(x.version, b);
    Rules.check(x.status.equals("STORED"), "ITEM_NOT_STORED");
    Rules.check(
        db.query(ClaimCase.class, "from ClaimCase where itemId=?1", id).isEmpty()
            && db.query(DisposalCase.class, "from DisposalCase where itemId=?1", id).isEmpty(),
        "RECORD_REFERENCED");
    String n = Rules.paragraph(b.get("note"), 2000, true);
    x.status = "VOID";
    x.version++;
    event("ITEM_VOID", x.departmentId, id, null, null, null, n);
    return x;
  }

  /** 报失列表：有工作人员权限才看服务点所有记录，否则仅本人。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object reports(String q, String state, int page, int size, String sort, boolean mine) {
    access.current();
    if (mine || !staffReports()) access.require("request");
    CustodyPolicy.page(page, size);
    String term = Rules.text(q, 200, false).toLowerCase(Locale.ROOT);
    if (!Set.of("latest", "date").contains(sort)
        || !Set.of("", "OPEN", "MATCHING", "RESOLVED", "WITHDRAWN").contains(state))
      throw new Problem(400, "INVALID_INPUT");
    var rows =
        db.all(LostReport.class).stream()
            .filter(access::report)
            .filter(x -> !mine || x.reporterId.equals(access.current().id))
            .filter(x -> state.isEmpty() || x.status.equals(state))
            .filter(
                x ->
                    (x.title + " " + x.color + " " + x.lostPlace)
                        .toLowerCase(Locale.ROOT)
                        .contains(term))
            .sorted(
                sort.equals("date")
                    ? Comparator.comparing((LostReport x) -> x.lostDate).thenComparing(x -> x.id)
                    : Comparator.comparing((LostReport x) -> x.id).reversed())
            .toList();
    return paged(rows, page, size);
  }

  /** 报失详情只返回本人或本服务点工作人员可见数据。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object report(Long id, boolean mine) {
    var r = db.get(LostReport.class, id);
    ownReport(r);
    if (mine) requester(r);
    return Map.of(
        "report",
        r,
        "photos",
        photos(null, id),
        "claims",
        db.query(ClaimCase.class, "from ClaimCase where reportId=?1 order by id desc", id).stream()
            .map(c -> claimView(c, !mine && staffReports()))
            .toList(),
        "history",
        historyRows("report", id, !mine && staffReports()));
  }

  /** 本人提交或修改报失；作者和服务点不可伪造，无公开物品目录。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public LostReport saveReport(Long id, Map<String, Object> b) {
    access.require("request");
    admin.lock();
    var r = id == null ? new LostReport() : db.get(LostReport.class, id);
    if (id == null) {
      r.departmentId = access.current().departmentId;
      r.reporterId = access.current().id;
      r.createdAt = clock.instant();
      Rules.check(
          db.query(
                      LostReport.class,
                      "from LostReport where reporterId=?1 and status in ('OPEN','MATCHING')",
                      r.reporterId)
                  .size()
              < admin.setting("max_open_reports"),
          "REPORT_LIMIT");
    } else {
      requester(r);
      version(r.version, b);
      Rules.check(
          r.status.equals("OPEN") && activeClaims().stream().noneMatch(c -> c.reportId.equals(id)),
          "REPORT_BUSY");
      r.version++;
    }
    if (b.get("reporterId") != null && !r.reporterId.equals(Rules.id(b.get("reporterId"))))
      throw new Problem(403, "OUT_OF_SCOPE");
    if (b.get("departmentId") != null && !r.departmentId.equals(Rules.id(b.get("departmentId"))))
      throw new Problem(403, "OUT_OF_SCOPE");
    r.title = Rules.text(b.get("title"), 200, true);
    r.category = Rules.text(b.get("category"), 80, true);
    category(r.category);
    r.color = Rules.text(b.get("color"), 80, false);
    r.lostDate = past(b.get("lostDate"), r.departmentId);
    r.lostPlace = Rules.text(b.get("lostPlace"), 200, true);
    r.description = Rules.paragraph(b.get("description"), 2000, true);
    if (id == null) db.save(r);
    event(
        id == null ? "REPORT_CREATED" : "REPORT_EDITED",
        r.departmentId,
        null,
        r.id,
        null,
        null,
        "");
    return r;
  }

  void closeClaim(ClaimCase c, String status, String note) {
    if (c.status.equals("READY")) {
      var x = db.get(FoundItem.class, c.itemId);
      if (x.status.equals("RESERVED")) {
        x.status = "STORED";
        x.version++;
      }
      var r = db.get(LostReport.class, c.reportId);
      if (r.status.equals("MATCHING")) {
        r.status = "OPEN";
        r.version++;
      }
    }
    c.status = status;
    c.reviewNote = note;
    c.closedAt = clock.instant();
    c.version++;
    event("CLAIM_" + status, c.departmentId, c.itemId, c.reportId, c.id, null, "");
  }

  /** 本人撤回未交出的报失，候选/保留同步取消，不影响其他失主。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public LostReport withdraw(Long id, Map<String, Object> b) {
    admin.lock();
    var r = db.get(LostReport.class, id);
    requester(r);
    version(r.version, b);
    Rules.check(Set.of("OPEN", "MATCHING").contains(r.status), "REPORT_CLOSED");
    var claims = activeClaims().stream().filter(c -> c.reportId.equals(id)).toList();
    Rules.check(claims.stream().noneMatch(c -> c.status.equals("DELIVERED")), "HANDOVER_COMPLETE");
    for (var c : claims) closeClaim(c, "CANCELLED", "REPORT_WITHDRAWN");
    r.status = "WITHDRAWN";
    r.version++;
    event(
        "REPORT_WITHDRAWN",
        r.departmentId,
        null,
        id,
        null,
        null,
        Rules.paragraph(b.get("note"), 2000, true));
    return r;
  }

  /** 人工候选只供工作人员筛选，不自动确认所有权或外发通知。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object candidates(Long id) {
    var r = db.get(LostReport.class, id);
    site(r.departmentId, "propose");
    return db.all(FoundItem.class).stream()
        .filter(
            x ->
                x.departmentId.equals(r.departmentId)
                    && x.status.equals("STORED")
                    && x.category.equals(r.category)
                    && !x.foundDate.isBefore(r.lostDate.minusDays(2)))
        .sorted(Comparator.comparing((FoundItem x) -> x.foundDate).thenComparing(x -> x.id))
        .map(
            x ->
                Map.of(
                    "id",
                    x.id,
                    "code",
                    x.code,
                    "title",
                    x.title,
                    "color",
                    x.color,
                    "foundDate",
                    x.foundDate,
                    "foundPlace",
                    x.foundPlace))
        .toList();
  }

  /** 提出候选时冻结描述快照；失主不接收隐藏特征和保管照片。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object propose(Map<String, Object> b) {
    access.staff("propose");
    admin.lock();
    var r = db.get(LostReport.class, Rules.id(b.get("reportId")));
    var x = db.get(FoundItem.class, Rules.id(b.get("itemId")));
    site(r.departmentId, "propose");
    Rules.check(r.departmentId.equals(x.departmentId), "SITE_MISMATCH");
    Rules.check(!r.reporterId.equals(access.current().id), "SELF_REVIEW");
    Rules.check(r.status.equals("OPEN"), "REPORT_CLOSED");
    Rules.check(x.status.equals("STORED"), "ITEM_NOT_STORED");
    Rules.check(
        activeClaims().stream().noneMatch(c -> c.itemId.equals(x.id) && c.reportId.equals(r.id)),
        "PROPOSAL_DUPLICATE");
    var c = new ClaimCase();
    c.departmentId = r.departmentId;
    c.itemId = x.id;
    c.reportId = r.id;
    c.claimantId = r.reporterId;
    c.proposedBy = access.current().id;
    c.itemTitle = x.title;
    c.itemMarks = x.privateMarks;
    c.reportTitle = r.title;
    c.reportDescription = r.description;
    c.message = Rules.paragraph(b.get("message"), 2000, true);
    c.createdAt = clock.instant();
    db.save(c);
    event("CLAIM_PROPOSED", c.departmentId, x.id, r.id, c.id, null, "");
    return claimView(c);
  }

  /** 本人提供私有认领特征；工作人员不能代替失主提交确认。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object evidence(Long id, Map<String, Object> b) {
    admin.lock();
    var c = db.get(ClaimCase.class, id);
    requester(db.get(LostReport.class, c.reportId));
    version(c.version, b);
    Rules.check(Set.of("PROPOSED", "AWAITING_REVIEW").contains(c.status), "CLAIM_STATE");
    c.evidence = Rules.paragraph(b.get("evidence"), 2000, true);
    Rules.check(c.evidence.length() >= 10, "EVIDENCE_REQUIRED");
    c.status = "AWAITING_REVIEW";
    c.submittedAt = clock.instant();
    c.version++;
    event("CLAIM_EVIDENCE", c.departmentId, c.itemId, c.reportId, id, null, "");
    return claimView(c);
  }

  /** 独立人工核验；不能自审提出的候选，批准只预留物品，不自动交出。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object review(Long id, Map<String, Object> b) {
    admin.lock();
    var c = db.get(ClaimCase.class, id);
    site(c.departmentId, "review");
    version(c.version, b);
    Rules.check(c.status.equals("AWAITING_REVIEW"), "CLAIM_STATE");
    Rules.check(
        !access.current().id.equals(c.proposedBy) && !access.current().id.equals(c.claimantId),
        "SELF_REVIEW");
    String note = Rules.paragraph(b.get("reviewNote"), 2000, true);
    Rules.check(note.length() >= 10, "EVIDENCE_REQUIRED");
    c.message = Rules.paragraph(b.get("message"), 2000, true);
    boolean approve = Rules.flag(b.get("approve"));
    c.reviewedBy = access.current().id;
    c.reviewedAt = clock.instant();
    c.reviewNote = note;
    if (!approve) {
      closeClaim(c, "REJECTED", note);
      return claimView(c);
    }
    var x = db.get(FoundItem.class, c.itemId);
    var r = db.get(LostReport.class, c.reportId);
    Rules.check(x.status.equals("STORED") && r.status.equals("OPEN"), "ITEM_NOT_STORED");
    var owner = db.get(Account.class, c.claimantId);
    Rules.check(
        owner.enabled
            && db.get(AccessRole.class, owner.roleId).permissions.contains("request")
            && db.get(Department.class, c.departmentId).enabled,
        "CLAIMANT_DISABLED");
    for (var other : activeClaims())
      if (!other.id.equals(id) && (other.itemId.equals(x.id) || other.reportId.equals(r.id)))
        closeClaim(other, "REJECTED", "OTHER_CLAIM_APPROVED");
    x.status = "RESERVED";
    x.version++;
    r.status = "MATCHING";
    r.version++;
    c.status = "READY";
    c.pickupUntil = clock.instant().plusSeconds(admin.setting("pickup_hours") * 3600L);
    c.version++;
    event("CLAIM_APPROVED", c.departmentId, x.id, r.id, id, null, "");
    return claimView(c);
  }

  /** 交出前取消候选/保留；交出后不能通过取消把实物重新入库。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object cancel(Long id, Map<String, Object> b) {
    admin.lock();
    var c = db.get(ClaimCase.class, id);
    if (staffReports()) site(c.departmentId, "propose");
    else requester(db.get(LostReport.class, c.reportId));
    version(c.version, b);
    Rules.check(Set.of("PROPOSED", "AWAITING_REVIEW", "READY").contains(c.status), "CLAIM_STATE");
    closeClaim(c, "CANCELLED", Rules.paragraph(b.get("note"), 2000, true));
    return claimView(c);
  }

  /** 保管员按核验结果实际交出；核验员不得兼作交出人。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object handover(Long id, Map<String, Object> b) {
    admin.lock();
    var c = db.get(ClaimCase.class, id);
    site(c.departmentId, "handover");
    version(c.version, b);
    Rules.check(c.status.equals("READY"), "CLAIM_STATE");
    Rules.check(!CustodyPolicy.expired(c.pickupUntil, clock.instant()), "PICKUP_EXPIRED");
    Rules.check(
        !access.current().id.equals(c.reviewedBy) && !access.current().id.equals(c.claimantId),
        "SELF_REVIEW");
    var a = db.get(Account.class, c.claimantId);
    Rules.check(
        a.enabled && db.get(AccessRole.class, a.roleId).permissions.contains("request"),
        "CLAIMANT_DISABLED");
    Rules.check(Rules.flag(b.get("physicalConfirmed")), "PHYSICAL_CONFIRMATION");
    var x = db.get(FoundItem.class, c.itemId);
    Rules.check(x.status.equals("RESERVED"), "ITEM_NOT_STORED");
    c.handoverNote = Rules.paragraph(b.get("note"), 2000, true);
    c.status = "DELIVERED";
    c.handedBy = access.current().id;
    c.handedAt = clock.instant();
    c.version++;
    x.status = "HANDED_OVER";
    x.version++;
    event("ITEM_HANDED_OVER", c.departmentId, x.id, c.reportId, id, null, "");
    return claimView(c);
  }

  /** 失主本人确认实物收讫；不能由保管员代填签收。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object receipt(Long id, Map<String, Object> b) {
    admin.lock();
    var c = db.get(ClaimCase.class, id);
    requester(db.get(LostReport.class, c.reportId));
    version(c.version, b);
    Rules.check(c.status.equals("DELIVERED"), "CLAIM_STATE");
    Rules.check(Rules.flag(b.get("physicalConfirmed")), "PHYSICAL_CONFIRMATION");
    c.receiptNote = Rules.paragraph(b.get("note"), 2000, false);
    c.status = "CLOSED";
    c.receivedAt = clock.instant();
    c.closedAt = c.receivedAt;
    c.version++;
    var x = db.get(FoundItem.class, c.itemId);
    Rules.check(x.status.equals("HANDED_OVER"), "ITEM_NOT_STORED");
    x.status = "RETURNED";
    x.version++;
    var r = db.get(LostReport.class, c.reportId);
    r.status = "RESOLVED";
    r.version++;
    event("CLAIM_RECEIVED", c.departmentId, x.id, r.id, id, null, "");
    return claimView(c);
  }

  /** 工作人员显式清理已到领取截止的保留；GET从不改变状态。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object expire() {
    access.staff("handover");
    admin.lock();
    int n = 0;
    for (var c : activeClaims())
      if (access.department(c.departmentId)
          && c.status.equals("READY")
          && CustodyPolicy.expired(c.pickupUntil, clock.instant())) {
        closeClaim(c, "EXPIRED", "PICKUP_EXPIRED");
        n++;
      }
    return Map.of("expired", n);
  }

  /** 服务端构造可见认领数据；普通失主绝不收到隐藏特征或工作人员核验笔记。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> claimView(ClaimCase c) {
    return claimView(c, staffReports());
  }

  /** 本人业务入口强制使用私有字段最小投影，即使当前账号另有工作人员角色。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> claimView(ClaimCase c, boolean privateView) {
    var v = new LinkedHashMap<String, Object>();
    v.put("id", c.id);
    v.put("departmentId", c.departmentId);
    v.put("reportId", c.reportId);
    v.put("status", c.status);
    v.put("message", c.message);
    v.put("itemTitle", c.itemTitle);
    v.put("reportTitle", c.reportTitle);
    v.put("reportDescription", c.reportDescription);
    v.put("evidence", c.evidence);
    v.put("version", c.version);
    v.put("createdAt", c.createdAt);
    v.put("pickupUntil", c.pickupUntil);
    v.put(
        "pickupExpired",
        c.status.equals("READY") && CustodyPolicy.expired(c.pickupUntil, clock.instant()));
    v.put("handedAt", c.handedAt);
    v.put("receivedAt", c.receivedAt);
    v.put("receiptNote", c.receiptNote);
    if (privateView) {
      v.put("itemId", c.itemId);
      v.put("claimantId", c.claimantId);
      v.put("claimantName", db.get(Account.class, c.claimantId).displayName);
      v.put("proposedBy", c.proposedBy);
      v.put("reviewedBy", c.reviewedBy);
      v.put("handedBy", c.handedBy);
      v.put("itemMarks", c.itemMarks);
      v.put("reviewNote", c.reviewNote);
      v.put("handoverNote", c.handoverNote);
    }
    return v;
  }

  /** 当前授权范围的认领记录，搜索/状态/分页/排序在服务端执行。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object claims(String q, String state, int page, int size, String sort, boolean mine) {
    if (mine || !staffReports()) access.require("request");
    CustodyPolicy.page(page, size);
    String term = Rules.text(q, 200, false).toLowerCase(Locale.ROOT);
    if (!Set.of("latest", "pickup").contains(sort)
        || !Set.of(
                "",
                "PROPOSED",
                "AWAITING_REVIEW",
                "READY",
                "DELIVERED",
                "CLOSED",
                "REJECTED",
                "CANCELLED",
                "EXPIRED")
            .contains(state)) throw new Problem(400, "INVALID_INPUT");
    var rows =
        db.all(ClaimCase.class).stream()
            .filter(c -> access.report(db.get(LostReport.class, c.reportId)))
            .filter(c -> !mine || c.claimantId.equals(access.current().id))
            .filter(c -> state.isEmpty() || c.status.equals(state))
            .filter(
                c ->
                    (c.itemTitle + " " + c.reportTitle + " " + c.id)
                        .toLowerCase(Locale.ROOT)
                        .contains(term))
            .sorted(
                sort.equals("pickup")
                    ? Comparator.comparing(
                            (ClaimCase c) -> c.pickupUntil,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(c -> c.id)
                    : Comparator.comparing((ClaimCase c) -> c.id).reversed())
            .map(c -> claimView(c, !mine && staffReports()))
            .toList();
    return paged(rows, page, size);
  }

  /** 单个认领详情权限与列表一致。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object claim(Long id, boolean mine) {
    var c = db.get(ClaimCase.class, id);
    claimRead(c);
    if (mine) requester(db.get(LostReport.class, c.reportId));
    return Map.of(
        "claim",
        claimView(c, !mine && staffReports()),
        "history",
        historyRows("claim", id, !mine && staffReports()));
  }

  /** 到期处置申请；有任何待办认领不能处置，期限由入库策略快照决定。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public DisposalCase disposalRequest(Map<String, Object> b) {
    admin.lock();
    var x = db.get(FoundItem.class, Rules.id(b.get("itemId")));
    site(x.departmentId, "intake");
    version(x.version, b);
    Rules.check(x.status.equals("STORED"), "ITEM_NOT_STORED");
    Rules.check(CustodyPolicy.due(x.retainUntil, today(x.departmentId)), "RETENTION_NOT_DUE");
    Rules.check(activeClaims().stream().noneMatch(c -> c.itemId.equals(x.id)), "ACTIVE_CLAIMS");
    var d = new DisposalCase();
    d.departmentId = x.departmentId;
    d.itemId = x.id;
    d.requestedBy = access.current().id;
    d.method = Rules.text(b.get("method"), 80, true);
    Rules.check(
        Set.of("DONATE", "RECYCLE", "DESTROY", "TRANSFER").contains(d.method), "INVALID_INPUT");
    d.reason = Rules.paragraph(b.get("reason"), 2000, true);
    d.createdAt = clock.instant();
    db.save(d);
    x.status = "DISPOSAL_PENDING";
    x.version++;
    event("DISPOSAL_REQUESTED", d.departmentId, x.id, null, null, d.id, "");
    return d;
  }

  /** 独立处置批准，不允许申请人自批；拒绝恢复保管，不擦除记录。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public DisposalCase disposalReview(Long id, Map<String, Object> b) {
    admin.lock();
    var d = db.get(DisposalCase.class, id);
    site(d.departmentId, "dispose");
    version(d.version, b);
    Rules.check(d.status.equals("PENDING"), "DISPOSAL_STATE");
    Rules.check(!d.requestedBy.equals(access.current().id), "SELF_REVIEW");
    var x = db.get(FoundItem.class, d.itemId);
    Rules.check(x.status.equals("DISPOSAL_PENDING"), "ITEM_NOT_STORED");
    d.reviewNote = Rules.paragraph(b.get("note"), 2000, true);
    d.reviewedBy = access.current().id;
    d.reviewedAt = clock.instant();
    boolean approve = Rules.flag(b.get("approve"));
    d.status = approve ? "APPROVED" : "REJECTED";
    x.status = approve ? "DISPOSAL_APPROVED" : "STORED";
    d.version++;
    x.version++;
    event("DISPOSAL_" + d.status, d.departmentId, x.id, null, null, id, "");
    return d;
  }

  /** 已批准处置的实物执行；批准人不可兼作执行人。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public DisposalCase disposalExecute(Long id, Map<String, Object> b) {
    admin.lock();
    var d = db.get(DisposalCase.class, id);
    site(d.departmentId, "intake");
    version(d.version, b);
    Rules.check(d.status.equals("APPROVED"), "DISPOSAL_STATE");
    Rules.check(!access.current().id.equals(d.reviewedBy), "SELF_REVIEW");
    Rules.check(Rules.flag(b.get("physicalConfirmed")), "PHYSICAL_CONFIRMATION");
    var x = db.get(FoundItem.class, d.itemId);
    Rules.check(x.status.equals("DISPOSAL_APPROVED"), "ITEM_NOT_STORED");
    d.executionNote = Rules.paragraph(b.get("note"), 2000, true);
    d.executedBy = access.current().id;
    d.executedAt = clock.instant();
    d.status = "EXECUTED";
    d.version++;
    x.status = "DISPOSED";
    x.version++;
    event("DISPOSAL_EXECUTED", d.departmentId, x.id, null, null, id, "");
    return d;
  }

  /** 实物执行前撤销处置；执行完成记录不可逆改写。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public DisposalCase disposalCancel(Long id, Map<String, Object> b) {
    admin.lock();
    var d = db.get(DisposalCase.class, id);
    site(d.departmentId, "intake");
    version(d.version, b);
    Rules.check(Set.of("PENDING", "APPROVED").contains(d.status), "DISPOSAL_STATE");
    var x = db.get(FoundItem.class, d.itemId);
    d.reviewNote = Rules.paragraph(b.get("note"), 2000, true);
    d.status = "CANCELLED";
    d.version++;
    x.status = "STORED";
    x.version++;
    event("DISPOSAL_CANCELLED", d.departmentId, x.id, null, null, id, "");
    return d;
  }

  /** 工作人员查看本服务点处置记录与物品名称。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object disposals(String q, String state, int page, int size) {
    access.staff("items");
    CustodyPolicy.page(page, size);
    String term = Rules.text(q, 200, false).toLowerCase(Locale.ROOT);
    if (!Set.of("", "PENDING", "APPROVED", "REJECTED", "EXECUTED", "CANCELLED").contains(state))
      throw new Problem(400, "INVALID_INPUT");
    var rows =
        db.all(DisposalCase.class).stream()
            .filter(d -> access.department(d.departmentId))
            .filter(d -> state.isEmpty() || d.status.equals(state))
            .filter(
                d ->
                    (db.get(FoundItem.class, d.itemId).title + " " + d.id)
                        .toLowerCase(Locale.ROOT)
                        .contains(term))
            .sorted(Comparator.comparing((DisposalCase d) -> d.id).reversed())
            .map(
                d ->
                    Map.of(
                        "disposal",
                        d,
                        "itemTitle",
                        db.get(FoundItem.class, d.itemId).title,
                        "itemCode",
                        db.get(FoundItem.class, d.itemId).code))
            .toList();
    return paged(rows, page, size);
  }

  List<?> historyRows(String kind, Long id, boolean privateView) {
    var rows =
        db.all(CustodyEvent.class).stream()
            .filter(
                e ->
                    switch (kind) {
                      case "item" -> Objects.equals(e.itemId, id);
                      case "report" -> Objects.equals(e.reportId, id);
                      case "claim" -> Objects.equals(e.claimId, id);
                      case "disposal" -> Objects.equals(e.disposalId, id);
                      default -> false;
                    })
            .sorted(Comparator.comparing((CustodyEvent e) -> e.id).reversed())
            .toList();
    if (privateView) return rows;
    return rows.stream()
        .map(e -> Map.of("id", e.id, "action", e.action, "createdAt", e.createdAt))
        .toList();
  }

  /** 概览真实授权范围，失主只获得本人记录指标。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object dashboard(boolean mine) {
    if (mine || !access.has("reports")) {
      access.require("request");
      var reports =
          db.all(LostReport.class).stream()
              .filter(
                  r ->
                      r.reporterId.equals(access.current().id) && access.department(r.departmentId))
              .toList();
      var claims =
          db.all(ClaimCase.class).stream()
              .filter(
                  c ->
                      c.claimantId.equals(access.current().id) && access.department(c.departmentId))
              .toList();
      return Map.of(
          "openReports",
          reports.stream()
              .filter(r -> r.status.equals("OPEN") || r.status.equals("MATCHING"))
              .count(),
          "ready",
          claims.stream()
              .filter(
                  c ->
                      c.status.equals("READY")
                          && !CustodyPolicy.expired(c.pickupUntil, clock.instant()))
              .count(),
          "receiptDue",
          claims.stream().filter(c -> c.status.equals("DELIVERED")).count(),
          "closed",
          claims.stream().filter(c -> c.status.equals("CLOSED")).count());
    }
    access.staff("reports");
    var items =
        db.all(FoundItem.class).stream().filter(x -> access.department(x.departmentId)).toList();
    var out = new LinkedHashMap<String, Object>();
    for (String status : List.of("STORED", "RESERVED", "HANDED_OVER", "RETURNED", "DISPOSED"))
      out.put(status, items.stream().filter(x -> x.status.equals(status)).count());
    out.put(
        "retentionDue",
        items.stream()
            .filter(
                x ->
                    x.status.equals("STORED")
                        && CustodyPolicy.due(x.retainUntil, today(x.departmentId)))
            .count());
    out.put(
        "verificationDue",
        db.all(ClaimCase.class).stream()
            .filter(c -> access.department(c.departmentId) && c.status.equals("AWAITING_REVIEW"))
            .count());
    out.put(
        "disposalDue",
        db.all(DisposalCase.class).stream()
            .filter(d -> access.department(d.departmentId) && d.status.equals("PENDING"))
            .count());
    return out;
  }

  /** 保管台账CSV不包含隐藏特征、失主描述、核验备注或图片。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public String export() {
    access.staff("reports");
    StringBuilder out =
        new StringBuilder("code,title,category,found_date,received_date,retain_until,status\r\n");
    for (var x : db.all(FoundItem.class))
      if (access.department(x.departmentId)) {
        out.append(
                String.join(
                    ",",
                    List.of(
                        Rules.csv(x.code),
                        Rules.csv(x.title),
                        Rules.csv(x.category),
                        Rules.csv(x.foundDate),
                        Rules.csv(x.receivedDate),
                        Rules.csv(x.retainUntil),
                        Rules.csv(x.status))))
            .append("\r\n");
      }
    return out.toString();
  }
}
