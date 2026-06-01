# TapLog — Lightbulb Moments
> Insights captured during the build process. These are the "why didn't anyone think of this" observations that come from actually doing the work — not theorizing about it.
>
> Each entry records what triggered the insight, what the insight was, and what we did about it.
> These are worth more than the code. Anyone can write a tutorial. These are the moments that turn a tutorial into a playbook.

---

## LB-001 — The picker needs search
**Module:** 26 — OFC asset type picker
**Trigger:** After building a two-step category/type picker with 35 asset types across 6 categories, the question arose: what does a new inspector do when they don't know what category their asset falls under?
**Insight:** A new tradesperson standing in front of an unfamiliar piece of equipment doesn't think in OFC categories — they think in plain language. "That red cylinder on the wall." Forcing them to know the category first creates a barrier exactly where the app should be removing one. Search collapses the two-step picker into a single intent: just type what you see.
**What we did:** Added a search field to `AssetTypePickerDialog` that filters across all categories simultaneously. Category chips hide while searching; result count shown. Zero external dependencies.
**What's next:** Search by plain language is step one. The longer-term version is photo-based identification — inspector photographs the asset, AI suggests the type. This becomes natural once photo capture is added in Module 29. Flag this as a named feature: **Visual Asset Identification**.

---

## LB-002 — Inspection interval belongs on the asset type, not the asset
**Module:** 26 — OFC asset type picker
**Trigger:** Designing where to store `nextInspectionDue` — on the asset record, calculated manually, or derived from the asset type.
**Insight:** The OFC mandates inspection intervals by asset type, not by asset. A wet chemical extinguisher is always semi-annual. An emergency lighting unit is always 6 months. Letting an inspector set their own interval per asset is a compliance liability — they might get it wrong. The correct place to encode this knowledge is in the asset type definition itself, so the app enforces the right cadence automatically.
**What we did:** Added `inspectionIntervalMonths` to `OFCAssetType`. `nextInspectionDue` is auto-calculated from `installDate + interval` when the asset is registered. The inspector never touches it.
**Broader principle:** Regulatory knowledge belongs in data, not in UI. When the OFC changes an interval, you update one line in `OFCAssetTypes.kt` — not a form field somewhere.

---

## LB-003 — Back navigation reveals your mental model of the app
**Module:** 26 polish / navigation
**Trigger:** After building the asset detail screen, there was no way to get back to the asset list — only back to idle.
**Insight:** Navigation paths reveal how you think users move through the app. A single "Back → Home" model assumes every session starts with a scan. But an inspector doing a building walkthrough is more likely to browse the asset list and tap into individual assets one by one. That's a fundamentally different flow — and it needs Back to go to the list, not home. The source of navigation (scan vs. list) determines the correct back destination.
**What we did:** Added `AssetDetailSource` enum (`FROM_SCAN` / `FROM_LIST`) tracked in the ViewModel. Back button label and destination adapt accordingly: "Asset List" or "Home".
**Broader principle:** Navigation is UX documentation. Where Back goes tells the user what the app thinks they were doing.

---

## LB-004 — Specs in the repo solve the "did you read it" problem
**Module:** 26b — OpenSpec setup
**Trigger:** After creating `taplog-md-prime-directives.md`, the question arose: how do you enforce that an AI reads the directives before doing anything? Uploading a file relies on the human remembering to do it and the AI choosing to prioritize it.
**Insight:** The problem isn't the content of the directives — it's the delivery mechanism. A file uploaded to a chat session is ephemeral and unenforceable. A file that lives in the repository and is read automatically by the AI tool at session start is structural. You can't forget to upload what's already there.
**What we did:** Installed OpenSpec, which creates `.claude/AGENTS.md` — a file Claude Code reads automatically before every session. Copied prime directives there. Established end-of-session sync: `copy taplog-md-prime-directives.md .claude\AGENTS.md`.
**Broader principle:** Enforcement through structure beats enforcement through reminders. If you find yourself reminding an AI (or a person) to do something every session, the right fix is to make it impossible to skip — not to add it to a checklist.

---

