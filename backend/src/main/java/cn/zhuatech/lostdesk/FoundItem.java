// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import jakarta.persistence.*;
import java.time.*;

/** 实际收存物品与隐藏识别特征；只向工作人员返回。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "found_item")
public class FoundItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long departmentId;
  public Long storageId;
  public String code;
  public String title;
  public String category;
  public String color;
  public LocalDate foundDate;
  public String foundPlace;
  public String privateMarks;
  public String note;
  public LocalDate receivedDate;
  public LocalDate retainUntil;
  public int retentionDays;
  public String status = "STORED";
  public Long createdBy;
  public Instant createdAt;
  public long version = 1;
}
