// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.time.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** 真实日期、输入、CSV和图片边界，不模拟业务实现。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
class CustodyPolicyTest {
  @Test
  void codesNormalize() {
    assertEquals("CASE-001", CustodyPolicy.code("case-001"));
  }

  @Test
  void pathLikeCodesRejected() {
    assertThrows(Problem.class, () -> CustodyPolicy.code("../x"));
  }

  @Test
  void pickupExactDeadlineExpired() {
    var n = Instant.parse("2026-10-08T08:00:00Z");
    assertTrue(CustodyPolicy.expired(n, n));
    assertFalse(CustodyPolicy.expired(n, n.minusSeconds(1)));
  }

  @Test
  void nullDeadlineNotExpired() {
    assertFalse(CustodyPolicy.expired(null, Instant.now()));
  }

  @Test
  void exactRetentionDateDue() {
    var d = LocalDate.parse("2026-11-07");
    assertTrue(CustodyPolicy.due(d, d));
    assertFalse(CustodyPolicy.due(d, d.minusDays(1)));
  }

  @Test
  void csvFormulaProtected() {
    assertEquals("\"'=1+1\"", Rules.csv("=1+1"));
    assertTrue(Rules.csv(" \t@cmd").startsWith("\"'"));
  }

  @Test
  void csvEscapesQuotesAndLines() {
    assertEquals("\"a\"\"b\nnext\"", Rules.csv("a\"b\nnext"));
  }

  @Test
  void integersNoSilentRounding() {
    assertThrows(Problem.class, () -> Rules.integer("1.5", 1, 10));
  }

  @Test
  void booleansStrict() {
    assertThrows(Problem.class, () -> Rules.flag("true"));
  }

  @Test
  void paragraphsPreserveLegitimateNewlines() {
    assertEquals("first\nsecond", Rules.paragraph("first\r\nsecond", 50, true));
  }

  @Test
  void hiddenNulNeverAccepted() {
    assertThrows(Problem.class, () -> Rules.paragraph("\u0000text", 50, true));
    assertThrows(Problem.class, () -> Rules.text("text\u0000", 50, true));
  }

  @Test
  void fakeImageMimeRejected() {
    assertThrows(
        Problem.class, () -> PhotoService.normalize("<script>x</script>".getBytes(), "image/png"));
  }

  @Test
  void validImageReencoded() throws Exception {
    var i = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    var out = new ByteArrayOutputStream();
    ImageIO.write(i, "png", out);
    assertNotNull(
        ImageIO.read(
            new ByteArrayInputStream(PhotoService.normalize(out.toByteArray(), "image/png"))));
  }

  @Test
  void oversizedImageDataRejected() {
    assertThrows(
        Problem.class, () -> PhotoService.normalize(new byte[2 * 1024 * 1024 + 1], "image/png"));
  }

  @Test
  void tooWideImageRejected() throws Exception {
    var i = new BufferedImage(2501, 1, BufferedImage.TYPE_INT_RGB);
    var out = new ByteArrayOutputStream();
    ImageIO.write(i, "png", out);
    assertThrows(Problem.class, () -> PhotoService.normalize(out.toByteArray(), "image/png"));
  }

  @Test
  void invalidPageRejected() {
    assertThrows(Problem.class, () -> CustodyPolicy.page(0, 10));
    assertThrows(Problem.class, () -> CustodyPolicy.page(1, 101));
  }

  @Test
  void mandatoryWhitespaceRejected() {
    assertThrows(Problem.class, () -> Rules.text("   ", 30, true));
    assertThrows(Problem.class, () -> Rules.paragraph(" \n\t ", 30, true));
  }
}
