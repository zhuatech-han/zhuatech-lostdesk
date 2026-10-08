// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.lostdesk;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 真实MockMvc、JPA、Flyway、控制时钟；业务服务不被Mock替代。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@SpringBootTest
@AutoConfigureMockMvc(print = org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint.NONE)
@Import(CustodyIntegrationTest.TimeConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustodyIntegrationTest {
  static final String PASSWORD = "Aa9" + UUID.randomUUID();

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add(
        "spring.datasource.url",
        () -> "jdbc:h2:mem:lostdesk;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    r.add("spring.datasource.username", () -> "sa");
    r.add("spring.datasource.password", () -> "");
    r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
    r.add("lostdesk.admin-password", () -> PASSWORD);
  }

  /** 注入可移动时钟，只在专用测试内存在。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  static class TestClock extends Clock {
    volatile Instant now = Instant.parse("2026-10-08T08:00:00Z");

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId z) {
      return Clock.fixed(now, z);
    }

    public Instant instant() {
      return now;
    }
  }

  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    TestClock testClock() {
      return new TestClock();
    }
  }

  @Autowired MockMvc mvc;
  @Autowired Store db;
  @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager fixtureEm;
  @Autowired TransactionTemplate tx;
  @Autowired TestClock clock;
  final JsonMapper json = JsonMapper.builder().build();
  MockHttpSession admin, keeper, verifier, owner, second, outsider, viewer;
  long ownerId, secondId, keeperId, verifierId, outsiderId, otherSite;
  JsonNode item, report, claim;

  Map<String, Object> m(Object... a) {
    var out = new LinkedHashMap<String, Object>();
    for (int i = 0; i < a.length; i += 2) out.put(a[i].toString(), a[i + 1]);
    return out;
  }

  Map<String, Object> map(JsonNode n) {
    return json.convertValue(n, Map.class);
  }

  MockHttpSession login(String u) throws Exception {
    var r =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content(json.writeValueAsString(m("username", u, "password", PASSWORD))))
            .andReturn();
    assertEquals(200, r.getResponse().getStatus());
    return (MockHttpSession) r.getRequest().getSession(false);
  }

  MvcResult request(MockHttpSession s, String path, String method, Object b) throws Exception {
    MockHttpServletRequestBuilder r =
        switch (method) {
          case "GET" -> get("/api" + path);
          case "POST" -> post("/api" + path);
          case "PUT" -> put("/api" + path);
          case "DELETE" -> delete("/api" + path);
          default -> throw new IllegalArgumentException();
        };
    if (s != null) r.session(s);
    if (!method.equals("GET")) r.with(csrf());
    if (b != null) r.contentType("application/json").content(json.writeValueAsString(b));
    return mvc.perform(r).andReturn();
  }

  JsonNode call(MockHttpSession s, String p, String method, Object b) throws Exception {
    var r = request(s, p, method, b);
    assertEquals(200, r.getResponse().getStatus(), r.getResponse().getContentAsString());
    return json.readTree(r.getResponse().getContentAsString());
  }

  void fail(MockHttpSession s, String p, String method, Object b, int status, String code)
      throws Exception {
    var r = request(s, p, method, b);
    assertEquals(status, r.getResponse().getStatus());
    assertEquals(code, json.readTree(r.getResponse().getContentAsString()).get("code").asText());
  }

  long createUser(String u, int role, long site) throws Exception {
    return call(
            admin,
            "/admin/users",
            "POST",
            m(
                "username",
                u,
                "displayName",
                "TEST " + u,
                "roleId",
                role,
                "departmentId",
                site,
                "enabled",
                true,
                "password",
                PASSWORD))
        .get("id")
        .asLong();
  }

  @BeforeAll
  void identities() throws Exception {
    admin = login("admin");
    otherSite =
        call(
                admin,
                "/admin/departments",
                "POST",
                m("name", "TEST Other site", "zone", "UTC", "enabled", true))
            .get("id")
            .asLong();
    keeperId = createUser("test-keeper", 2, 1);
    verifierId = createUser("test-verifier", 3, 1);
    ownerId = createUser("test-owner", 4, 1);
    secondId = createUser("test-second", 4, 1);
    outsiderId = createUser("test-outsider", 2, otherSite);
    createUser("test-viewer", 5, 1);
  }

  @BeforeEach
  void clean() throws Exception {
    clock.now = Instant.parse("2026-10-08T08:00:00Z");
    tx.executeWithoutResult(
        s -> {
          for (String name :
              List.of(
                  "PhotoAsset",
                  "CustodyEvent",
                  "ClaimCase",
                  "DisposalCase",
                  "FoundItem",
                  "LostReport")) fixtureEm.createQuery("delete from " + name).executeUpdate();
          for (var a : db.all(Account.class)) {
            a.enabled = true;
            if (a.username.equals("test-owner")) a.roleId = 4L;
            if (a.username.equals("test-keeper")) a.roleId = 2L;
          }
          for (var p : db.all(SystemSetting.class)) {
            p.value =
                switch (p.code) {
                  case "retention_days" -> "30";
                  case "pickup_hours" -> "72";
                  default -> "10";
                };
          }
          for (var spot : db.all(StorageSpot.class)) spot.enabled = true;
        });
    admin = login("admin");
    keeper = login("test-keeper");
    verifier = login("test-verifier");
    owner = login("test-owner");
    second = login("test-second");
    outsider = login("test-outsider");
    viewer = login("test-viewer");
    item = newItem("TEST-" + UUID.randomUUID().toString().substring(0, 8));
    report = newReport(owner);
    claim = null;
  }

  JsonNode newItem(String code) throws Exception {
    return call(
        keeper,
        "/items",
        "POST",
        m(
            "departmentId",
            1,
            "storageId",
            1,
            "code",
            code,
            "title",
            "TEST 黑色雨伞",
            "category",
            "PERSONAL",
            "color",
            "black",
            "foundDate",
            "2026-10-07",
            "foundPlace",
            "TEST Hall",
            "privateMarks",
            "TEST 隐藏标记 PRIVATE-MARK-9",
            "note",
            "TEST first line\nsecond line"));
  }

  JsonNode newReport(MockHttpSession who) throws Exception {
    return call(
        who,
        "/lost-reports",
        "POST",
        m(
            "title",
            "TEST 丢失雨伞",
            "category",
            "PERSONAL",
            "color",
            "black",
            "lostDate",
            "2026-10-07",
            "lostPlace",
            "TEST Hall",
            "description",
            "TEST 蓝色线绑在伞柄上"));
  }

  JsonNode propose(JsonNode i, JsonNode r) throws Exception {
    return call(
        keeper,
        "/claims",
        "POST",
        m(
            "itemId",
            i.get("id").asLong(),
            "reportId",
            r.get("id").asLong(),
            "message",
            "TEST 发现一件可能符合的物品，请补充特征"));
  }

  JsonNode evidence(MockHttpSession who, JsonNode c) throws Exception {
    return call(
        who,
        "/claims/" + c.get("id").asLong() + "/evidence",
        "POST",
        m("version", c.get("version").asLong(), "evidence", "TEST 蓝线和柄内的专有标记，可在现场证明"));
  }

  JsonNode approve(JsonNode c) throws Exception {
    return call(
        verifier,
        "/claims/" + c.get("id").asLong() + "/review",
        "POST",
        m(
            "version",
            c.get("version").asLong(),
            "approve",
            true,
            "reviewNote",
            "TEST 现场核查隐藏标记，PRIVATE-REVIEW-3 一致",
            "message",
            "TEST 请到接待柜台领取"));
  }

  JsonNode ready() throws Exception {
    claim = approve(evidence(owner, propose(item, report)));
    return claim;
  }

  String cp(JsonNode c, String a) {
    return "/claims/" + c.get("id").asLong() + "/" + a;
  }

  @Test
  void fullClaimHandoverReceipt() throws Exception {
    var c = ready();
    assertEquals("READY", c.get("status").asText());
    c =
        call(
            keeper,
            cp(c, "handover"),
            "POST",
            m(
                "version",
                c.get("version").asLong(),
                "physicalConfirmed",
                true,
                "note",
                "TEST 已核对并交出实物"));
    assertEquals("DELIVERED", c.get("status").asText());
    c =
        call(
            owner,
            cp(c, "receipt"),
            "POST",
            m("version", c.get("version").asLong(), "physicalConfirmed", true, "note", "TEST 收到"));
    assertEquals("CLOSED", c.get("status").asText());
    assertEquals(
        "RETURNED",
        call(keeper, "/items/" + item.get("id").asLong(), "GET", null)
            .get("item")
            .get("status")
            .asText());
    assertEquals(
        "RESOLVED",
        call(owner, "/lost-reports/" + report.get("id").asLong(), "GET", null)
            .get("report")
            .get("status")
            .asText());
  }

  @Test
  void claimProjectionKeepsPrivateMarksAndNotes() throws Exception {
    var c = ready();
    var own = call(owner, "/claims/" + c.get("id").asLong(), "GET", null).toString();
    assertFalse(own.contains("PRIVATE-MARK-9"));
    assertFalse(own.contains("PRIVATE-REVIEW-3"));
    assertFalse(own.contains("itemMarks"));
    assertFalse(own.contains("itemId"));
    assertFalse(own.contains("reviewNote"));
    assertTrue(
        call(verifier, "/claims/" + c.get("id").asLong(), "GET", null)
            .toString()
            .contains("PRIVATE-MARK-9"));
  }

  @Test
  void otherClaimantCannotSeeReportsOrClaims() throws Exception {
    var c = propose(item, report);
    fail(second, "/lost-reports/" + report.get("id").asLong(), "GET", null, 403, "OUT_OF_SCOPE");
    fail(second, "/claims/" + c.get("id").asLong(), "GET", null, 403, "OUT_OF_SCOPE");
    assertEquals(0, call(second, "/claims", "GET", null).get("total").asInt());
    fail(owner, "/items", "GET", null, 403, "FORBIDDEN");
  }

  @Test
  void scopedCustodianCannotReadOtherSite() throws Exception {
    fail(outsider, "/items/" + item.get("id").asLong(), "GET", null, 403, "OUT_OF_SCOPE");
    fail(outsider, "/lost-reports/" + report.get("id").asLong(), "GET", null, 403, "OUT_OF_SCOPE");
    assertEquals(0, call(outsider, "/items", "GET", null).get("total").asInt());
  }

  @Test
  void anonymousAndCsrfGuards() throws Exception {
    fail(null, "/items", "GET", null, 401, "UNAUTHENTICATED");
    assertEquals(
        403,
        mvc.perform(
                post("/api/items").session(keeper).contentType("application/json").content("{}"))
            .andReturn()
            .getResponse()
            .getStatus());
  }

  @Test
  void forgedReporterAndSiteRejected() throws Exception {
    var b = map(report);
    b.put("reporterId", secondId);
    fail(owner, "/lost-reports", "POST", b, 403, "OUT_OF_SCOPE");
    b = map(report);
    b.put("departmentId", otherSite);
    fail(owner, "/lost-reports", "POST", b, 403, "OUT_OF_SCOPE");
  }

  @Test
  void selfReviewRejectedEvenWithAllPermissions() throws Exception {
    var c = evidence(owner, propose(item, report));
    tx.executeWithoutResult(s -> db.get(Account.class, keeperId).roleId = 1L);
    keeper = login("test-keeper");
    fail(
        keeper,
        cp(c, "review"),
        "POST",
        m(
            "version",
            c.get("version").asLong(),
            "approve",
            true,
            "reviewNote",
            "TEST verification note sufficiently long",
            "message",
            "TEST"),
        409,
        "SELF_REVIEW");
  }

  @Test
  void proofMustPrecedeReview() throws Exception {
    var c = propose(item, report);
    fail(
        verifier,
        cp(c, "review"),
        "POST",
        m(
            "version",
            c.get("version").asLong(),
            "approve",
            true,
            "reviewNote",
            "TEST sufficiently long review",
            "message",
            "TEST"),
        409,
        "CLAIM_STATE");
    fail(
        owner,
        cp(c, "evidence"),
        "POST",
        m("version", c.get("version").asLong(), "evidence", "short"),
        409,
        "EVIDENCE_REQUIRED");
  }

  @Test
  void duplicateProposalAndItemCodeBlocked() throws Exception {
    propose(item, report);
    fail(
        keeper,
        "/claims",
        "POST",
        m(
            "itemId",
            item.get("id").asLong(),
            "reportId",
            report.get("id").asLong(),
            "message",
            "TEST"),
        409,
        "PROPOSAL_DUPLICATE");
    fail(keeper, "/items", "POST", map(item), 409, "CODE_DUPLICATE");
  }

  @Test
  void approvalRejectsCompetingCandidates() throws Exception {
    var r2 = newReport(second);
    var c2 = evidence(second, propose(item, r2));
    var c1 = evidence(owner, propose(item, report));
    approve(c1);
    assertEquals(
        "REJECTED",
        call(second, "/claims/" + c2.get("id").asLong(), "GET", null)
            .get("claim")
            .get("status")
            .asText());
  }

  @Test
  void exactPickupExpiryBlocksAndExplicitCleanupReleases() throws Exception {
    var c = ready();
    clock.now = Instant.parse(c.get("pickupUntil").asText());
    assertTrue(
        call(owner, "/claims/" + c.get("id").asLong(), "GET", null)
            .get("claim")
            .get("pickupExpired")
            .asBoolean());
    assertEquals(
        "READY",
        call(keeper, "/claims/" + c.get("id").asLong(), "GET", null)
            .get("claim")
            .get("status")
            .asText());
    fail(
        keeper,
        cp(c, "handover"),
        "POST",
        m("version", c.get("version").asLong(), "physicalConfirmed", true, "note", "TEST"),
        409,
        "PICKUP_EXPIRED");
    assertEquals(1, call(keeper, "/claims/expire", "POST", m()).get("expired").asInt());
    assertEquals(
        "STORED",
        call(keeper, "/items/" + item.get("id").asLong(), "GET", null)
            .get("item")
            .get("status")
            .asText());
  }

  @Test
  void ownerCancellationReleasesReservedItem() throws Exception {
    var c = ready();
    call(
        owner,
        cp(c, "cancel"),
        "POST",
        m("version", c.get("version").asLong(), "note", "TEST 取消领取"));
    assertEquals(
        "OPEN",
        call(owner, "/lost-reports/" + report.get("id").asLong(), "GET", null)
            .get("report")
            .get("status")
            .asText());
  }

  @Test
  void deliveryCannotBeCancelledOrWithdrawn() throws Exception {
    var c = ready();
    c =
        call(
            keeper,
            cp(c, "handover"),
            "POST",
            m("version", c.get("version").asLong(), "physicalConfirmed", true, "note", "TEST 交出"));
    fail(
        owner,
        cp(c, "cancel"),
        "POST",
        m("version", c.get("version").asLong(), "note", "TEST"),
        409,
        "CLAIM_STATE");
    var r = call(owner, "/lost-reports/" + report.get("id").asLong(), "GET", null).get("report");
    fail(
        owner,
        "/lost-reports/" + r.get("id").asLong() + "/withdraw",
        "POST",
        m("version", r.get("version").asLong(), "note", "TEST"),
        409,
        "HANDOVER_COMPLETE");
  }

  @Test
  void physicalHandoverAndReceiptMustBeExplicit() throws Exception {
    var c = ready();
    fail(
        keeper,
        cp(c, "handover"),
        "POST",
        m("version", c.get("version").asLong(), "physicalConfirmed", false, "note", "TEST"),
        409,
        "PHYSICAL_CONFIRMATION");
    fail(
        owner,
        cp(c, "receipt"),
        "POST",
        m("version", c.get("version").asLong(), "physicalConfirmed", true, "note", "TEST"),
        409,
        "CLAIM_STATE");
  }

  @Test
  void reviewRejectionNeverReservesItem() throws Exception {
    var c = evidence(owner, propose(item, report));
    c =
        call(
            verifier,
            cp(c, "review"),
            "POST",
            m(
                "version",
                c.get("version").asLong(),
                "approve",
                false,
                "reviewNote",
                "TEST 特征不匹配，人工拒绝",
                "message",
                "TEST 尚无法确认该物品"));
    assertEquals("REJECTED", c.get("status").asText());
    assertEquals(
        "STORED",
        call(keeper, "/items/" + item.get("id").asLong(), "GET", null)
            .get("item")
            .get("status")
            .asText());
  }

  @Test
  void metadataFreezesAfterProposalButMoveRemainsAudited() throws Exception {
    propose(item, report);
    fail(keeper, "/items/" + item.get("id").asLong(), "PUT", map(item), 409, "ITEM_FROZEN");
    var x =
        call(
            keeper,
            "/items/" + item.get("id").asLong() + "/move",
            "POST",
            m("version", item.get("version").asLong(), "storageId", 1, "note", "TEST 更正柜位"));
    assertTrue(
        call(keeper, "/items/" + x.get("id").asLong(), "GET", null)
            .get("history")
            .toString()
            .contains("ITEM_MOVED"));
  }

  @Test
  void staleVersionsRejectLostUpdates() throws Exception {
    var b = map(item);
    b.put("title", "TEST changed");
    call(keeper, "/items/" + item.get("id").asLong(), "PUT", b);
    fail(keeper, "/items/" + item.get("id").asLong(), "PUT", b, 409, "VERSION_CONFLICT");
  }

  @Test
  void locationDeletionAndDisableProtected() throws Exception {
    var spot = call(keeper, "/spots", "GET", null).get(0);
    var b = map(spot);
    b.put("enabled", false);
    fail(keeper, "/spots/1", "PUT", b, 409, "LOCATION_BUSY");
    fail(keeper, "/spots/1?version=1", "DELETE", null, 409, "RECORD_REFERENCED");
    var unused =
        call(
            keeper,
            "/spots",
            "POST",
            m("departmentId", 1, "code", "TEST-UNUSED", "name", "TEST Unused", "enabled", true));
    call(keeper, "/spots/" + unused.get("id").asLong() + "?version=1", "DELETE", null);
  }

  @Test
  void reportWithdrawalClosesOwnCandidates() throws Exception {
    var c = propose(item, report);
    call(
        owner,
        "/lost-reports/" + report.get("id").asLong() + "/withdraw",
        "POST",
        m("version", report.get("version").asLong(), "note", "TEST 撤回"));
    assertEquals(
        "CANCELLED",
        call(owner, "/claims/" + c.get("id").asLong(), "GET", null)
            .get("claim")
            .get("status")
            .asText());
  }

  @Test
  void futureDatesAndInvalidCodesRejected() throws Exception {
    var b = map(item);
    b.put("foundDate", "2026-10-09");
    b.put("code", "NEW-DATE");
    fail(keeper, "/items", "POST", b, 400, "DATE_FUTURE");
    b.put("foundDate", "2026-10-07");
    b.put("code", "../x");
    fail(keeper, "/items", "POST", b, 400, "CODE_INVALID");
  }

  @Test
  void earlyAndPendingClaimDisposalBlocked() throws Exception {
    fail(
        keeper,
        "/disposals",
        "POST",
        m("itemId", item.get("id").asLong(), "version", 1, "method", "DONATE", "reason", "TEST"),
        409,
        "RETENTION_NOT_DUE");
    propose(item, report);
    clock.now = clock.now.plus(Duration.ofDays(31));
    fail(
        keeper,
        "/disposals",
        "POST",
        m("itemId", item.get("id").asLong(), "version", 1, "method", "DONATE", "reason", "TEST"),
        409,
        "ACTIVE_CLAIMS");
  }

  @Test
  void independentDisposalWorkflow() throws Exception {
    clock.now = clock.now.plus(Duration.ofDays(30));
    var d =
        call(
            keeper,
            "/disposals",
            "POST",
            m(
                "itemId",
                item.get("id").asLong(),
                "version",
                1,
                "method",
                "RECYCLE",
                "reason",
                "TEST 到期依机构规则申请"));
    assertEquals("PENDING", d.get("status").asText());
    d =
        call(
            verifier,
            "/disposals/" + d.get("id").asLong() + "/review",
            "POST",
            m("version", 1, "approve", true, "note", "TEST 独立复核完成"));
    d =
        call(
            keeper,
            "/disposals/" + d.get("id").asLong() + "/execute",
            "POST",
            m(
                "version",
                d.get("version").asLong(),
                "physicalConfirmed",
                true,
                "note",
                "TEST 记录现场处理结果"));
    assertEquals("EXECUTED", d.get("status").asText());
    assertEquals(
        "DISPOSED",
        call(keeper, "/items/" + item.get("id").asLong(), "GET", null)
            .get("item")
            .get("status")
            .asText());
  }

  @Test
  void retentionAndPickupPolicySnapshotsSurviveChange() throws Exception {
    tx.executeWithoutResult(
        s ->
            db.all(SystemSetting.class)
                .forEach(
                    x -> {
                      if (x.code.equals("retention_days")) x.value = "90";
                    }));
    assertEquals("2026-11-07", item.get("retainUntil").asText());
    var c = ready();
    var until = c.get("pickupUntil").asText();
    tx.executeWithoutResult(
        s ->
            db.all(SystemSetting.class)
                .forEach(
                    x -> {
                      if (x.code.equals("pickup_hours")) x.value = "1";
                    }));
    assertEquals(
        until,
        call(owner, "/claims/" + c.get("id").asLong(), "GET", null)
            .get("claim")
            .get("pickupUntil")
            .asText());
  }

  @Test
  void csvExportOmitsPrivateMarksAndOwnerData() throws Exception {
    ready();
    var r = request(keeper, "/reports/export", "GET", null);
    assertEquals(200, r.getResponse().getStatus());
    assertTrue(r.getResponse().getContentAsString().contains(item.get("code").asText()));
    assertFalse(r.getResponse().getContentAsString().contains("PRIVATE-MARK-9"));
    assertFalse(r.getResponse().getContentAsString().contains("蓝色线"));
    fail(owner, "/reports/export", "GET", null, 403, "FORBIDDEN");
    assertTrue(
        request(outsider, "/reports/export", "GET", null)
                .getResponse()
                .getContentAsString()
                .split("\r\n")
                .length
            <= 1);
  }

  @Test
  void lastAdminAndAccountDisableProtected() throws Exception {
    var a = call(admin, "/admin/users", "GET", null).get(0);
    var b = map(a);
    b.put("enabled", false);
    b.put("password", "");
    fail(admin, "/admin/users/1", "PUT", b, 409, "LAST_ADMIN");
    tx.executeWithoutResult(s -> db.get(Account.class, ownerId).enabled = false);
    fail(owner, "/auth/me", "GET", null, 401, "UNAUTHENTICATED");
  }

  @Test
  void reassignedSiteBlockedWhenHistoryExists() throws Exception {
    var a =
        call(admin, "/admin/users", "GET", null)
            .valueStream()
            .filter(x -> x.get("id").asLong() == ownerId)
            .findFirst()
            .orElseThrow();
    var b = map(a);
    b.put("departmentId", otherSite);
    b.put("password", "");
    fail(admin, "/admin/users/" + ownerId, "PUT", b, 409, "ACCOUNT_ASSIGNED");
  }

  @Test
  void revokedRequestPermissionStopsOwnDetailAccess() throws Exception {
    tx.executeWithoutResult(s -> db.get(Account.class, ownerId).roleId = 5L);
    fail(owner, "/lost-reports/" + report.get("id").asLong(), "GET", null, 403, "FORBIDDEN");
  }

  @Test
  void assignedCustomRoleCannotReadStaffInventory() throws Exception {
    tx.executeWithoutResult(
        s -> {
          var role = new AccessRole();
          role.name = "TEST Mixed own";
          role.scope = "ASSIGNED";
          role.permissions.addAll(Set.of("items", "request"));
          db.save(role);
          db.get(Account.class, ownerId).roleId = role.id;
        });
    fail(owner, "/items", "GET", null, 403, "OUT_OF_SCOPE");
  }

  @Test
  void privatePhotosRequireObjectOwnershipAndFreeze() throws Exception {
    var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var bytes = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, "png", bytes);
    var f = new MockMultipartFile("file", "../../unsafe.png", "image/png", bytes.toByteArray());
    var r =
        mvc.perform(
                multipart("/api/items/" + item.get("id").asLong() + "/photos")
                    .file(f)
                    .session(keeper)
                    .with(csrf()))
            .andReturn();
    assertEquals(200, r.getResponse().getStatus());
    var p = json.readTree(r.getResponse().getContentAsString());
    assertFalse(p.has("content"));
    assertFalse(r.getResponse().getContentAsString().contains("unsafe.png"));
    assertEquals(
        200,
        request(keeper, "/photos/" + p.get("id").asLong(), "GET", null).getResponse().getStatus());
    fail(owner, "/photos/" + p.get("id").asLong(), "GET", null, 403, "FORBIDDEN");
    propose(item, report);
    fail(
        keeper,
        "/photos/" + p.get("id").asLong() + "?version=1",
        "DELETE",
        null,
        409,
        "ITEM_FROZEN");
  }

  @Test
  void reportPhotosVisibleOnlyToOwnerOrScopedStaff() throws Exception {
    var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var bytes = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, "png", bytes);
    var r =
        mvc.perform(
                multipart("/api/lost-reports/" + report.get("id").asLong() + "/photos")
                    .file(
                        new MockMultipartFile("file", "TEST.png", "image/png", bytes.toByteArray()))
                    .session(owner)
                    .with(csrf()))
            .andReturn();
    assertEquals(200, r.getResponse().getStatus());
    long p = json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    assertEquals(200, request(owner, "/photos/" + p, "GET", null).getResponse().getStatus());
    fail(second, "/photos/" + p, "GET", null, 403, "OUT_OF_SCOPE");
    fail(outsider, "/photos/" + p, "GET", null, 403, "OUT_OF_SCOPE");
  }

  @Test
  void reportAndClaimSearchPagingStateGuards() throws Exception {
    propose(item, report);
    assertEquals(1, call(owner, "/claims?q=雨伞&size=1&page=1", "GET", null).get("items").size());
    assertEquals(0, call(owner, "/claims?state=CLOSED", "GET", null).get("total").asInt());
    fail(keeper, "/items?page=0", "GET", null, 400, "INVALID_INPUT");
    fail(keeper, "/items?sort=evil", "GET", null, 400, "INVALID_INPUT");
    assertTrue(call(owner, "/dashboard", "GET", null).has("openReports"));
  }

  @Test
  void claimantAndIndependentVerifierCannotPerformEachOthersActions() throws Exception {
    var c = ready();
    fail(
        verifier,
        cp(c, "handover"),
        "POST",
        m("version", c.get("version").asLong(), "physicalConfirmed", true, "note", "TEST"),
        403,
        "FORBIDDEN");
    fail(
        keeper,
        cp(c, "receipt"),
        "POST",
        m("version", c.get("version").asLong(), "physicalConfirmed", true, "note", "TEST"),
        403,
        "FORBIDDEN");
    fail(viewer, "/items", "GET", null, 403, "FORBIDDEN");
  }

  @Test
  void concurrentApprovalNeverAllocatesSameItemTwice() throws Exception {
    var c1 = evidence(owner, propose(item, report));
    var c2 = evidence(second, propose(item, newReport(second)));
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      var latch = new CountDownLatch(1);
      var tasks = new ArrayList<Future<Integer>>();
      for (var c : List.of(c1, c2))
        tasks.add(
            pool.submit(
                () -> {
                  latch.await();
                  return request(
                          verifier,
                          cp(c, "review"),
                          "POST",
                          m(
                              "version",
                              c.get("version").asLong(),
                              "approve",
                              true,
                              "reviewNote",
                              "TEST independently checked hidden proof",
                              "message",
                              "TEST ready"))
                      .getResponse()
                      .getStatus();
                }));
      latch.countDown();
      var states = new ArrayList<Integer>();
      for (var f : tasks) states.add(f.get(30, TimeUnit.SECONDS));
      Collections.sort(states);
      assertEquals(List.of(200, 409), states);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void historicalIntakeDatesAndServerEditFlags() throws Exception {
    var b = map(item);
    b.put("code", "TEST-HISTORICAL");
    b.put("foundDate", "2026-09-01");
    b.put("receivedDate", "2026-09-02");
    var x = call(keeper, "/items", "POST", b);
    assertEquals("2026-10-02", x.get("retainUntil").asText());
    var detail = call(keeper, "/items/" + x.get("id").asLong(), "GET", null);
    assertTrue(detail.get("canEdit").asBoolean());
    assertTrue(detail.get("retentionDue").asBoolean());
    var update = map(x);
    update.put("receivedDate", "2026-09-03");
    fail(keeper, "/items/" + x.get("id").asLong(), "PUT", update, 409, "IDENTITY_LOCKED");
  }

  @Test
  void invalidIntakeOrderAndFrozenEditFlags() throws Exception {
    var b = map(item);
    b.put("code", "TEST-DATE-ORDER");
    b.put("receivedDate", "2026-10-06");
    fail(keeper, "/items", "POST", b, 400, "DATE_ORDER");
    propose(item, report);
    assertFalse(
        call(keeper, "/items/" + item.get("id").asLong(), "GET", null).get("canEdit").asBoolean());
  }

  @Test
  void mineListsStayOwnForAdministrativeRoles() throws Exception {
    var r2 = newReport(second);
    var c1 = propose(item, report);
    propose(newItem("TEST-OTHER-ITEM"), r2);
    tx.executeWithoutResult(s -> db.get(Account.class, ownerId).roleId = 1L);
    assertEquals(2, call(owner, "/lost-reports", "GET", null).get("total").asInt());
    assertEquals(1, call(owner, "/lost-reports?mine=true", "GET", null).get("total").asInt());
    var own = call(owner, "/claims?mine=true", "GET", null);
    assertEquals(1, own.get("total").asInt());
    assertFalse(own.toString().contains("PRIVATE-MARK-9"));
    assertEquals(1, call(owner, "/dashboard?mine=true", "GET", null).get("openReports").asInt());
    assertFalse(call(owner, "/dashboard?mine=true", "GET", null).has("STORED"));
    assertFalse(
        call(owner, "/claims/" + c1.get("id").asLong() + "?mine=true", "GET", null)
            .get("claim")
            .has("itemMarks"));
  }

  @Test
  void mineDetailsRejectOthersEvenForAllScope() throws Exception {
    var r2 = newReport(second);
    var c2 = propose(item, r2);
    tx.executeWithoutResult(s -> db.get(Account.class, ownerId).roleId = 1L);
    fail(
        owner,
        "/lost-reports/" + r2.get("id").asLong() + "?mine=true",
        "GET",
        null,
        403,
        "OUT_OF_SCOPE");
    fail(
        owner, "/claims/" + c2.get("id").asLong() + "?mine=true", "GET", null, 403, "OUT_OF_SCOPE");
    assertNotNull(call(owner, "/lost-reports/" + r2.get("id").asLong(), "GET", null).get("report"));
  }
}
