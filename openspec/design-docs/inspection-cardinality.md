# Design Doc — Inspection Cardinality
> Status: Open — not yet designed
> Blocking: Fleet (FLEET), Hatch multi-asset sessions, Anchor batch pre-use

## Problem

`asset.id` is the FK anchor for `Inspection`, `Deficiency`, `ScanEvent`, and `TagEvent`.
One tap = one asset = one inspection is load-bearing throughout the data model, Room schema,
sync payload, and ScanState machine.

Fleet, Hatch (session), and Anchor (batch) need a session-scoped or space-scoped inspection
that covers multiple assets or a physical space rather than a single asset.

## Questions to answer before designing

- Does a session get its own UUID, or does it decompose into per-asset inspections?
- How does NFC scanning work inside a session — does each tap add to the session?
- What does the sync payload look like for a session vs. per-asset inspections?
- How does the PDF report render for a session inspection?

## Verticals affected

- **HATCH** — entry event covers a confined space (not an asset)
- **FLEET** — vehicle inspection covers the whole vehicle across sub-systems
- **ANCHOR** — batch pre-use check across multiple harnesses in one crew session

## Verticals NOT affected (single-asset model is correct)

EMBER, LIFT, NEWEL, APEX, VAULT, VALVE, SPAN, MAST, GRID, SEAM, CRANE, RACK, WARD
