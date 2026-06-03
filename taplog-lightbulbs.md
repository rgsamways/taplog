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

## LB-006 — The inspection guidance gap is a product, not a feature
**Module:** Strategy session (Session 7)
**Trigger:** Discussing what happens after the OFC checklist is displayed — does TapLog just show the steps, or does it help the inspector understand what they're looking at?
**Insight:** Showing a checklist is table stakes. The gap is interpretive: a newer inspector standing in front of a dry chemical extinguisher knows the checklist says "check pressure gauge" but may not know what a green zone reading looks like, what a slow leak pattern means, or when a gauge reading requires immediate removal from service vs. a note. That interpretive layer — the knowledge an experienced inspector carries in their head — is not in any app today. It is also exactly what a well-prompted AI model can surface, per asset type, per checklist item, on demand.
**What we did:** Named the concept **AI co-pilot** — a module that adds contextual guidance to the inspection form. Per-item "What am I looking for?" prompts, severity guidance, and deficiency language suggestions. Flagged as Module 33. Positioned as a premium tier differentiator, not a free feature.
**Broader principle:** The most valuable product additions are the ones that transfer expert knowledge to non-experts at the moment it's needed. A checklist tells you what to do. A co-pilot tells you what it means.

---

## LB-007 — The dashboard is a company tool, not an inspector tool
**Module:** Strategy session (Session 8)
**Trigger:** Designing `SiteListScreen` as the home screen. The question arose: whose view is this — the individual inspector or the company owner?
**Insight:** A solo inspector and a company owner with 10 field staff need fundamentally different home screens. The solo inspector wants "what am I doing today." The company owner wants "what is my company's coverage status." Building the dashboard for the inspector produces a screen that's useless to the buyer. Building it for the company owner produces a screen the inspector finds confusing. The correct answer is: the dashboard is a company tool by default, and the company owner is the person who actually decides to buy TapLog.
**What we did:** Reframed `SiteListScreen` as an org-level site dashboard, not a personal task list. Sites are org assets; the inspector's job is to service them. Added site-level stats (asset count, overdue count) to each site card. The company owner can see coverage at a glance; the inspector uses it as a jump-off point.
**Broader principle:** Design for the buyer's view, not the user's view. In B2B tools, the person who pays and the person who taps are often different people. Build the home screen for the person who signs the cheque.

---

## LB-008 — Refactoring erases startup side effects
**Module:** Module 28 — Authenticated identity (Session 9)
**Trigger:** After refactoring `TapLogApplication` to add repository wiring, background sync stopped firing on app launch.
**Insight:** The original `TapLogApplication.onCreate()` called `scheduleSyncIfNeeded(this)`. The refactored version didn't — it was a clean rewrite focused on adding the new repository wiring, and the sync call was simply not carried over. The app built without errors because `scheduleSyncIfNeeded` is a standalone function with no callers enforced by the compiler.
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

