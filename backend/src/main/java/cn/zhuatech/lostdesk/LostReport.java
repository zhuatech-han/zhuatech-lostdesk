// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import jakarta.persistence.*;
import java.time.*;

/** 失主报失；个人记录不可被其他失主查询。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "lost_report")
public class LostReport {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long departmentId;
  public Long reporterId;
  public String title;
  public String category;
  public String color;
  public LocalDate lostDate;
  public String lostPlace;
  public String description;
  public String status = "OPEN";
  public Instant createdAt;
  public long version = 1;
}
