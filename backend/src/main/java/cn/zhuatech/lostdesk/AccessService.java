// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import java.time.Clock;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** 每次请求重新检查账号、密码指纹、角色及服务点范围。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Service
public class AccessService {
  final Store db;
  final Clock clock;

  /** 连接实时身份和审计时钟。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public AccessService(Store db, Clock clock) {
    this.db = db;
    this.clock = clock;
  }

  /** 停用账号、服务点或重置密码立即撤销旧会话。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Account current() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) throw new Problem(401, "UNAUTHENTICATED");
    var rows = db.query(Account.class, "from Account where username=?1", auth.getName());
    if (rows.isEmpty()) throw new Problem(401, "UNAUTHENTICATED");
    var a = rows.getFirst();
    if (!a.enabled
        || !Objects.equals(auth.getDetails(), a.passwordHash)
        || !db.get(Department.class, a.departmentId).enabled)
      throw new Problem(401, "UNAUTHENTICATED");
    return a;
  }

  /** 读取最新角色，不信任登录时复制的权限。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public AccessRole role() {
    return db.get(AccessRole.class, current().roleId);
  }

  /** 业务权限不足返回403。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public void require(String code) {
    if (!role().permissions.contains(code)) throw new Problem(403, "FORBIDDEN");
  }

  /** 当前角色权限。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public boolean has(String code) {
    return role().permissions.contains(code);
  }

  /** 服务点范围用于目录与选项过滤。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public boolean department(Long id) {
    return role().scope.equals("ALL") || Objects.equals(current().departmentId, id);
  }

  /** 服务点数据范围；失主仅查看本人记录。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public void library(Long departmentId, String code) {
    require(code);
    if (!department(departmentId)) throw new Problem(403, "OUT_OF_SCOPE");
  }

  /** 私有工作人员接口不接受本人范围角色；仍检查当前权限。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public void staff(String code) {
    require(code);
    if (role().scope.equals("ASSIGNED")) throw new Problem(403, "OUT_OF_SCOPE");
  }

  /** 失主仅可访问自己的报失，工作人员按服务点访问。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public boolean report(LostReport r) {
    return department(r.departmentId)
        && (Objects.equals(current().id, r.reporterId)
            || (has("lost_reports") && !role().scope.equals("ASSIGNED")));
  }

  /** 审计只保存身份、动作和对象ID。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public void audit(String action, Object id, Long dept) {
    var e = new AuditEvent();
    e.actor = current().username;
    e.action = action;
    e.objectId = String.valueOf(id);
    e.departmentId = dept;
    e.createdAt = clock.instant();
    db.save(e);
  }

  /** 当前菜单和安全身份，不返回密码散列。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> profile() {
    var a = current();
    var r = role();
    return Map.of(
        "id",
        a.id,
        "username",
        a.username,
        "displayName",
        a.displayName,
        "departmentId",
        a.departmentId,
        "role",
        r.name,
        "scope",
        r.scope,
        "permissions",
        r.permissions,
        "menus",
        db.all(NavMenu.class).stream()
            .filter(m -> m.enabled && r.permissions.contains(m.permissionCode))
            .sorted(Comparator.comparingInt(m -> m.position))
            .toList());
  }
}
