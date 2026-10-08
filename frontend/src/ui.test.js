// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
import test from "node:test";
import assert from "node:assert/strict";
import { reactive } from "vue";
import {
  language,
  t,
  stateName,
  errorMessage,
  cloneRecord,
  ask,
  answerConfirmation,
  confirmation,
} from "./ui.js";
test("language changes visible statuses and errors", () => {
  language.value = "en";
  assert.equal(stateName("PROPOSED"), "Evidence needed");
  assert.match(errorMessage(new Error("EVIDENCE_REQUIRED")), /evidence/);
  language.value = "zh";
  assert.equal(t("保管", "Stored"), "保管");
});
test("unknown errors retain diagnostic code", () =>
  assert.match(errorMessage(new Error("UNKNOWN")), /UNKNOWN/));
test("reactive editing cannot mutate source list", () => {
  const row = reactive({ id: 1, permissions: ["items"] });
  const copy = cloneRecord(row);
  copy.permissions.push("users");
  assert.deepEqual(row.permissions, ["items"]);
});
test("cancel and confirm are distinct promises", async () => {
  const pending = ask("TEST");
  assert.ok(confirmation.value);
  assert.equal(await ask("second"), false);
  answerConfirmation(false);
  assert.equal(await pending, false);
  const next = ask("next");
  answerConfirmation(true);
  assert.equal(await next, true);
});