## LB-010 — Ask what's different before building the abstraction
**Module:** Module 34 — Vertical engine (Session 10)
**Trigger:** Before implementing the `VerticalConfig` abstraction, the question was raised: what is actually different between verticals? The initial design assumed `resultOptions: List<String>` and `intervalMonths: Int?` were sufficient.
**Insight:** The surface-level differences between verticals (different labels, different intervals) are easy to parameterize. The deeper differences are structural: inspection cardinality (one NFC tap = one asset = one form is wrong for Fleet and Hatch), pass/fail downstream logic (Anchor's "Remove from service" is a physical action, not just a label), and trigger type (Fleet runs on mileage, not months). Parameterizing labels without parameterizing downstream logic produces a config engine that looks general but isn't — the hard-coded assumptions just move from screen code to data structures.
**What we did:** Replaced `resultOptions: List<String>` with `List<ResultOption>` where each option carries a `ResultAction` enum (NONE, REMOVE_FROM_SERVICE, NOTIFY_AUTHORITY, ISSUE_CERTIFICATE, DELIVER_REPORT). Replaced `intervalMonths: Int?` with `TriggerConfig` carrying type (CALENDAR, MILEAGE, ENGINE_HOURS) and the appropriate interval value. Left inspection cardinality as an open design doc — it's load-bearing across Room FKs, sync payload, and ScanState, and speculative change would be more expensive than the current limitation.
**Broader principle:** The right time to ask "what's different?" is before building the abstraction, not after. A premature abstraction locks in the wrong shape. An abstraction built after cataloguing the differences is almost always simpler and more durable. Two hours of dissimilarity analysis avoids two sprints of refactoring.

---

## LB-011 — Context files are the cheapest form of architectural documentation
**Module:** Module 34 — Vertical engine (Session 10)
**Trigger:** The `/opsx:propose` command ran and produced a complete set of artifacts — for the wrong module entirely. The proposal described adding a fire pump asset type. The actual module was a major architectural refactor. The context wasn't in the repo; it was in a strategy session transcript that hadn't been committed.
**Insight:** An AI that reads the repo cold will build from what's there. If the architectural intent for a module lives only in a chat session or a person's head, the AI has no way to distinguish "add a fire pump type" from "build a multi-vertical engine." A single markdown file dropped into `openspec/` with the strategic context for an upcoming module costs 15 minutes to write and eliminates an entire category of propose/apply do-overs.
**What we did:** Added `openspec/module-34-context.md` with full architectural intent: what the module is, key decisions, data structures, vertical catalogue, scope boundary, and what doesn't change. The next propose read it, produced the right proposal on the first run.
**Broader principle:** The gap between "what you intend to build" and "what the AI will propose" is exactly equal to the gap between what's in the repo and what's in your head. Close that gap with files, not prompts. Files persist across sessions, team members, and tool switches. Prompts don't.

---

## LB-012 — Registration entry points define org structure
**Module:** Session 11 — Onboarding governance discussion
**Trigger:** Designing the invitation / join flow for multi-inspector companies. Who creates the org? How does inspector 2–15 join an existing company account rather than accidentally creating a new one?
**Insight:** The registration form is effectively the org creation form. If every inspector who downloads the app registers independently, you get N orphaned solo orgs instead of one company account. The company owner needs to control org membership — which means the invite-token model: owner creates the org, generates a join code, shares it with their team. The inspector's registration screen accepts a join code. Without a join code, the account is solo by default. The token routes new inspectors into the right org before a single record is created.
**What we discussed:** V1 can use a simple 6-character join code on the registration screen. The V2 admin portal — browser-based, not in the inspector app — gives the org owner an interface to generate codes, see which inspectors have joined, manage billing seats, and view org-wide coverage. This is a distinct product surface that doesn't belong in the inspector app.
**Broader principle:** The registration entry point encodes your assumptions about who the customer is. Designing registration for individuals produces individual accounts. Designing it for companies — with an opt-in join mechanism — produces org-level accounts from day one. Build the revenue unit into the entry point.

---

## LB-013 — Org membership count is the UI complexity dial
**Module:** Session 11 — Onboarding governance discussion
**Trigger:** Looking at `SiteListScreen`, which shows all sites for the org. With one inspector, that's fine — there's only one person's work to show. With 10 inspectors sharing an org, the site list is a shared pool and the UI has no mechanism to show inspector assignment, filter by responsible inspector, or surface a per-inspector workload view.
**Insight:** The current UI implicitly assumes `org.inspectorCount == 1`. The site list, the asset list, the open deficiencies view — all show org-level data because there's only one inspector to show it for. When the company tier becomes the primary revenue unit, this assumption breaks. "Whose sites are these?" becomes a meaningful question. Multi-inspector UI is not a feature increment on single-inspector UI — it requires a different information architecture. But building that architecture speculatively, before the company tier has a single paying customer, is waste.
**What we discussed:** Keep V1 with the single-inspector-per-org mental model. When the company tier activates, gate UI complexity behind `org.inspectorCount > 1` — show the simple view by default, unlock the complex view when the data signals it's needed.
**Broader principle:** Premature multi-tenancy UI is expensive. Build for the smallest viable customer and let the data dial up complexity progressively. An org inspector count is the right signal — it reflects actual usage, not a pricing tier flag that can be gamed.

---

## LB-014 — Vertical bundles are industry fluency, not feature lists
**Module:** Strategy session (Session 13)
**Trigger:** Observing that mining operations need multiple verticals (Ember, Anchor, Hatch, plus future mining-specific types) and that no single vertical captures the industry.
**Insight:** A mine safety manager doesn't want to evaluate whether TapLog's fall protection vertical covers their regulatory requirements — they want a product that speaks their language out of the box. Packaging verticals into named, branded bundles sells industry fluency rather than feature lists. The buyer recognizes themselves in the product name before they read a single feature.
**What we discussed:** A `VerticalBundle` — a named collection of `TapLogVertical` enums with a display name, industry tag, and billing SKU. Org onboarding presents bundles first; individual vertical selection is available as "custom." Bundle naming uses the industry's own vocabulary, not TapLog's internal vertical names. First named bundle: **TapLog Adit** (mining) — Ember + Anchor + Hatch as the core life safety cluster for underground operations. Name chosen for industry recognition (an adit is a horizontal mine entry tunnel), no common-language baggage, and a latent product metaphor: an adit is how you get into the mine; TapLog Adit is how you get into compliance.
**Broader principle:** Buyers don't buy components — they buy recognition. A product that reflects a buyer's industry back at them closes faster than one that requires them to assemble the right combination themselves. Bundling is a sales motion disguised as a packaging decision.

---

## LB-015 — Inspectors know what they found; they need help saying it
**Module:** Strategy session (Session 13)
**Trigger:** Observing that field inspectors often write vague pass/fail notes — not because they don't understand the finding, but because precise regulatory language doesn't come naturally under field conditions.
**Insight:** "Pressure low, needs service" and "gauge reading 150 PSI against CAN/ULC-S536:19 minimum of 175 PSI — 14% below threshold, remove from service pending recharge" describe the same finding. Only one is defensible in an investigation, credible to a building manager, or useful to an insurer. The inspector knew the finding; they just didn't have the language at their fingertips in a mechanical room with gloves on. This applies equally to pass notes — "all good" and "all 12 extinguishers within CAN/ULC-S536:19 operating range, pins and seals intact, no corrosion observed" are the same outcome with very different evidentiary weight.
**What we discussed:** A hybrid interaction model inside the deficiency dialog and pass notes field: (1) offline chip row — pre-built per asset type: `[Code Reference]` `[Threshold]` `[Action Required]` `[Severity]` — tap to append a structured clause instantly, no AI call, works underground; (2) a single "Improve this" button that takes the full notes field and returns polished, code-referenced language in one AI call. Chips handle the 80% case with zero latency. Polish handles the inspector who wants professional output without thinking about it.
**Reframe:** This is not about making failures sound better — it's about making every finding speak the language of the standard, regardless of outcome. A detailed pass record is as valuable as a deficiency record in a post-incident investigation or insurance claim.
**Relationship to LB-006:** The AI co-pilot (LB-006) helps inspectors understand what they're looking at. This feature helps them communicate what they found. Same session, different moment — guidance before the form, language after.
**What to call it:** TBD — lives inside the inspection form and deficiency dialog, possibly as a named capability within the co-pilot module (Module 33) or as a distinct sub-feature.
**Broader principle:** The value of an inspection record is not just its accuracy — it's its authority. A finding that references the standard, cites the threshold, and uses the code's own language is unchallengeable. Tools that help non-writers produce authoritative records create compounding value: better records → more credible reports → more defensible inspectors → stronger TapLog data layer.

## LB-032 — Farpost is a template, not a product
**Module:** Strategy session (Session 13)
**Trigger:** Asking whether Farpost's architecture could be replicated to build four other downstream applications that consume TapLog records.
**Insight:** Farpost isn't an insurance claims app. It's a record consumption and decision surfacing engine. The insurance workflow is the first skin on top of that engine. The engine itself is four steps: (1) Ingest — consume verified records from TapLog keyed by a common identifier; (2) Match — when a trigger event occurs, look up all relevant records for that identifier; (3) Surface — present matched records in the context of the decision the professional needs to make; (4) Act — professional makes a better decision, logs the outcome, which becomes a record for the next downstream application. That engine is Farpost. It's also every other application in the family.
**The five applications:**
- **Farpost** — insurance claims dispatch. Trigger: claim filed. Professional: adjuster. Decision: liability and settlement.
- **Permit** — construction draw management. Trigger: draw requested. Professional: construction lender. Decision: release funds.
- **Roster** — workforce certification. Trigger: worker checks in. Professional: site safety manager. Decision: authorize site access.
- **Ledger** — property compliance history. Trigger: due diligence / title transfer. Professional: real estate lawyer or lender. Decision: approve transaction.
- **Signal** — portfolio risk scoring. Trigger: policy renewal or underwriting. Professional: commercial property insurer. Decision: price the risk.
**The platform:** The shared engine — ingest, match, surface, act — is the template. Each application shares the ingest layer, matching engine, and sync infrastructure. What changes per app is the domain model, the trigger events, and the UI skin.
**Investor framing:** TapLog generates the records. The Farpost platform family consumes them. The founder owns both ends of the pipe.
**Broader principle:** When you find yourself building the same engine twice, you've accidentally invented a platform. Name it, abstract it, and build the second and third applications on top of it rather than beside it. The moat isn't any one application — it's the engine underneath all of them.

---

*Add new lightbulb moments as they occur during the build. Format: trigger → insight → action → broader principle (if any).*
