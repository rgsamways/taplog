## 1. Seed script

- [x] 1.1 Create `scripts/seed_anchor_vertical.py` — loads `MONGODB_URL` and `DB_NAME` from environment (python-dotenv fallback), connects via pymongo, upserts the full Anchor `VerticalConfig` document on `{"vertical": "ANCHOR"}`
- [x] 1.2 Populate the six asset type entries (HAR, LAN, SRL, ANC, HLL, RKT) with codes, labels, descriptions, `trigger_config`, and `checklist_items` per the context file
- [x] 1.3 Set HAR, LAN, SRL trigger_config to `{"type": "PRE_USE", "interval_months": null}`; set ANC, HLL, RKT to `{"type": "CALENDAR", "interval_months": 12}`
- [x] 1.4 Set result options to PASS (action NONE) and REMOVE_FROM_SERVICE (action REMOVE_FROM_SERVICE) only — no middle option
- [x] 1.5 Set form fields: `condition_notes` (TEXT, required false) and `last_cert_date` (DATE, required false)
- [x] 1.6 Print `"Anchor VerticalConfig upserted."` on success; print error and exit 1 on failure

## 2. Deploy and verify

- [x] 2.1 Run `python scripts/seed_anchor_vertical.py` against the Railway/Atlas environment
- [x] 2.2 Confirm `GET /api/v1/verticals/ANCHOR` returns HTTP 200 with full document
- [x] 2.3 Confirm `GET /api/v1/verticals` returns both EMBER and ANCHOR for a test org with both in `licensed_verticals` (can patch one via Atlas UI temporarily to verify)
- [x] 2.4 Run seed script a second time — confirm exactly one ANCHOR document remains (idempotency check)

## 3. Android verification (observational only — no code changes)

- [x] 3.1 Clear app storage on test device, cold-start the app with connectivity — confirm `vertical_configs` table in DB Inspector shows a row with `vertical_code = "ANCHOR"` after startup
- [x] 3.2 Confirm `GET /api/v1/verticals` log in Railway shows two configs returned when the org's `licensed_verticals` includes ANCHOR
