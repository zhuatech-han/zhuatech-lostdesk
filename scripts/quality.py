#!/usr/bin/env python3
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""只在专用本机空库验证真实报失认领，凭证和快照保持私有。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
import argparse
import hashlib
import io
import json
import os
import secrets
from pathlib import Path
from urllib.parse import urlparse

import requests

ROOT = Path(__file__).resolve().parents[1]
COUNT = 0


def check(ok, label):
    """每条检查都有明确业务或安全含义。知华科技 https://www.zhuatech.cn/。"""
    global COUNT
    if not ok:
        raise AssertionError(label)
    COUNT += 1


class Session:
    """真实同源会话；读取CSRF、不自动重放写入。知华科技 https://www.zhuatech.cn/。"""

    def __init__(self, base):
        self.base = base
        self.session = requests.Session()

    def call(
        self, method, path, body=None, status=200, code=None, raw=False, files=None
    ):
        headers = {}
        if method not in ["GET", "HEAD"]:
            csrf = self.session.get(self.base + "/api/auth/csrf", timeout=20)
            check(csrf.status_code == 200, "CSRF bootstrap")
            data = csrf.json()
            headers[data["header"]] = data["token"]
        response = self.session.request(
            method,
            self.base + "/api" + path,
            json=body if files is None else None,
            files=files,
            headers=headers,
            timeout=30,
        )
        check(
            response.status_code == status,
            f"{method} {path}: expected {status}, got {response.status_code}; {response.text[:120] if response.status_code != status else ''}",
        )
        if code:
            check(
                response.json().get("code") == code,
                "Expected business error "
                + code
                + "; received "
                + str(response.json().get("code")),
            )
        return response if raw else response.json()

    def login(self, user, password):
        return self.call(
            "POST", "/auth/login", {"username": user, "password": password}
        )


