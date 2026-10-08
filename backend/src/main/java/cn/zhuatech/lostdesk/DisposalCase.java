// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import jakarta.persistence.*;
import java.time.*;

/** 到期处置申请、独立批准与实际执行；不是自动处置授权。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "disposal_case")
public class DisposalCase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long departmentId;
  public Long itemId;
  public Long requestedBy;
  public Long reviewedBy;
  public Long executedBy;
  public String method;
  public String reason;
  public String status = "PENDING";
  public String reviewNote = "";
  public String executionNote = "";
  public Instant createdAt;
  public Instant reviewedAt;
  public Instant executedAt;
  public long version = 1;
}
