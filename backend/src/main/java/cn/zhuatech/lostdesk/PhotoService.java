// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import java.io.*;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** 图片尺寸、签名和权限核验；PNG重新编码去除原元数据，内容存MySQL。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Service
@Transactional
public class PhotoService {
  final Store db;
  final AccessService access;
  final AdminService admin;
  final CustodyService business;
  final Clock clock;

  public PhotoService(Store d, AccessService a, AdminService m, CustodyService b, Clock c) {
    db = d;
    access = a;
    admin = m;
    business = b;
    clock = c;
  }

  /** 上传对象只允许物品或本人报失，忽略客户端文件名。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public PhotoAsset upload(String kind, Long id, MultipartFile file) throws Exception {
    admin.lock();
    boolean item = kind.equals("items");
    if (!item && !kind.equals("lost-reports")) throw new Problem(404, "NOT_FOUND");
    if (item) {
      var x = db.get(FoundItem.class, id);
      business.site(x.departmentId, "intake");
      Rules.check(
          x.status.equals("STORED")
              && db.query(ClaimCase.class, "from ClaimCase where itemId=?1", id).isEmpty(),
          "ITEM_FROZEN");
    } else {
      var r = db.get(LostReport.class, id);
      business.requester(r);
      Rules.check(
          r.status.equals("OPEN")
              && db.query(ClaimCase.class, "from ClaimCase where reportId=?1", id).isEmpty(),
          "REPORT_BUSY");
    }
    Rules.check(business.photos(item ? id : null, item ? null : id).size() < 4, "PHOTO_LIMIT");
    byte[] bytes = normalize(file.getBytes(), file.getContentType());
    var p = new PhotoAsset();
    p.itemId = item ? id : null;
    p.reportId = item ? null : id;
    p.createdBy = access.current().id;
    p.createdAt = clock.instant();
    p.content = bytes;
    p.sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    db.save(p);
    access.audit(
        "PHOTO_UPLOAD",
        p.id,
        item
            ? db.get(FoundItem.class, id).departmentId
            : db.get(LostReport.class, id).departmentId);
    return p;
  }

  /** 内容检查在解码前核对尺寸，不以扩展名或浏览器声明为准。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static byte[] normalize(byte[] data, String mime) throws IOException {
    if (data.length == 0 || data.length > 2 * 1024 * 1024) throw new Problem(413, "FILE_TOO_LARGE");
    boolean png =
        data.length >= 8
            && data[0] == (byte) 137
            && data[1] == 80
            && data[2] == 78
            && data[3] == 71;
    boolean jpeg =
        data.length >= 3 && data[0] == (byte) 255 && data[1] == (byte) 216 && data[2] == (byte) 255;
    if (!(png && "image/png".equals(mime)) && !(jpeg && "image/jpeg".equals(mime)))
      throw new Problem(400, "PHOTO_INVALID");
    try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
      var readers = ImageIO.getImageReaders(in);
      if (!readers.hasNext()) throw new Problem(400, "PHOTO_INVALID");
      var reader = readers.next();
      try {
        reader.setInput(in, true, true);
        int w = reader.getWidth(0), h = reader.getHeight(0);
        if (w < 1 || h < 1 || w > 2500 || h > 2500 || (long) w * h > 3000000)
          throw new Problem(400, "PHOTO_DIMENSIONS");
        var image = reader.read(0);
        if (image == null) throw new Problem(400, "PHOTO_INVALID");
        var out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        byte[] normalized = out.toByteArray();
        if (normalized.length > 4 * 1024 * 1024) throw new Problem(413, "FILE_TOO_LARGE");
        return normalized;
      } finally {
        reader.dispose();
      }
    } catch (Problem p) {
      throw p;
    } catch (Exception e) {
      throw new Problem(400, "PHOTO_INVALID");
    }
  }

  /** 每次图片下载重新核对账号、当前角色和对象范围。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public PhotoAsset get(Long id) {
    var p = db.get(PhotoAsset.class, id);
    if (!p.active) throw new Problem(404, "NOT_FOUND");
    if (p.itemId != null) business.site(db.get(FoundItem.class, p.itemId).departmentId, "items");
    else business.ownReport(db.get(LostReport.class, p.reportId));
    return p;
  }

  /** 未进入认领核验的照片可软删除；已用证据不破坏历史。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Object remove(Long id, long version) {
    admin.lock();
    var p = get(id);
    Rules.check(p.version == version, "VERSION_CONFLICT");
    if (p.itemId != null) {
      var x = db.get(FoundItem.class, p.itemId);
      business.site(x.departmentId, "intake");
      Rules.check(
          x.status.equals("STORED")
              && db.query(ClaimCase.class, "from ClaimCase where itemId=?1", x.id).isEmpty(),
          "ITEM_FROZEN");
      access.audit("PHOTO_REMOVED", id, x.departmentId);
    } else {
      var r = db.get(LostReport.class, p.reportId);
      business.requester(r);
      Rules.check(
          r.status.equals("OPEN")
              && db.query(ClaimCase.class, "from ClaimCase where reportId=?1", r.id).isEmpty(),
          "REPORT_BUSY");
      access.audit("PHOTO_REMOVED", id, r.departmentId);
    }
    p.active = false;
    p.version++;
    return Map.of("ok", true);
  }
}
