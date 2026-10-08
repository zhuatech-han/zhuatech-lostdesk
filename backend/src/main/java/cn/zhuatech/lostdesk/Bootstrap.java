// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 首次空库初始化私有管理员、服务点、菜单和业务目录。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Component
public class Bootstrap implements ApplicationRunner {
  static final List<String> CODES =
      List.of(
          "items",
          "intake",
          "lost_reports",
          "propose",
          "review",
          "handover",
          "request",
          "dispose",
          "reports",
          "users",
          "roles",
          "settings",
          "audit");
  final Store db;
  final BCryptPasswordEncoder encoder;
  final String username, password;

  public Bootstrap(
      Store d,
      BCryptPasswordEncoder e,
      @Value("${lostdesk.admin-username}") String u,
      @Value("${lostdesk.admin-password}") String p) {
    db = d;
    encoder = e;
    username = u;
    password = p;
  }

  /** 已有数据库不重设身份或业务。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!db.all(Account.class).isEmpty()) return;
    AdminService.validatePassword(password);
    if (!username.matches("[a-zA-Z0-9_.-]{3,60}"))
      throw new IllegalStateException("Invalid administrator name");
    for (String code : CODES) {
      var p = new Permission();
      p.code = code;
      p.name = code;
      db.save(p);
    }
    var d = new Department();
    d.name = "主服务点 / Main service site";
    d.zone = "Asia/Shanghai";
    db.save(d);
    var arole = role("管理员 / Administrator", "ALL", CODES);
    role(
        "保管员 / Custodian",
        "DEPARTMENT",
        List.of("items", "intake", "lost_reports", "propose", "handover", "reports", "audit"));
    role(
        "核验员 / Verifier",
        "DEPARTMENT",
        List.of("items", "lost_reports", "review", "dispose", "reports", "audit"));
    role("失主 / Claimant", "ASSIGNED", List.of("request"));
    role("统计查阅 / Report viewer", "DEPARTMENT", List.of("reports"));
    var a = new Account();
    a.username = username.toLowerCase(Locale.ROOT);
    a.displayName = "管理员 / Administrator";
    a.passwordHash = encoder.encode(password);
    a.departmentId = d.id;
    a.roleId = arole.id;
    db.save(a);
    String[][] menus = {
      {"items", "保管物品", "Stored items", "items"},
      {"reportsdesk", "报失处理", "Lost reports", "lost_reports"},
      {"claims", "认领核验", "Claims", "lost_reports"},
      {"disposals", "到期处置", "Disposals", "items"},
      {"spots", "保管位置", "Storage locations", "intake"},
      {"my", "我的报失与认领", "My lost reports", "request"},
      {"dashboard", "统计台账", "Reports", "reports"},
      {"users", "登录账号", "Accounts", "users"},
      {"roles", "角色与权限", "Roles & permissions", "roles"},
      {"settings", "服务点与设置", "Sites & settings", "settings"},
      {"audit", "操作记录", "Audit trail", "audit"}
    };
    for (int i = 0; i < menus.length; i++) {
      var m = new NavMenu();
      m.code = menus[i][0];
      m.name = menus[i][1];
      m.nameEn = menus[i][2];
      m.permissionCode = menus[i][3];
      m.position = i;
      m.enabled = true;
      db.save(m);
    }
    String[][] cats = {
      {"PERSONAL", "日常用品", "Personal items"},
      {"CLOTHING", "衣物", "Clothing"},
      {"ELECTRONICS", "电子设备", "Electronics"},
      {"OTHER", "其他", "Other"}
    };
    for (var row : cats) {
      var x = new DictionaryEntry();
      x.type = "CATEGORY";
      x.code = row[0];
      x.name = row[1];
      x.nameEn = row[2];
      db.save(x);
    }
    for (var row :
        new String[][] {
          {"retention_days", "30"}, {"pickup_hours", "72"}, {"max_open_reports", "10"}
        }) {
      var x = new SystemSetting();
      x.code = row[0];
      x.value = row[1];
      db.save(x);
    }
    var spot = new StorageSpot();
    spot.departmentId = d.id;
    spot.code = "DESK-01";
    spot.name = "接待保管点 / Service desk";
    db.save(spot);
  }

  private AccessRole role(String name, String scope, List<String> codes) {
    var r = new AccessRole();
    r.name = name;
    r.scope = scope;
    r.permissions.addAll(codes);
    return db.save(r);
  }
}
