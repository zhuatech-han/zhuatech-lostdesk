// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import jakarta.persistence.*;
import java.time.*;

/** 认领特征核验、限时保留和双边收讫；保存提出时特征快照。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "claim_case")
public class ClaimCase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long departmentId;
  public Long itemId;
  public Long reportId;
  public Long claimantId;
  public Long proposedBy;
  public Long reviewedBy;
  public Long handedBy;
  public String status = "PROPOSED";
  public String message;
  public String itemTitle;
  public String itemMarks;
  public String reportTitle;
  public String reportDescription;
  public String evidence = "";
  public String reviewNote = "";
  public String handoverNote = "";
  public String receiptNote = "";
  public Instant createdAt;
  public Instant submittedAt;
  public Instant reviewedAt;
  public Instant pickupUntil;
  public Instant handedAt;
  public Instant receivedAt;
  public Instant closedAt;
  public long version = 1;
}
