// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.*;

/** 私有保管/报失照片，仅授权端点返回经过重新编码的PNG。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "photo_asset")
public class PhotoAsset {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long itemId;
  public Long reportId;
  public Long createdBy;
  public Instant createdAt;
  public String sha256;
  public boolean active = true;
  public long version = 1;

  @Lob
  @JsonIgnore
  @Column(columnDefinition = "LONGBLOB")
  public byte[] content;
}