## LB-005 — Asset identity is not tag identity
**Module:** Strategy session (Session 7)
**Trigger:** Discussing tag economics and what happens when a tag is damaged, lost, or painted over. The question arose: how does an inspector reconnect a replacement tag to an asset without losing its inspection history?
**Insight:** The current data model treats `Asset.nfcTagId` as a permanent identity — but it isn't. The tag is hardware. Hardware fails, gets damaged, gets replaced. If the asset's identity is the tag, every replacement starts a new history. That's catastrophic for a compliance tool. The asset is the permanent record. The tag is a replaceable pointer to it. All inspections, deficiencies, and scan events must attach to the asset UUID — never to the tag UID directly.
**What we did:** Designed a `TagEvent` entity — insert-only, records the full lifecycle of every tag attached to an asset (attachedAt, retiredAt, retiredReason: DAMAGED/LOST/REPLACED/REMOVED, retiredByInspectorId). Added a tag replacement workflow to AssetDetailScreen. Updated sync order to include tag_events. Flagged `Asset.nfcTagId` explicitly as a current pointer, not permanent identity.
**What's next:** Build tag lifecycle as part of Module 27 alongside the tamper-evident scan log. The two belong together — both are insert-only audit trail entities, both prove what happened at a physical asset over time.
**Broader principle:** In any system that bridges physical hardware and digital records, the physical object and the digital identity must be explicitly decoupled. Hardware fails. Identity persists. Get this wrong at the schema level and you pay a painful migration cost later. Validated by firsthand experience with Toyota and FileTrack asset management implementations.

---

## LB-006 — The inspection form is a moment of professional uncertainty
**Module:** Strategy session (Session 7)
**Trigger:** Discussing how difficult it is for tradespeople to access, parse, and retain Ontario regulatory requirements. The question arose: what happens when an inspector is standing in front of an unfamiliar asset in a basement mechanical room with no signal, trying to remember what the OFC requires?
**Insight:** The inspection form is not just a data entry screen. It is a moment of professional uncertainty — the inspector knows their trade but may not remember the specific code requirement for this asset type, this interval, this deficiency severity. Right now they call a colleague, guess, or skip it. That moment of uncertainty is the most important moment TapLog can own. The app that resolves uncertainty at the point of inspection owns the relationship with the inspector in a way that passive record-keeping software never can.
**What we did:** Identified a four-level guidance roadmap: (1) `checklistItems` per OFCAssetType — what to inspect, displayed pre-form, no connectivity required; (2) collapsible contextual guidance panel on the inspection form — plain-language OFC interpretation per asset type; (3) in-app AI assistant — OFC-aware, asset-context-aware, offline-cached answers to common questions; (4) pre-inspection building briefing — overdue assets, open deficiencies, OFC changes since last visit. Level 1 is a data addition, deliverable in Module 27. Level 3 is the defensible moat.
**What's next:** Add `checklistItems: List<String>` to `OFCAssetType` in Module 27. Design the AI assistant architecture post-Module 31 when the core is stable — system prompt loaded with OFC knowledge and current asset context, plain-language responses, offline cache of common Q&A per asset type.
**Broader principle:** The highest-value moment to help a user is the moment of uncertainty, not after the fact. A tool that is present and useful at the moment of doubt earns loyalty that a reporting dashboard never will. Build for the basement, not the boardroom.

---

## LB-007 — The real customer had their good tool taken away
**Module:** Module 27a — Organisation + Site hierarchy (Session 8)
**Trigger:** Learning the backstory of TapLog's Ember pilot contact. A 30-year veteran fire safety inspector whose company switched from a purpose-built offline tool to a cloud-only generic asset tracker after a leadership change. The new tool has no offline capability, no NFC, no OFC knowledge. The inspector now manually bridges two systems to do his job. The switch was made by decision-makers who don't do the work.
**Insight:** The most motivated early adopter is not someone discovering a new capability — it's someone whose existing capability was taken away by people who didn't understand it. This inspector already knows what good looks like. He doesn't need to be convinced that offline-first matters, or that NFC proves presence, or that OFC intervals shouldn't be manually entered. He lived all of that. TapLog isn't selling him something new — it's giving him back something he lost.
**What we did:** Reframed the go-to-market target. The primary market segment is not "fire safety inspectors who want to go digital" — it's "fire safety companies whose field tool was replaced by a cloud-only generic tracker by non-field decision-makers." This segment is reachable, motivated, and already has budget allocated (they're paying for the inferior tool). Updated competitive positioning to include a direct attack angle against generic trackers.
**Research finding:** The specific class of generic tracker TapLog displaces has a documented profile: cloud-only (no offline), QR/barcode-only (no NFC, no proof of physical presence), no regulatory intelligence (no OFC intervals, no trade-specific knowledge), freemium pricing that surprises users as data grows, and mobile app functionality rated as "restricted and unreliable" in user reviews. The tool looks good in a browser demo. It fails in a basement mechanical room.
**Broader principle:** When a product decision is made by people who don't use the product, the people who do use it become a reachable market. Look for software categories where the buyer and the user are different people with different priorities. The buyer optimized for the demo; the user optimizes for the basement.

