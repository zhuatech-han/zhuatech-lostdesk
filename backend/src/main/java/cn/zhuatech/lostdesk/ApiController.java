// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 同源业务端点；范围、角色、版本和状态由服务端执行。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@RestController
@RequestMapping("/api")
public class ApiController {
  final CustodyService business;
  final PhotoService photos;
  final AdminService admin;
  final AccessService access;
  final Store db;

  public ApiController(CustodyService b, PhotoService p, AdminService a, AccessService s, Store d) {
    business = b;
    photos = p;
    admin = a;
    access = s;
    db = d;
  }

  /** 最小业务选项。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/options")
  public Object options() {
    return business.options();
  }

  /** 私有保管物品搜索。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/items")
  public Object items(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String state,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size,
      @RequestParam(defaultValue = "latest") String sort) {
    return business.items(q, state, page, size, sort);
  }

  /** 私有物品详情。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/items/{id}")
  public Object item(@PathVariable Long id) {
    return business.item(id);
  }

  /** 收存记录。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/items")
  public Object itemCreate(@RequestBody Map<String, Object> b) {
    return business.saveItem(null, b);
  }

  /** 更新未冻结记录。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/items/{id}")
  public Object itemUpdate(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.saveItem(id, b);
  }

  /** 位置转移。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/items/{id}/move")
  public Object move(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.move(id, b);
  }

  /** 作废错误收存。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/items/{id}/void")
  public Object voidItem(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.voidItem(id, b);
  }

  /** 报失列表按本人或工作人员范围过滤。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/lost-reports")
  public Object reports(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String state,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "false") boolean mine) {
    return business.reports(q, state, page, size, sort, mine);
  }

  /** 报失及候选详情。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/lost-reports/{id}")
  public Object report(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean mine) {
    return business.report(id, mine);
  }

  /** 本人新建报失。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/lost-reports")
  public Object reportCreate(@RequestBody Map<String, Object> b) {
    return business.saveReport(null, b);
  }

  /** 本人改正未进入认领的报失。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/lost-reports/{id}")
  public Object reportUpdate(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.saveReport(id, b);
  }

  /** 本人撤回。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/lost-reports/{id}/withdraw")
  public Object withdraw(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.withdraw(id, b);
  }

  /** 工作人员规则筛选候选。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/lost-reports/{id}/candidates")
  public Object candidates(@PathVariable Long id) {
    return business.candidates(id);
  }

  /** 认领列表与状态搜索。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/claims")
  public Object claims(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String state,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size,
      @RequestParam(defaultValue = "latest") String sort,
      @RequestParam(defaultValue = "false") boolean mine) {
    return business.claims(q, state, page, size, sort, mine);
  }

  /** 按当前身份构造认领详情。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/claims/{id}")
  public Object claim(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean mine) {
    return business.claim(id, mine);
  }

  /** 人工提出候选。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/claims")
  public Object propose(@RequestBody Map<String, Object> b) {
    return business.propose(b);
  }

  /** 本人提交私有特征。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/claims/{id}/evidence")
  public Object evidence(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.evidence(id, b);
  }

  /** 独立核验。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/claims/{id}/review")
  public Object review(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.review(id, b);
  }

  /** 交出前取消。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/claims/{id}/cancel")
  public Object cancel(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.cancel(id, b);
  }

  /** 实物交出。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/claims/{id}/handover")
  public Object handover(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.handover(id, b);
  }

  /** 本人确认收讫。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/claims/{id}/receipt")
  public Object receipt(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.receipt(id, b);
  }

  /** 显式到期整理。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/claims/expire")
  public Object expire() {
    return business.expire();
  }

  /** 保管位置目录。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/spots")
  public Object spots() {
    return business.spots();
  }

  /** 新增保管位置。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/spots")
  public Object spotCreate(@RequestBody Map<String, Object> b) {
    return business.saveSpot(null, b);
  }

  /** 更新保管位置。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/spots/{id}")
  public Object spotUpdate(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.saveSpot(id, b);
  }

  /** 删除未使用位置。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/spots/{id}")
  public Object spotDelete(@PathVariable Long id, @RequestParam long version) {
    business.deleteSpot(id, version);
    return Map.of("ok", true);
  }

  /** 处置申请记录。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/disposals")
  public Object disposals(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String state,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size) {
    return business.disposals(q, state, page, size);
  }

  /** 到期处置申请。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/disposals")
  public Object disposalCreate(@RequestBody Map<String, Object> b) {
    return business.disposalRequest(b);
  }

  /** 独立处置批准。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/disposals/{id}/review")
  public Object disposalReview(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.disposalReview(id, b);
  }

  /** 实际执行处置。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/disposals/{id}/execute")
  public Object disposalExecute(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.disposalExecute(id, b);
  }

  /** 执行前撤回处置。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/disposals/{id}/cancel")
  public Object disposalCancel(@PathVariable Long id, @RequestBody Map<String, Object> b) {
    return business.disposalCancel(id, b);
  }

  /** 限定对象的图片上传。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping(value = "/{kind:items|lost-reports}/{id}/photos", consumes = "multipart/form-data")
  public Object photoUpload(
      @PathVariable String kind, @PathVariable Long id, @RequestPart("file") MultipartFile file)
      throws Exception {
    return photos.upload(kind, id, file);
  }

  /** 私有图片原生加载；禁止共享缓存。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/photos/{id}")
  public ResponseEntity<byte[]> photo(@PathVariable Long id) {
    var p = photos.get(id);
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .cacheControl(CacheControl.noStore())
        .header("Content-Disposition", "inline; filename=photo-" + id + ".png")
        .body(p.content);
  }

  /** 认领核验前软删除图片。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/photos/{id}")
  public Object photoDelete(@PathVariable Long id, @RequestParam long version) {
    return photos.remove(id, version);
  }

  /** 真实范围统计。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/dashboard")
  public Object dashboard(@RequestParam(defaultValue = "false") boolean mine) {
    return business.dashboard(mine);
  }

  /** CSV最小业务台账。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/reports/export")
  public ResponseEntity<byte[]> export() {
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=lostdesk-inventory.csv")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .cacheControl(CacheControl.noStore())
        .body(business.export().getBytes(StandardCharsets.UTF_8));
  }

  /** 系统目录读取。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/admin/{kind}")
  public Object directory(@PathVariable String kind) {
    return admin.read(kind);
  }

  /** 账号配置最小选项。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/admin/options")
  public Object adminOptions() {
    return admin.options();
  }

  /** 系统目录新增。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/admin/{kind}")
  public Object directoryCreate(@PathVariable String kind, @RequestBody Map<String, Object> b) {
    return admin.save(kind, null, b);
  }

  /** 系统目录更新。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/admin/{kind}/{id}")
  public Object directoryUpdate(
      @PathVariable String kind, @PathVariable Long id, @RequestBody Map<String, Object> b) {
    return admin.save(kind, id, b);
  }

  /** 审计按服务点过滤，本人范围仅能查本人操作。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/audit")
  @Transactional(readOnly = true)
  public Object audit() {
    access.require("audit");
    return db.all(AuditEvent.class).stream()
        .filter(
            e ->
                access.department(e.departmentId)
                    && (!access.role().scope.equals("ASSIGNED")
                        || e.actor.equals(access.current().username)))
        .sorted(Comparator.comparing((AuditEvent e) -> e.id).reversed())
        .toList();
  }
}
