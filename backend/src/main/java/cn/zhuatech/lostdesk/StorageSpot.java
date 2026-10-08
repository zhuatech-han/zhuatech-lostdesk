// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import jakarta.persistence.*;
import java.time.*;

/** 保管位置目录；停用不删除历史。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "storage_spot")
public class StorageSpot {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long departmentId;
  public String code;
  public String name;
  public boolean enabled = true;
  public long version = 1;
}
