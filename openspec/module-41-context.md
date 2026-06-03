# Module 41 — Context File
> For: `/opsx:propose "module-41-app-icon"`
> Written: 2026-06-01
> No dependencies — can be done any time

---

## What this module is

Replace the default Android robot launcher icon with the TapLog brand icon. The icon is already fully defined as Canvas drawing code in `SplashScreen.kt` (`drawTapLogIcon()`). This module translates that into proper adaptive launcher icon assets.

---

## Scope

**In scope:**
- `ic_launcher_foreground.xml` — vector drawable of the TapLog icon foreground
- `ic_launcher_background.xml` — solid navy background layer
- `res/mipmap-anydpi-v26/ic_launcher.xml` — adaptive icon definition
- `res/mipmap-anydpi-v26/ic_launcher_round.xml` — round variant
- Legacy PNG fallbacks in `mipmap-hdpi/xhdpi/xxhdpi/xxxhdpi` (for pre-API 26, unlikely but correct)
- `AndroidManifest.xml` `android:icon` and `android:roundIcon` — already point to `@mipmap/ic_launcher`, no change needed if files replace in place

**Out of scope:**
- No Kotlin code changes
- No Room changes
- No backend changes
- Play Store feature graphic or screenshots — separate concern

---

## Icon design

The TapLog icon is defined in `SplashScreen.kt` `drawTapLogIcon()`. Translate that directly to a vector drawable:

**Background layer:**
- Solid fill: `#1A2744` (TapLogNavy800)
- The adaptive icon system handles the rounded-rect masking — background should be a full bleed rectangle, not a pre-rounded shape

**Foreground layer — vector drawable elements:**
1. **Rounded rectangle** — navy (`#1A2744`) body, slightly inset from edges to give breathing room within the adaptive icon safe zone
2. **Three NFC arc strokes** — centred, teal, increasing radius:
   - Inner arc: `#38BDF8` (TapLogTeal400), full opacity, stroke ~3dp
   - Middle arc: `#38BDF8`, ~45% opacity
   - Outer arc: `#38BDF8`, ~20% opacity
   - Arcs are concentric quarter-circles opening upward-right (standard NFC convention)
3. **Phone body** — small rounded rectangle, teal fill (`#38BDF8`), centred below arcs
4. **Screen area** — slightly darker inset on phone body (`#0EA5E9`, TapLogTeal600)
5. **Log lines** — two or three short horizontal strokes on the screen area, teal-deep
6. **Tap dot** — small filled circle, `#7DD3FC` (TapLogTeal200), at the NFC focal point

**Safe zone:** Android adaptive icons have a 66dp safe zone within a 108dp canvas — all important elements must stay within the inner 66dp circle to avoid being clipped by launcher masks.

---

## Vector drawable approach

Write `ic_launcher_foreground.xml` as an Android vector drawable (`<vector>` with `<path>` elements). The SplashScreen Canvas code is the reference — translate each `drawArc`, `drawRoundRect`, `drawCircle` call into equivalent SVG path data.

Key Canvas → Vector translations:
- `drawRoundRect(rect, rx, ry, paint)` → `<path>` with rounded rect path data, or use `<shape>` drawable
- `drawArc(oval, startAngle, sweepAngle, false, paint)` → `<path>` arc command in SVG path data
- `drawCircle(cx, cy, r, paint)` → `<path>` circle or `<shape>` oval

The vector canvas is 108dp × 108dp (full adaptive icon canvas). Safe zone is centred 66dp × 66dp.

---

## File locations

```
app/src/main/res/
├── drawable/
│   └── ic_launcher_foreground.xml     ← new vector drawable
├── mipmap-anydpi-v26/
│   ├── ic_launcher.xml                ← adaptive icon (foreground + background)
│   └── ic_launcher_round.xml          ← same, round variant
├── mipmap-hdpi/
│   └── ic_launcher.png                ← legacy raster (replace existing)
├── mipmap-xhdpi/
│   └── ic_launcher.png
├── mipmap-xxhdpi/
│   └── ic_launcher.png
├── mipmap-xxxhdpi/
│   └── ic_launcher.png
└── values/
    └── ic_launcher_background.xml     ← <color name="ic_launcher_background">#1A2744</color>
```

`ic_launcher.xml` (adaptive):
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```

---

## Legacy PNG fallbacks

Generate raster PNGs from the vector at the correct densities:
- `mipmap-hdpi`: 72×72px
- `mipmap-xhdpi`: 96×96px
- `mipmap-xxhdpi`: 144×144px
- `mipmap-xxxhdpi`: 192×192px

Android Studio's Image Asset wizard can generate these automatically from the vector — CLI should use that approach rather than manually creating PNGs.

---

## Verification

After applying:
1. Build and run — launcher icon on the Pixel home screen shows TapLog brand icon
2. Long-press the icon — round variant (for launchers that use it) also shows correctly
3. Icon looks correct at small size — NFC arcs readable, not muddy
4. No `ic_launcher` reference errors in build output

---

## What does NOT change

- `SplashScreen.kt` — the Canvas drawing code stays as-is; the app icon is a separate asset
- All Kotlin files
- Room schema
- Backend
- `AndroidManifest.xml` icon references (already point to `@mipmap/ic_launcher`)
