[中文](README.md) | [English](README.en.md)

# LostDesk lost property custody and claims · Java 21 / Spring Boot / Vue 3

**ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)** · [Official website](https://www.zhuatech.cn/)

LostDesk supports property/customer-service teams at campuses, workplaces, malls and venues. It manages physical intake, own lost reports, manual candidates, identifying-evidence review, physical handover and claimant receipt. Staff-only inventory is separate from claimant records; complete custody photos and hidden marks are not exposed for public claiming.

**Public source learning edition / non-commercial source edition.** Personal learning and exchange only. Commercial use requires written authorization from Shanghai Rujing Zhihua Information Technology Co., Ltd. Public source is not an MIT/Apache free-commercial license. [LICENSE](LICENSE) governs use.

## Workflow

1. Custodians register actual items, unique codes, category, found date/place, storage location, hidden marks and private PNG/JPEG photos.
2. Claimants sign in, submit private lost reports and see only their own records. They do not browse the custody inventory.
3. Staff filter manual candidates by site/category/date and propose a claim. The claimant provides identifying evidence; another verifier records private notes and a separate claimant-facing message.
4. Approval reserves the item. A custodian physically hands it over, then the claimant personally confirms receipt to close the claim/report. Proposers cannot self-review; reviewers cannot hand over the same item.
5. Retention-due items with no active claims may enter independent disposal review and recorded physical execution.

Manual filtering is not image recognition or ownership determination. Receipt records are not electronic signatures or legal identity certification. Retention dates are workflow gates; organizations configure their applicable policies.

## Implemented roles and functions

| Portal / responsibility | Current capabilities |
| --- | --- |
| Custodian | Intake, unique codes, categories/storage, correction before reference, audited moves, unused-entry voiding, proposals, handover, explicit pickup expiry, disposal request/execution |
| Verifier | Private identifying-evidence review, independent approval/rejection, independent disposal review |
| Claimant | Own lost reports/private photos, edit/withdraw before workflow, evidence, deadlines, cancel undelivered candidates, own receipt |
| Administration | Accounts/roles/permissions/menus/sites, categories, timezones/policies, password reset, enable/disable, final administrator protection |
| Reports and audit | Scoped actual metrics, inventory CSV, search/state/paging/sort, custody events and account audit |

MySQL persists records, history and private photos. The server enforces session authentication, CSRF, role/site/own-record scope, versions and state. Account/site disable or password reset revokes old sessions. Claimants do not receive hidden marks, inventory photos, other claimant data or private review notes.

Historic received dates may be entered, found dates cannot follow receipt, and receipt dates are immutable. Retention deadlines use policy at registration. Defaults: 30 retention days, 72 pickup hours and ten open reports per claimant. Existing records retain deadline snapshots. GET does not mutate state. Custodians explicitly process expiry; there is no scheduler or notification service.

## Actual running pages

Screenshots contain real TEST acceptance records, not customer data or customer cases. Fresh installations do not seed these items, lost reports or test accounts.

### Sign-in

Private sign-in without shared demo credentials.

![Sign-in](docs/screenshots/01-login.jpg)

### Private inventory

Custody codes, categories, storage locations and actual item state.

![Private inventory](docs/screenshots/02-items.jpg)

### Item details

Staff-only identifying details, custody photos and history.

![Item details](docs/screenshots/03-item.jpg)

### Lost reports

Staff review actual lost reports within site scope.

![Lost reports](docs/screenshots/04-reports.jpg)

### Claim proposal

Proposals snapshot information without automatically establishing ownership.

![Claim proposal](docs/screenshots/05-proposal.jpg)

### Independent verification

Hidden marks, claimant evidence and independent review notes.

![Independent verification](docs/screenshots/06-review.jpg)

### Physical handover

Custodian handover followed by the claimant’s own receipt.

![Physical handover](docs/screenshots/07-handover.jpg)

### Claimant portal

Own lost reports, claims, pickup deadlines and receipt state.

![Claimant portal](docs/screenshots/08-claimant.jpg)

### Storage locations

Storage directory with occupied/history protection.

![Storage locations](docs/screenshots/09-locations.jpg)

### Accounts

Administrators maintain accounts, roles, sites and enable/disable.

![Accounts](docs/screenshots/10-accounts.jpg)

### Roles and permissions

Custody/review/own-record permissions and site scope.

![Roles and permissions](docs/screenshots/11-roles.jpg)

### Sites and policies

Categories, timezones, new-intake retention and pickup windows.

![Sites and policies](docs/screenshots/12-settings.jpg)

### Metrics and inventory CSV

Actual scoped metrics and CSV without hidden evidence.

![Metrics and inventory CSV](docs/screenshots/13-reports.jpg)

### Mobile claimant portal

Responsive own-record workflow; wide tables scroll in their container.

![Mobile claimant portal](docs/screenshots/14-mobile.jpg)

### English interface

Interface language changes without rewriting business data.

![English interface](docs/screenshots/15-english.jpg)

### Retention disposal

Due-item requests, independent review, execution and cancellation records.

![Retention disposal](docs/screenshots/16-disposals.jpg)

## Architecture and directories

Java 21, Spring Boot 4.0.7, Spring Security, JPA/Hibernate and Flyway; Vue 3.5.43, Vite 8.1.5 and Lucide; MySQL 8.4, Nginx and Docker Compose. Same-origin /api; private photo endpoints authenticate object scope and use no-store. Single-instance writes serialize with a database lock to prevent duplicate allocation/handover. BCrypt 12 rounds, HttpOnly/SameSite sessions and CSRF operate independently.

```text
backend/src/main/java/cn/zhuatech/lostdesk/ # identity, custody, claims, photos
backend/src/main/resources/db/migration/  # V1 identity / V2 custody
backend/src/test/java/                    # real HTTP, boundary and concurrent tests
frontend/src/                            # staff and claimant Vue UI
frontend/public/brand/                   # original logo/contact assets
docs/                                   # operating/deployment/API/license notes
scripts/                                # private QA, backup/restore and release checks
compose.yaml                            # MySQL, backend and same-origin Nginx
.env.example                            # configuration names, no credentials
```

## Requirements and startup

Docker Engine/Desktop and Compose V2 with access to official Maven/npm/Docker registries. At least 4 GB available development memory is recommended. Source development needs JDK21, Maven3.9+, Node.js24.19.0+, npm and Python3.10+. Compose includes MySQL; no existing host database is required.

```sh
python3 scripts/init-env.py
docker compose -p lostdesk config --quiet
docker compose -p lostdesk up -d --build --wait --wait-timeout 240
```

Open [http://127.0.0.1:8128/](http://127.0.0.1:8128/) and [health](http://127.0.0.1:8128/actuator/health). Empty-database username: `admin`; read `ADMIN_PASSWORD` from private .env. Initialization generates random passwords, writes mode0600 and refuses overwrite. Changing environment variables does not reset existing database users. Never upload .env.

First create separate custodians, verifiers and claimant accounts, configure actual sites/storage, then register found property and submit own lost reports. See [user guide](docs/USER_GUIDE.md). No anonymous reports or self-signup; the organization operates account provisioning.

## Database and configuration

V1__identity.sql creates nine identity/directory/policy/audit tables; V2__custody.sql creates seven storage/item/report/claim/disposal/event/photo tables. There are 16 application tables plus Flyway history. Migrations apply by version and Hibernate validates without rewriting data. Empty initialization creates one administrator/site, five roles, thirteen permissions, eleven menus, four categories, three policies and an editable default storage directory entry. It creates no business/customer data.

| Variable | Purpose / default |
| --- | --- |
| MYSQL_ROOT_PASSWORD | Required, unique database root password |
| DATABASE_PASSWORD | Required, unique application database password |
| ADMIN_USERNAME / ADMIN_PASSWORD | Empty-database bootstrap only; `admin` / required private random password, 12–72 bytes with upper/lowercase and digit |
| WEB_PORT / BIND_ADDRESS | `8128` / `127.0.0.1` |
| COOKIE_SECURE | `false` for localhost HTTP; `true` behind HTTPS |
| DATABASE_URL / DATABASE_USER | Optional external MySQL; bundled user `lostdesk` |

See [.env.example](.env.example). External MySQL requires least-privilege access, VERIFY_IDENTITY and a trusted CA; backup supports only the bundled database. Override conflicting ports with WEB_PORT=18128; never stop unrelated projects. Keep the database volume for routine shutdown. Back up before upgrades, preserve applied checksums and add migrations.

## Tests and deployment

```sh
mvn -B -f backend/pom.xml spotless:check test package
cd frontend
npm ci --no-audit --no-fund
npm run format:check
npm run lint
npm test
npm run build
cd ..
python3 scripts/release-check.py
git diff --check
```

The backend has 37 real MockMvc/JPA/Flyway integration tests and 17 business/input/photo boundary tests. Frontend: eleven request/error/language/form-isolation tests. Integration tests use isolated H2 MySQL mode; full deployment is separately accepted with a fresh MySQL8.4 volume. Backend Docker packaging runs all tests.

Only run the following in a disposable empty localhost instance: it creates TEST accounts/records and rejects non-empty directories. Credentials/snapshots remain in ignored private output/.

```sh
python3 -m venv .venv
.venv/bin/pip install -r scripts/requirements-quality.txt
.venv/bin/black --check scripts
.venv/bin/python scripts/quality.py --base http://127.0.0.1:8128
# Restart this dedicated test instance before persistence verification.
.venv/bin/python scripts/verify-persistence.py --base http://127.0.0.1:8128
.venv/bin/python scripts/backup.py --project lostdesk --output private-backups/lostdesk.zip
# Create private .env.restore with another port, e.g. 28128.
.venv/bin/python scripts/restore.py private-backups/lostdesk.zip --project lostdesk-restore --env-file .env.restore
.venv/bin/python scripts/verify-persistence.py --base http://127.0.0.1:28128
```

Acceptance covers report→proposal→owner evidence→independent approval→physical handover→receipt, duplicate/concurrent claims, scope/field privacy, photo access/freeze, locations/CSV/history and restart/isolated restore hashes. Controlled clocks cover exact pickup/retention boundaries.

Internet deployment additionally needs HTTPS, domain/certificate, secure passwords, account policies, backup and monitoring. See [deployment](docs/DEPLOYMENT.md) and [API/scope](docs/API.md). Local Compose acceptance is not a public-domain deployment or production-capacity guarantee.

## Limits, troubleshooting and feedback

- Single-instance learning edition, 10,000-row query limit per entity; some lists/metrics are in memory. No large-venue capacity or multi-node load tests.
- No public inventory, anonymous reports/self-signup, image recognition/similarity matching, scheduled expiry, email/SMS/WeChat notifications, courier/payment, SSO or legal identity certification integration. Basic workflow needs no paid external service.
- No post-handover dispute investigation, physical damage compensation or automatic restocking. Organizations handle physical exceptions. Records do not replace actual verification/custody or applicable policies.
- Photos are reencoded as PNG without original file/metadata. Database contains photos/private descriptions; organizations must operate access/backup/retention policies. No compliance certification claim.
- Timestamp display follows browser timezone; business dates use site timezone and pickup deadlines use absolute time. Deadline snapshots remain unchanged by later policy changes.
- For startup issues inspect Compose logs, database health and required settings. Do not bypass migration checks. Missing menus: check account/role/enabled entries. 403: permission/site/separation; 409: refresh current version/state; unknown network results: check before resubmitting.

Use repository Issues with redacted steps/version/error codes. Test contributions and keep customer data/photos/passwords/API keys out of commits. Report security vulnerabilities privately through the emails below, not with sensitive public issues. [Third-party notices](docs/THIRD_PARTY.md) retain upstream licenses. Software is provided as-is under the LICENSE disclaimer.

## License and contact

ZhiHua Technology provides this public source learning edition for personal study, technical research and non-commercial exchange. Commercial use requires written authorization from Shanghai Rujing Zhihua Information Technology Co., Ltd. This is not an OSI open-source license; upstream licenses are not replaced. [LICENSE](LICENSE) is consistent with this notice.

**ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)** · [Official website](https://www.zhuatech.cn/)

For commercial source licensing, private deployment, data migration, account integration, customization/system integration or private security reports:

- Email: [han@zhuatech.cn](mailto:han@zhuatech.cn)
- Email: [jack@zhuatech.cn](mailto:jack@zhuatech.cn)
- WhatsApp: [+86 17521234993](https://wa.me/8617521234993)