---

## LB-008 — A trigger that's never called is the same as a feature that doesn't exist
**Module:** Module 28 — Authenticated identity (Session 9)
**Trigger:** After verifying auth, org, site, and inspection all worked on device, sync produced no records in MongoDB. No errors in Railway logs. No data arriving. Backend was healthy, app was healthy, sync was simply never running.
**Insight:** The `TapLogApplication` class was rewritten during Module 28's auth refactor. The original version had no `onCreate()` — sync was only triggered by `ConnectivityReceiver`. When the auth rewrite added the new class structure, the connectivity receiver trigger remained, but it only fires on network state *changes*. On a device that's been connected the whole time, there's no change, no receiver, no sync. The fix was one line — `scheduleSyncIfNeeded(this)` in `TapLogApplication.onCreate()`. A perfectly correct sync implementation sitting dormant because the entry point was missing.
**What we did:** Added `override fun onCreate() { super.onCreate(); scheduleSyncIfNeeded(this) }` to `TapLogApplication`. Sync fires on every app launch now.
**What made this hard to debug:** `ExistingWorkPolicy.KEEP` meant the airplane-mode trick only worked once. After the first toggle, WorkManager held onto the (non-existent) queued work and ignored subsequent triggers. The Background Task Inspector in Android Studio would have shown the work was never enqueued — the right diagnostic tool for WorkManager issues.
**Broader principle:** When you rewrite a class that has startup side effects, audit every effect that was in the old class. Refactoring is a context switch — the code that disappears from one file often needs to reappear in another. The compiler can't catch missing `onCreate()` calls; only a full-flow test can.

---

## LB-009 — Library wrappers add fragility on top of stable APIs
**Module:** Module 28 — Authenticated identity (Session 9)
**Trigger:** Backend returning 500 on account registration. Stack trace pointed to `passlib.handlers.bcrypt` — `AttributeError: module 'bcrypt' has no attribute '__about__'`. The `bcrypt` library had been updated to 4.x; `passlib` 1.7.4 used `__about__` to read the bcrypt version, which was removed.
**Insight:** `passlib` is a convenience wrapper around `bcrypt` that adds a CryptContext abstraction, deprecated-scheme handling, and a plugin architecture for multiple hashing backends. For TapLog, only one backend (bcrypt) is ever used. The underlying `bcrypt` API is three functions: `hashpw`, `checkpw`, `gensalt`. They've been stable for a decade. `passlib` added a layer of indirection that broke when `bcrypt` 4.x changed an internal attribute. The fix was simpler than the original — remove `passlib`, call `bcrypt` directly.
**What we did:** Removed `passlib[bcrypt]` from `requirements.txt` and `pyproject.toml`, replaced with `bcrypt==4.2.1`. Rewrote `hash_password()` and `verify_password()` in `app/auth.py` to use `bcrypt.hashpw`/`checkpw`/`gensalt` directly.
**Broader principle:** Library wrappers that add abstraction over a stable, simple underlying API are fragility, not convenience. When you only need 10% of a library's surface area, the other 90% is surface area for breakage. Prefer direct API calls for stable, well-understood operations. The cost of a thin wrapper is the maintenance risk of an indirect dependency.

---

*Add new lightbulb moments as they occur during the build. Format: trigger → insight → action → broader principle (if any).*
