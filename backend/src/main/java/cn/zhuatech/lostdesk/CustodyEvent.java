// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import jakarta.persistence.*;
import java.time.*;

/** 不可覆盖的保管与认领动作历史。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "custody_event")
public class CustodyEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long departmentId;
  public Long itemId;
  public Long reportId;
  public Long claimId;
  public Long disposalId;
  public Long actorId;
  public String action;
  public String note;
  public Instant createdAt;
}
