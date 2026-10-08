// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import java.time.*;
import java.util.*;

/** 标识符、到期和分页规则；到期只是软件办理门禁。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
public final class CustodyPolicy {
  /** 稳定保管编号，不从照片或个人信息生成。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static String code(Object value) {
    String s = Rules.text(value, 40, true).toUpperCase(Locale.ROOT);
    if (!s.matches("[A-Z0-9][A-Z0-9_.-]{2,39}")) throw new Problem(400, "CODE_INVALID");
    return s;
  }

  /** 领取截止等于当前时刻也已过期。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static boolean expired(Instant until, Instant now) {
    return until != null && !now.isBefore(until);
  }

  /** 保管到期可申请独立处置，不自动销毁物品。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static boolean due(LocalDate until, LocalDate today) {
    return !today.isBefore(until);
  }

  /** 页码和数量界限。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static void page(int page, int size) {
    Rules.integer(page, 1, 100000);
    Rules.integer(size, 1, 100);
  }

  private CustodyPolicy() {}
}
