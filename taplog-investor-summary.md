# TapLog — Investor Summary
> Last updated: 2026-05-31

---

## What is TapLog?

TapLog is an offline-first NFC/RFID asset inspection platform for regulated trades. Inspectors tap their phone to a passive NFC tag attached to a physical asset — a fire extinguisher, a fall arrest anchor, a pressure vessel — and the app logs a timestamped, tamper-evident inspection record. No internet required. Records sync to a backend when connectivity is available.

**Tagline:** Tap. Log. Done.

---

## The Problem

Regulated trades — fire safety, fall protection, confined space, elevators, pressure vessels — are required by law to inspect and document physical assets on a recurring schedule. The inspection record is the proof of compliance.

Today, most of this happens on paper or in generic spreadsheet tools. The problems:

- Paper records get lost, damaged, or falsified
- No proof of physical presence at the asset
- No real-time visibility into overdue inspections or open deficiencies
- Existing software is either too broad (generic field service tools), too deep (enterprise-only), or built for a single trade with no platform thinking

A particularly acute problem has emerged recently: fire safety companies switching from purpose-built tools to generic cloud-based asset trackers — typically after a leadership change where the new decision-makers optimized for the browser demo rather than the field reality. These tools have no offline capability, no NFC proof of presence, and no regulatory intelligence. Field inspectors end up manually bridging two systems. This displacement pattern creates a motivated, reachable, already-budgeted market for TapLog.

No one has built the platform model: one inspection engine, purpose-built for multiple regulated trade verticals.

---

## The Solution

TapLog solves all of this in one tap:

- **Physical presence proven** — NFC tag on the asset means the inspector was physically there
- **Offline-first** — works in basements, mechanical rooms, remote sites with no signal
- **Tamper-evident audit trail** — every inspection is timestamped and tied to a specific tag ID
- **Deficiency tracking** — problems are logged with severity codes at time of inspection
- **Sync** — records upload automatically when back in range
- **Regulatory intelligence built in** — OFC inspection intervals enforced automatically; no inspector can accidentally set the wrong cadence
- **Authenticated identity** — inspector accounts with device registration mean knowing a cert number is not enough to fake a record
- **Organisation hierarchy** — multi-inspector companies manage their sites, clients, and assets in a single system

The inspector experience is designed for the trades, not for office workers. The entire inspection flow — scan, fill form, submit — takes under two minutes.

---

## Proof of Presence — The Core Differentiator

No competitor offers NFC-native proof of physical presence. The combination of controls makes falsification forensically detectable:

- NFC tag on asset (can't tap remotely)
- On-metal tags that destroy on removal (tag stays on asset)
- Tamper-evident scan log independent of inspection record
- GPS coordinates on every inspection
- Device ID on every record
- Authenticated inspector identity — password + registered device required
- Photo capture (roadmap)

**The position:** We don't make fraud impossible. We make it stupid.

---

## Platform Model

TapLog is not one product. It is a platform with vertical-specific modules, each serving a different regulated trade.

| Vertical | Domain | Status |
|---|---|---|
| **Ember** | Fire safety inspection | 🔨 Active build — full hierarchy working on device |
| **Anchor** | Fall protection | Specced, next after Ember |
| **Hatch** | Confined space | Specced, tier 1 underserved |
| Mast | Scaffolding / telecom towers | Tier 2 |
| Crane | Crane and hoist | Tier 2 |
| Seam | Welding / pressure vessels | Tier 2 |
| **Newel** | Home inspection | Held for pilot user |
| Span | Bridges / infrastructure | Tier 3 |

Each vertical shares the same core engine (NFC scanning, offline sync, deficiency tracking, reporting) and adds trade-specific asset types, inspection codes, and regulatory frameworks. The platform moat: every vertical built makes the next one cheaper and faster to ship.

---

## Beachhead Strategy

**Vertical 1: Ember (fire safety)**

Fire safety is the strongest beachhead:
- Every commercial building in Ontario requires annual fire safety inspections under the Ontario Fire Code
- Fire safety inspectors are licensed, identifiable, and actively looking for tools
- Paper-based and generic-tool workflows are dominant — the bar is low
- Asset types are well-defined (extinguishers, pull stations, suppression systems, exit lighting)
- Inspection codes (OFC) are standardized and now built directly into the app
- A significant number of fire safety companies have recently migrated to cloud-only generic trackers that fail in the field — they are motivated, reachable, and already have budget allocated

**Target customer:** Multi-inspector fire safety company (3–15 inspectors, territorial coverage). The company owner is the buyer; the field inspector is the champion. This is a B2B sale to a trade business, not a consumer app.

**Sequence after Ember:** Anchor → Hatch — the "life safety cluster." These three verticals together address the highest-urgency, most underserved segment of the market.

**Life safety cluster — full comparison:**

| | 🔴 Ember | 🟡 Anchor | 🟠 Hatch |
|---|---|---|---|
| Legislation | Ontario Fire Code | OHSA O. Reg. 213/91 | OHSA O. Reg. 632/05 |
| Who inspects | Licensed fire safety inspector | Competent worker + certified inspector | Supervisor + attendant + entrant |
| Inspection trigger | Calendar (annual/semi-annual) | Pre-use + annual certified | Every entry event |
| Offline criticality | Moderate | High | Very high (underground) |
| User profile | Dedicated inspector, building walkthrough | Worker inspecting own gear | Multi-role team, entry event |
| Annual data points (Ontario est.) | 4–8M | 25–250M | 250K–7.5M |
| Ontario revenue ceiling (est.) | $5–10M ARR | $15–30M ARR | $5–15M ARR |
| Time to ship after Ember | — | 4–8 weeks | 8–16 weeks |
| Regulatory codes to encode | OFC (done) | CSA Z259 series | O. Reg. 632/05 |

---

## Competitive Landscape

| Competitor | Why they're not a threat |
|---|---|
| Inspect Point | Single-trade, annual contract only, $200–500/mo for small shops, 3.2/5 value rating, no NFC |
| FireMate / Uptick | Australian acquisition, enterprise pricing, built for Australian regulations not OFC |
| SafetyCulture | Generic blank-canvas forms, no OFC knowledge, no NFC, inspector builds everything from scratch |
| Generic cloud trackers | Cloud-only, QR/barcode only (no NFC), no OFC knowledge, no offline, mobile app "restricted and unreliable" per user reviews. Freemium pricing that surprises users as assets scale. Chosen by office decision-makers for the browser demo; fails in the field. |
| Google Forms / spreadsheets | No NFC, no offline-first, no audit trail, personal liability on every lost record |

No player is building the platform model across regulated trade verticals with NFC-native, offline-first, authenticated-identity architecture. That is the white space TapLog occupies.

**Attack angles:**
- vs Inspect Point: "Built for the office, not the field."
- vs FireMate/Uptick: "Australian product, doesn't know the OFC."
- vs SafetyCulture: "You're using a blank canvas. TapLog is a fire safety tool."
- vs generic cloud trackers: "No offline. No NFC. No OFC. It looked good in the demo. Your inspector is manually bridging two systems in a basement with no signal."
- When they already use Inspect Point: "TapLog is the NFC layer that proves your inspector was physically there. Nothing else can do that."

---

## Regulatory Environment — A Tailwind, Not a Risk

The Ontario Fire Code was significantly amended effective January 1, 2026 under O. Reg. 87/25 — the most significant overhaul of fire safety standards in two decades:

- Fire alarm inspections now governed by CAN/ULC-S536:19 — expanded from 44 to 73 pages, 90+ revisions
- Every device must be tested individually — no more sampling
- Stricter attendance logs and documentation requirements for technicians
- New Administrative Monetary Penalties — municipalities can now issue fines for non-compliance on the spot
- Inspections estimated 30–35% more time-intensive and expensive for building owners

This is a direct tailwind. Ontario's fire safety inspectors are facing the most demanding documentation environment they have ever seen, effective this year. They need better tools right now. TapLog's timing is not accidental — it is optimal.

The direction of regulatory travel is always toward stricter, never toward looser. Every future OFC amendment makes TapLog more valuable, not less. When inspection intervals change, TapLog updates one data file.

**Investor answer on regulatory risk:** "Regulatory change is a tailwind. We've already validated this with the 2026 amendments. When the code changes, we update a data file."

| Tier | Price | Who |
|---|---|---|
| Solo | $39/mo | 1-person operation, up to 3 devices |
| Team | $29/mo per seat, min 3 seats | 3–10 inspector company |
| Company | $199/mo flat | Up to 15 inspectors + reporting dashboard |

Month-to-month on all tiers. Annual option at 2 months free. No implementation fees. No annual contracts.

The market range for fire inspection software is $20–500/month. Inspect Point charges up to $129/user/month with annual billing required. TapLog's $39 solo tier is below every meaningful competitor and above the "is this a real product" threshold.

**Primary revenue target: Company tier ($199/mo flat).** Multi-inspector fire safety companies are the volume opportunity. The decision maker is the company owner; billing attaches to the Organisation, not the individual inspector. A 10-inspector company on Inspect Point's annual contract is paying $15,000+/year. TapLog's Company tier is $2,388/year — with NFC proof of presence they don't have, offline-first they don't have, and OFC compliance built in they don't have.

---

## Strategic Asset: TapLog × Farpost

TapLog is being built alongside **Farpost** — an insurance claims dispatch platform. The integration:

- TapLog (Ember/Newel) creates **pre-loss inspection records** tied to a civic address
- When a claim is filed in Farpost for the same address, the adjuster sees the inspection history automatically
- This makes TapLog records valuable not just to the inspector, but to the insurer

**The flywheel:** more TapLog inspections → better Farpost signal → more insurer adoption → more demand for TapLog inspections.

Two products that compound each other's value are more defensible than one product alone. The integration is the moat, not the bundle.

---

## Strategic Asset: The Data Layer

Every TapLog inspection generates a georeferenced, inspector-verified, asset-level compliance record — something that has never existed at scale before. Aggregated across thousands of inspectors and years, this dataset is valuable to parties well beyond the inspector paying the subscription:

- **Insurers** — actuarial risk signal at underwriting time. A building with a clean TapLog history is a demonstrably lower risk. Farpost is already the bridge; the next step is insurers paying for compliance scores at policy origination.
- **Property managers** — portfolio compliance dashboard across all buildings, all assets, real-time. The building owner pricing tier monetizes this directly.
- **Office of the Fire Marshal** — provincial compliance visibility and cluster risk signals. A partnership, not a revenue stream — but one that makes TapLog regulatory infrastructure.
- **Municipalities** — verification that private inspectors actually visited buildings. Creates procurement signals that drive adoption.
- **Research and academia** — built environment data at asset resolution is rare and valuable to fire safety researchers, urban planners, and insurance actuaries.

**The flywheel:** more inspectors → more records → more valuable dataset → more demand for TapLog → more inspectors.

**The design principle:** Never sell individual records without consent. No hard deletes — ever. The audit trail is the product. GPS on every record, civic address normalized from day one.

**Investor framing:** "TapLog generates subscription revenue today. At scale, the compliance dataset has actuarial value to insurers, regulatory value to the OFM, and portfolio value to property managers. We're building the inspection layer of the built environment."

---

## Strategic Asset: Bulk Tag Provisioning (v2)

- TapLog orders NFC tags in bulk (10,000+) and operates a provisioning service
- Tags are encoded server-side with client asset data and shipped ready to install
- On first scan of a pre-provisioned tag, the inspector's device pulls the asset record automatically
- Client never touches software to set up assets — they receive tagged, ready-to-inspect hardware

**Tag economics:**

| Quantity | On-metal NFC tag cost |
|---|---|
| 10 (pilot) | $2–5/tag |
| 100–500 | $1–2/tag |
| 1,000–5,000 | $0.50–1.00/tag |
| 10,000+ | $0.25–0.50/tag |

Provisioned tag landed cost at scale: ~$1.50–3.00. Charge: $5–8/tag. A 50-building property manager needing 7,500 tags represents $45,000 in tag revenue before a single month of subscription.

This creates a recurring hardware + service revenue stream alongside the software subscription, and dramatically lowers the barrier to deployment for large commercial clients.

---

## Technology

- **Platform:** Android (Kotlin, Jetpack Compose)
- **Offline storage:** Room (SQLite)
- **Sync:** WorkManager background worker (verified end-to-end)
- **Backend:** FastAPI + MongoDB (live — https://web-production-a9fb1.up.railway.app)
- **NFC:** Native Android NFC foreground dispatch — no third-party dependency
- **Identity:** Authenticated inspector accounts with device registration (Module 28)
- **Email:** Resend (transactional — verification codes)
- **Infrastructure:** Railway (auto-deploy), MongoDB Atlas (M0 free tier)

---

## Traction

- **Ember v1 loop verified end-to-end on physical device** (Pixel 10 Pro XL, API 36):
  Register → email verification → org setup → site creation → NFC scan → asset registration → inspection → PDF report → share sheet
- Full Organisation → Site → Asset → Inspection hierarchy working on device
- **Tamper-evident scan log** — every NFC tap logged as a `ScanEvent` (insert-only, synced), independent of whether inspection is submitted
- **Tag lifecycle** — `TagEvent` records every tag attachment and retirement. Tag replacement workflow on AssetDetailScreen. History survives tag replacement.
- **Pre-inspection checklists** — 35 OFC asset types, each with 5–7 CAN/ULC-S536:19-aligned inspection steps, displayed before the form, fully offline
- **Authenticated inspector identity** — registration, email verification (6-digit code), device registration with new-device email challenge, JWT on every request. Knowing a cert number is not enough to fake a record.
- **PDF inspection reports** — one PDF per inspection, generated on-device in under 1 second, shared via Android share sheet (email, WhatsApp, Drive). Includes full org/site/asset/inspector/deficiency record.
- **Photo capture for deficiencies** — inspector photographs the deficiency at time of recording. Thumbnail shown inline in inspection form and open deficiencies list. Photo path persisted in Room and sync record.
- OFC asset type picker — 6 categories, 35 asset types, full-text search, plain-English descriptions
- Regulatory inspection intervals enforced automatically per OFC
- Source-aware back navigation + system back gesture support on all screens
- Open deficiencies screen with mark-resolved workflow
- FastAPI backend live on Railway — all sync endpoints verified end-to-end with Bearer auth
- Full offline → sync pipeline verified: NFC tap → Room → WorkManager → Retrofit → Railway → MongoDB Atlas
- **Ember pilot user identified** — experienced fire safety inspector at a multi-location SW Ontario company
- **Newel pilot user identified** — Geoffrey Gordon, Citadel Home Inspections, Bancroft ON
- OAFC Annual Conference & Trade Show — Niagara Falls, November 19–21 2026 — target demo event

---

## Go-To-Market

**Phase 1 — Corridor (now → August 2026)**
Bancroft–Peterborough–Belleville corridor. Personal outreach. Ember pilot activated. 3–5 paying customers before the OAFC conference. Focus: companies that recently migrated to a cloud-only generic tracker and are feeling the pain.

**Phase 2 — Provincial (September → December 2026)**
OAFC Niagara Falls November. Reference customer from Phase 1. Expand to 20–50 paying inspector seats across Ontario.

**Phase 3 — National (2027)**
OFC framework applies Canada-wide. Same product, new provinces. NEFEC Huntsville. PDAC if mining vertical launched.

---

## Founder

The founder has approximately 25 years of software development experience, including 15 years building an offline-first CRM with backend sync — the exact architectural pattern at the core of TapLog. Academic background: geography major, geology minor (University of Windsor). Based near Bancroft, Ontario — the heart of Ontario's mineral belt and a natural market for earth science and trades technology.

---

## Market

- Ontario alone has tens of thousands of licensed fire safety inspectors, home inspectors, and fall protection contractors
- Every commercial property requires recurring regulated inspections — the demand is legislated, not optional
- The addressable market expands with every vertical added
- Canadian-first go-to-market, with architecture that scales nationally and internationally

---

## What We're Looking For

TapLog is seeking angel investment to accelerate:

1. Completion of Ember v1 — tamper-evident scan log ✅, authenticated identity ✅, PDF reports ✅, photo capture ✅, visual asset ID (Module 31)
2. Launch of Anchor and Hatch verticals
3. Sales and onboarding of first paying Ember customers
4. Farpost integration (Newel × Farpost address lookup)
5. Bulk tag provisioning service (v2)

---

## Contact

**Robin** — Founder, TapLog
taplog.ca