def snapshot(s):
    """保管对象、历史、照片内容和CSV逐字节快照。知华科技 https://www.zhuatech.cn/。"""
    ids = [x["id"] for x in s.call("GET", "/items?size=100")["items"]]
    items = {str(x): s.call("GET", f"/items/{x}") for x in ids}
    photos = {}
    for item in items.values():
        for p in item["photos"]:
            r = s.call("GET", f"/photos/{p['id']}", raw=True)
            photos[str(p["id"])] = hashlib.sha256(r.content).hexdigest()
    return {
        "items": items,
        "reports": s.call("GET", "/lost-reports?size=100")["items"],
        "claims": s.call("GET", "/claims?size=100")["items"],
        "disposals": s.call("GET", "/disposals?size=100")["items"],
        "spots": s.call("GET", "/spots"),
        "photoHashes": photos,
        "csvSha": hashlib.sha256(
            s.call("GET", "/reports/export", raw=True).content
        ).hexdigest(),
        "audit": s.call("GET", "/audit"),
    }


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--base", default="http://127.0.0.1:8128")
    p.add_argument("--env-file", default=str(ROOT / ".env"))
    p.add_argument("--state", default=str(ROOT / "output/quality-state.json"))
    a = p.parse_args()
    if urlparse(a.base).hostname not in ["127.0.0.1", "localhost", "::1"]:
        p.error("Only disposable localhost instances")
    env = dict(
        line.split("=", 1)
        for line in Path(a.env_file).read_text().splitlines()
        if "=" in line and not line.startswith("#")
    )
    admin = Session(a.base)
    check(
        requests.get(a.base + "/actuator/health", timeout=20).json()["status"] == "UP",
        "Health",
    )
    admin.call("GET", "/items", status=401, code="UNAUTHENTICATED")
    check(
        requests.post(a.base + "/api/items", json={}, timeout=20).status_code == 403,
        "Anonymous CSRF protection",
    )
    profile = admin.login(env["ADMIN_USERNAME"], env["ADMIN_PASSWORD"])
    check(
        len(profile["menus"]) == 11 and len(profile["permissions"]) == 13,
        "Enabled bootstrap menus and business roles",
    )
    check(
        admin.call("GET", "/items")["total"] == 0,
        "Refuse QA unless found inventory is empty",
    )
    check(
        admin.call("GET", "/lost-reports")["total"] == 0,
        "Refuse QA unless lost reports are empty",
    )
    users = admin.call("GET", "/admin/users")
    check(
        len(users) == 1 and "passwordHash" not in users[0], "Private administrator only"
    )
    admin.call(
        "PUT",
        "/admin/users/1",
        {**users[0], "enabled": False, "password": ""},
        status=409,
        code="LAST_ADMIN",
    )
    site2 = admin.call(
        "POST",
        "/admin/departments",
        {"name": "TEST Other site", "zone": "UTC", "enabled": True},
    )
    passwords, accounts, sessions = {}, {}, {}
    specs = [
        ("test-keeper", 2, 1),
        ("test-verifier", 3, 1),
        ("test-owner", 4, 1),
        ("test-second", 4, 1),
        ("test-outsider", 2, site2["id"]),
        ("test-viewer", 5, 1),
    ]
    for user, role, site in specs:
        pw = "Aa9" + secrets.token_hex(16)
        passwords[user] = pw
        accounts[user] = admin.call(
            "POST",
            "/admin/users",
            {
                "username": user,
                "displayName": "TEST "
                + {
                    "test-keeper": "保管员",
                    "test-verifier": "核验员",
                    "test-owner": "失主一",
                    "test-second": "失主二",
                    "test-outsider": "另一服务点保管员",
                    "test-viewer": "台账查看员",
                }[user],
                "roleId": role,
                "departmentId": site,
                "enabled": True,
                "password": pw,
            },
        )
        s = Session(a.base)
        s.login(user, pw)
        sessions[user] = s
    target = Path(a.state)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(
        json.dumps(
            {
                "status": "in-progress",
                "base": a.base,
                "passwords": passwords,
                "accounts": accounts,
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    os.chmod(target, 0o600)
    k, v, o, second, outside, viewer = [sessions[user] for user, _, _ in specs]
    owner_id = accounts["test-owner"]["id"]
    today = (
        __import__("datetime")
        .datetime.now(__import__("zoneinfo").ZoneInfo("Asia/Shanghai"))
        .date()
        .isoformat()
    )
    item_body = {
        "departmentId": 1,
        "storageId": 1,
        "title": "TEST 黑色折叠雨伞",
        "category": "PERSONAL",
        "color": "黑色",
        "foundDate": today,
        "foundPlace": "TEST 一楼接待区",
        "privateMarks": "TEST 隐藏特征 PRIVATE-CUSTODY-9：柄内有蓝色线",
        "note": "TEST 保管登记\n第二行说明",
    }
    items = []
    for code, title in [
        ("LF-001", "TEST 黑色折叠雨伞"),
        ("LF-002", "TEST 运动外套"),
        ("LF-003", "TEST 充电器"),
        ("LF-004", "TEST 帆布袋"),
        ("LF-005", "TEST 水杯"),
    ]:
        items.append(
            k.call("POST", "/items", {**item_body, "code": code, "title": title})
        )
    check(
        items[0]["retentionDays"] == 30 and "\n" in items[0]["note"],
        "Retention snapshot and multiline persistence",
    )
    k.call(
        "POST",
        "/items",
        {**item_body, "code": "LF-001"},
        status=409,
        code="CODE_DUPLICATE",
    )
    k.call(
        "POST",
        "/items",
        {**item_body, "code": "../invalid"},
        status=400,
        code="CODE_INVALID",
    )
    k.call("GET", "/items?page=0", status=400, code="INVALID_INPUT")
    k.call("GET", "/items?sort=evil", status=400, code="INVALID_INPUT")
    check(
        k.call("GET", "/items?q=雨伞&size=1")["total"] == 1,
        "Catalog search and pagination",
    )
    check(outside.call("GET", "/items")["total"] == 0, "Other site inventory excluded")
    outside.call("GET", f"/items/{items[0]['id']}", status=403, code="OUT_OF_SCOPE")
    o.call("GET", "/items", status=403, code="FORBIDDEN")
    viewer.call("GET", "/items", status=403, code="FORBIDDEN")
    # A small real PNG fixture generated by a standard byte-level encoder, not a customer photo.
    import struct, zlib

    chunk = (
        lambda tag, data: struct.pack(">I", len(data))
        + tag
        + data
        + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    )
    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", 4, 4, 8, 2, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress((b"\x00" + bytes([40, 70, 110]) * 4) * 4))
        + chunk(b"IEND", b"")
    )
    photo = k.call(
        "POST",
        f"/items/{items[0]['id']}/photos",
        files={"file": ("../../TEST.png", png, "image/png")},
    )
    check(
        "content" not in photo and "filename" not in photo,
        "Photo content and original path are not in metadata",
    )
    image = k.call("GET", f"/photos/{photo['id']}", raw=True)
    check(
        image.content.startswith(b"\x89PNG")
        and image.headers["Content-Type"].startswith("image/png"),
        "Actual normalized PNG bytes",
    )
    check(
        "no-store" in image.headers.get("Cache-Control", ""),
        "Private photos do not use shared cache",
    )
    o.call("GET", f"/photos/{photo['id']}", status=403, code="FORBIDDEN")
    outside.call("GET", f"/photos/{photo['id']}", status=403, code="OUT_OF_SCOPE")
    k.call(
        "POST",
        f"/items/{items[1]['id']}/photos",
        files={"file": ("TEST.png", b"<script>x</script>", "image/png")},
        status=400,
        code="PHOTO_INVALID",
    )
    report_body = {
        "title": "TEST 丢失的黑色雨伞",
        "category": "PERSONAL",
        "color": "黑色",
        "lostDate": today,
        "lostPlace": "TEST 一楼接待区",
        "description": "TEST 私有描述：伞柄内有蓝色线，侧面两处缝线",
    }
    r1 = o.call("POST", "/lost-reports", report_body)
    r2 = second.call(
        "POST", "/lost-reports", {**report_body, "title": "TEST 第二位失主的雨伞"}
    )
    r3 = o.call("POST", "/lost-reports", {**report_body, "title": "TEST 丢失充电器"})
    second.call("GET", f"/lost-reports/{r1['id']}", status=403, code="OUT_OF_SCOPE")
    outside.call("GET", f"/lost-reports/{r1['id']}", status=403, code="OUT_OF_SCOPE")
    o.call(
        "POST",
        "/lost-reports",
        {**report_body, "reporterId": accounts["test-second"]["id"]},
        status=403,
        code="OUT_OF_SCOPE",
    )
    o.call(
        "POST",
        "/lost-reports",
        {**report_body, "departmentId": site2["id"]},
        status=403,
        code="OUT_OF_SCOPE",
    )
    check(
        o.call("GET", "/lost-reports")["total"] == 2
        and second.call("GET", "/lost-reports")["total"] == 1,
        "Own report counts isolated",
    )
    check(
        len(k.call("GET", f"/lost-reports/{r1['id']}/candidates")) == 5,
        "Staff manual candidates use same site and category",
    )
    o.call("GET", f"/lost-reports/{r1['id']}/candidates", status=403, code="FORBIDDEN")

    def propose(item, report):
        return k.call(
            "POST",
            "/claims",
            {
                "itemId": item["id"],
                "reportId": report["id"],
                "message": "TEST 找到一件可能符合的物品，请补充认领特征",
            },
        )

    def evidence(s, c):
        return s.call(
            "POST",
            f"/claims/{c['id']}/evidence",
            {
                "version": c["version"],
                "evidence": "TEST 伞柄内蓝色线和侧面两处独有缝线，可以现场确认",
            },
        )

    def approve(c):
        return v.call(
            "POST",
            f"/claims/{c['id']}/review",
            {
                "version": c["version"],
                "approve": True,
                "reviewNote": "TEST 独立核对隐藏特征 PRIVATE-REVIEW-3 后通过",
                "message": "TEST 请在期限内到服务点柜台领取",
            },
        )

    c2 = evidence(second, propose(items[0], r2))
    c1 = propose(items[0], r1)
    k.call(
        "POST",
        "/claims",
        {"itemId": items[0]["id"], "reportId": r1["id"], "message": "TEST"},
        status=409,
        code="PROPOSAL_DUPLICATE",
    )
    v.call(
        "POST",
        f"/claims/{c1['id']}/review",
        {
            "version": c1["version"],
            "approve": True,
            "reviewNote": "TEST review before owner evidence",
            "message": "TEST",
        },
        status=409,
        code="CLAIM_STATE",
    )
    o.call(
        "POST",
        f"/claims/{c1['id']}/evidence",
        {"version": c1["version"], "evidence": "short"},
        status=409,
        code="EVIDENCE_REQUIRED",
    )
    c1 = evidence(o, c1)
    k.call(
        "POST",
        f"/claims/{c1['id']}/review",
        {
            "version": c1["version"],
            "approve": True,
            "reviewNote": "TEST enough evidence",
            "message": "TEST",
        },
        status=403,
        code="FORBIDDEN",
    )
    c1 = approve(c1)
    check(c1["status"] == "READY", "Independent verification reserves item")
    check(
        second.call("GET", f"/claims/{c2['id']}")["claim"]["status"] == "REJECTED",
        "Competing claim rejected atomically",
    )
    own = o.call("GET", f"/claims/{c1['id']}")
    check(
        not any(
            key in own["claim"]
            for key in [
                "itemMarks",
                "itemId",
                "reviewNote",
                "claimantId",
                "proposedBy",
                "handedBy",
            ]
        ),
        "Claimant projection excludes private fields",
    )
    check(
        "PRIVATE-CUSTODY-9" not in json.dumps(own)
        and "PRIVATE-REVIEW-3" not in json.dumps(own),
        "Claimant detail and history do not leak hidden evidence",
    )
    second.call("GET", f"/claims/{c1['id']}", status=403, code="OUT_OF_SCOPE")
    k.call("DELETE", f"/photos/{photo['id']}?version=1", status=409, code="ITEM_FROZEN")
    k.call(
        "PUT",
        f"/items/{items[0]['id']}",
        k.call("GET", f"/items/{items[0]['id']}")["item"],
        status=409,
        code="ITEM_NOT_STORED",
    )
    k.call(
        "POST",
        f"/claims/{c1['id']}/handover",
        {"version": c1["version"], "physicalConfirmed": False, "note": "TEST"},
        status=409,
        code="PHYSICAL_CONFIRMATION",
    )
    o.call(
        "POST",
        f"/claims/{c1['id']}/receipt",
        {"version": c1["version"], "physicalConfirmed": True, "note": "TEST"},
        status=409,
        code="CLAIM_STATE",
    )
    c1 = k.call(
        "POST",
        f"/claims/{c1['id']}/handover",
        {
            "version": c1["version"],
            "physicalConfirmed": True,
            "note": "TEST 已核对失主和实物，现场交出",
        },
    )
    check(
        c1["status"] == "DELIVERED",
        "Custodian handover does not forge claimant receipt",
    )
    o.call(
        "POST",
        f"/claims/{c1['id']}/cancel",
        {"version": c1["version"], "note": "TEST"},
        status=409,
        code="CLAIM_STATE",
    )
    k.call(
        "POST",
        f"/claims/{c1['id']}/receipt",
        {"version": c1["version"], "physicalConfirmed": True, "note": "TEST"},
        status=403,
        code="FORBIDDEN",
    )
    own_after = o.call("GET", f"/claims/{c1['id']}")["claim"]
    c1 = o.call(
        "POST",
        f"/claims/{c1['id']}/receipt",
        {
            "version": own_after["version"],
            "physicalConfirmed": True,
            "note": "TEST 本人已收到物品",
        },
    )
    check(c1["status"] == "CLOSED", "Claimant receipt closes claim")
    check(
        k.call("GET", f"/items/{items[0]['id']}")["item"]["status"] == "RETURNED",
        "Physical return state persisted",
    )
    check(
        o.call("GET", f"/lost-reports/{r1['id']}")["report"]["status"] == "RESOLVED",
        "Report resolved only after receipt",
    )
    c3 = approve(evidence(o, propose(items[2], r3)))
    c3 = o.call(
        "POST",
        f"/claims/{c3['id']}/cancel",
        {"version": c3["version"], "note": "TEST 暂不领取，取消本次候选"},
    )
    check(
        c3["status"] == "CANCELLED"
        and k.call("GET", f"/items/{items[2]['id']}")["item"]["status"] == "STORED",
        "Cancellation restores reserved stock",
    )
    c4 = propose(items[2], r3)
    o.call(
        "POST",
        f"/lost-reports/{r3['id']}/withdraw",
        {
            "version": o.call("GET", f"/lost-reports/{r3['id']}")["report"]["version"],
            "note": "TEST 撤回未领取报失",
        },
    )
    check(
        o.call("GET", f"/claims/{c4['id']}")["claim"]["status"] == "CANCELLED",
        "Report withdrawal closes active candidates",
    )
    k.call(
        "POST",
        "/disposals",
        {
            "itemId": items[1]["id"],
            "version": 1,
            "method": "RECYCLE",
            "reason": "TEST early",
        },
        status=409,
        code="RETENTION_NOT_DUE",
    )
    spot = k.call("GET", "/spots")[0]
    k.call(
        "PUT", "/spots/1", {**spot, "enabled": False}, status=409, code="LOCATION_BUSY"
    )
    k.call("DELETE", "/spots/1?version=1", status=409, code="RECORD_REFERENCED")
    extra_spot = k.call(
        "POST",
        "/spots",
        {
            "departmentId": 1,
            "code": "TEST-A02",
            "name": "TEST 二号保管柜",
            "enabled": True,
        },
    )
    unused_spot = k.call(
        "POST",
        "/spots",
        {
            "departmentId": 1,
            "code": "TEST-UNUSED",
            "name": "TEST 可删除位置",
            "enabled": True,
        },
    )
    k.call("DELETE", f"/spots/{unused_spot['id']}?version=1")
    moved = k.call(
        "POST",
        f"/items/{items[1]['id']}/move",
        {
            "version": items[1]["version"],
            "storageId": extra_spot["id"],
            "note": "TEST 实际转移到二号保管柜",
        },
    )
    check(moved["storageId"] == extra_spot["id"], "Storage move persisted")
    k.call(
        "POST",
        f"/items/{items[1]['id']}/move",
        {"version": items[1]["version"], "storageId": 1, "note": "TEST"},
        status=409,
        code="VERSION_CONFLICT",
    )
    check(
        k.call("POST", "/claims/expire")["expired"] == 0,
        "Expiry cleanup has no premature side effect",
    )
    historic_date = (
        __import__("datetime").date.fromisoformat(today)
        - __import__("datetime").timedelta(days=40)
    ).isoformat()
    old = k.call(
        "POST",
        "/items",
        {
            **item_body,
            "code": "LF-OLD-01",
            "title": "TEST 到期处置演练物品",
            "foundDate": historic_date,
            "receivedDate": historic_date,
        },
    )
    check(
        k.call("GET", f"/items/{old['id']}")["retentionDue"],
        "Historical intake reaches actual retention deadline",
    )
    d = k.call(
        "POST",
        "/disposals",
        {
            "itemId": old["id"],
            "version": old["version"],
            "method": "RECYCLE",
            "reason": "TEST 根据机构处理规则申请到期回收",
        },
    )
    check(
        k.call("GET", f"/items/{old['id']}")["item"]["status"] == "DISPOSAL_PENDING",
        "Disposal request stops claim allocation",
    )
    d = admin.call(
        "POST",
        f"/disposals/{d['id']}/review",
        {
            "version": d["version"],
            "approve": True,
            "note": "TEST 独立复核期满且无有效认领",
        },
    )
    admin.call(
        "POST",
        f"/disposals/{d['id']}/execute",
        {
            "version": d["version"],
            "physicalConfirmed": True,
            "note": "TEST same reviewer",
        },
        status=409,
        code="SELF_REVIEW",
    )
    k.call(
        "POST",
        f"/disposals/{d['id']}/execute",
        {"version": d["version"], "physicalConfirmed": False, "note": "TEST"},
        status=409,
        code="PHYSICAL_CONFIRMATION",
    )
    d = k.call(
        "POST",
        f"/disposals/{d['id']}/execute",
        {
            "version": d["version"],
            "physicalConfirmed": True,
            "note": "TEST 实物处理完成，登记内部凭据编号 TEST-R01",
        },
    )
    check(
        d["status"] == "EXECUTED"
        and k.call("GET", f"/items/{old['id']}")["item"]["status"] == "DISPOSED",
        "Independent disposal executes only after physical confirmation",
    )
    old2 = k.call(
        "POST",
        "/items",
        {
            **item_body,
            "code": "LF-OLD-02",
            "title": "TEST 待复核到期物品",
            "foundDate": historic_date,
            "receivedDate": historic_date,
        },
    )
    d2 = k.call(
        "POST",
        "/disposals",
        {
            "itemId": old2["id"],
            "version": old2["version"],
            "method": "TRANSFER",
            "reason": "TEST 待确认移交依据",
        },
    )
    d2 = v.call(
        "POST",
        f"/disposals/{d2['id']}/review",
        {
            "version": d2["version"],
            "approve": False,
            "note": "TEST 移交依据尚不完整，拒绝",
        },
    )
    check(
        k.call("GET", f"/items/{old2['id']}")["item"]["status"] == "STORED",
        "Rejected disposal preserves stock",
    )
    old2 = k.call("GET", f"/items/{old2['id']}")["item"]
    d2 = k.call(
        "POST",
        "/disposals",
        {
            "itemId": old2["id"],
            "version": old2["version"],
            "method": "TRANSFER",
            "reason": "TEST 完善移交说明后重新申请",
        },
    )
    d2 = v.call(
        "POST",
        f"/disposals/{d2['id']}/review",
        {"version": d2["version"], "approve": True, "note": "TEST 独立批准"},
    )
    d2 = k.call(
        "POST",
        f"/disposals/{d2['id']}/cancel",
        {"version": d2["version"], "note": "TEST 尚未实际执行，撤销本次申请"},
    )
    check(d2["status"] == "CANCELLED", "Disposal cancelled before execution")
    old2 = k.call("GET", f"/items/{old2['id']}")["item"]
    k.call(
        "POST",
        "/disposals",
        {
            "itemId": old2["id"],
            "version": old2["version"],
            "method": "RECYCLE",
            "reason": "TEST 新方式待独立复核",
        },
    )
    check(
        viewer.call("GET", "/dashboard")["RETURNED"] == 1,
        "Read-only metrics use actual stock",
    )
    o.call("GET", "/admin/users", status=403, code="FORBIDDEN")
    outside.call("GET", "/admin/users", status=403, code="FORBIDDEN")
    export = k.call("GET", "/reports/export", raw=True)
    check(
        b"PRIVATE-CUSTODY-9" not in export.content
        and b"PRIVATE-REVIEW-3" not in export.content,
        "CSV excludes identifying proof",
    )
    check(export.content.decode().count("\r\n") == 8, "Real CSV rows")
    o.call("GET", "/reports/export", status=403, code="FORBIDDEN")
    user = accounts["test-second"]
    updated = admin.call(
        "PUT", f"/admin/users/{user['id']}", {**user, "enabled": False, "password": ""}
    )
    second.call("GET", "/auth/me", status=401, code="UNAUTHENTICATED")
    admin.call(
        "PUT",
        f"/admin/users/{user['id']}",
        {**updated, "enabled": True, "password": ""},
    )
    admin.call(
        "PUT",
        f"/admin/users/{owner_id}",
        {**accounts["test-owner"], "departmentId": site2["id"], "password": ""},
        status=409,
        code="ACCOUNT_ASSIGNED",
    )
    state = {
        "base": a.base,
        "passwords": passwords,
        "accounts": accounts,
        "snapshot": snapshot(k),
        "checks": COUNT,
    }
    target = Path(a.state)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(state, ensure_ascii=False, indent=2))
    os.chmod(target, 0o600)
    print(
        f"PASS: {COUNT} real MySQL HTTP assertions. Private TEST credentials/snapshots saved; no secrets printed."
    )


if __name__ == "__main__":
    main()
