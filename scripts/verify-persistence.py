#!/usr/bin/env python3
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""重启或独立恢复后核对实物记录、认领、私有照片和CSV。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
import argparse
import json
import os
from pathlib import Path
from urllib.parse import urlparse

from quality import ROOT, Session, check, snapshot


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--base", required=True)
    p.add_argument("--state", default=str(ROOT / "output/quality-state.json"))
    p.add_argument("--refresh-snapshot", action="store_true")
    a = p.parse_args()
    if urlparse(a.base).hostname not in ["127.0.0.1", "localhost", "::1"]:
        p.error("Only disposable localhost instances")
    path = Path(a.state)
    state = json.loads(path.read_text())
    s = Session(a.base)
    s.login("test-keeper", state["passwords"]["test-keeper"])
    actual = snapshot(s)
    if a.refresh_snapshot:
        state["snapshot"] = actual
        path.write_text(json.dumps(state, ensure_ascii=False, indent=2))
        os.chmod(path, 0o600)
        print("Private post-UI snapshot refreshed; no credentials printed.")
        return
    expected = state["snapshot"]
    count = 0
    check(set(actual["items"]) == set(expected["items"]), "Same item IDs")
    count += 1
    for key, data in actual["items"].items():
        for field in ["item", "photos", "history", "canEdit", "retentionDue"]:
            check(
                data[field] == expected["items"][key][field], f"Persisted item {field}"
            )
            count += len(data[field]) if isinstance(data[field], list) else 1
    for name in ["reports", "claims", "disposals", "spots", "photoHashes", "csvSha"]:
        check(actual[name] == expected[name], f"Persisted {name}")
        count += len(actual[name]) if isinstance(actual[name], (list, dict)) else 1
    audit = {x["id"]: x for x in actual["audit"]}
    for old in expected["audit"]:
        check(audit.get(old["id"]) == old, "Immutable older audit")
        count += 1
    print(
        f"PASS: {count} persisted record/collection/hash checks after restart or restore."
    )


if __name__ == "__main__":
    main()
