---
name: generate-app-icons
description: Create minimalist app icons for this Android app — vector drawables (default), adaptive launcher icons with a themed monochrome layer, or multi-density WebP rasters via Python+PIL. Use when asked to create, design, or add new icons for navigation items, cards, buttons, the launcher, or any other UI element in this project.
---

# Generate App Icons

This project is Jetpack Compose + Material3 with minSdk 26. That means:

- `VectorDrawable` works natively — no `vectorDrawables.useSupportLibrary`, no
  PNG fallbacks needed.
- Adaptive launcher icons (`<adaptive-icon>`) are supported on every device, so
  no legacy per-density launcher PNGs are required. A `<monochrome>` layer
  (themed icons) is honored on API 33+ and ignored below.
- Icons are consumed from Compose (`Icon`, `painterResource`,
  `ImageVector`), not from XML layouts.

## Decide: Material icon vs. vector XML vs. raster

Pick top to bottom, stopping at the first that fits:

1. **Existing Material icon** — if `androidx.compose.material.icons.Icons`
   already has a matching glyph, use it. Only `Icons.Default.*` and the core
   set ship with `material3`; the extended set needs the
   `material-icons-extended` dependency (large; with R8 disabled in release
   builds here, prefer a custom vector over adding it for one or two icons).
   Adding a dependency means following the "When adding new libraries" rule in
   `CLAUDE.md`.
2. **Vector XML** (default for custom icons) — one file in
   `app/src/main/res/drawable/`, scales to any density, tints for free, and
   supports light/dark by tint. Use for anything expressible as paths: lines,
   rounded rects, circles, polygons, arcs.
3. **Raster WebP via PIL** — only for artwork that is painful as path data
   (organic shapes, textures, illustrations) or when explicitly asked for
   generated raster icons. Not for ordinary UI glyphs.

## Vector XML workflow (default)

1. 24x24dp viewport, matching Material icons:

   ```xml
   <vector xmlns:android="http://schemas.android.com/apk/res/android"
       android:width="24dp"
       android:height="24dp"
       android:viewportWidth="24"
       android:viewportHeight="24"
       android:tint="?attr/colorControlNormal">
       <path
           android:fillColor="@android:color/white"
           android:pathData="..." />
   </vector>
   ```

   Draw in a single opaque color (white or black — it is replaced by the
   tint). Keep ~2dp padding inside the viewport (live area 20x20).
2. Prefer **filled** paths. For outlined shapes either use `strokeColor` +
   `strokeWidth="2"` + `strokeLineCap="round"` + `strokeLineJoin="round"`, or
   convert strokes to fills; stay consistent with the rest of the icon set.
3. API 26 gives you more than API 19 did — use it when it simplifies the icon:
   - `<group>` transforms (rotate/scale/translate) to reuse one path.
   - `<clip-path>` for cut-outs.
   - `android:fillType="evenOdd"` for holes.
   - Color-state or theme attrs (`?attr/colorControlNormal`,
     `?attr/colorPrimary`) as `fillColor` / `tint`.
   - `android:autoMirrored="true"` for directional icons (back, next) so they
     flip in RTL.
4. **Naming**: `ic_<feature>_<name>.xml`, snake_case, e.g. `ic_nav_home.xml`,
   `ic_action_add.xml`. Nothing outside `drawable/` — no density folders.
5. Optionally preview a vector by importing it into Android Studio's
   Resource Manager, or by rendering a PIL mock of it (see below).

### Using the icon from Compose

```kotlin
Icon(
    painter = painterResource(R.drawable.ic_nav_home),
    contentDescription = stringResource(R.string.nav_home), // null if decorative
)
```

`Icon` tints with `LocalContentColor` by default, so the icon follows the
Material3 theme (light/dark, primary vs. non-primary, destructive/error
colors). Do not hardcode colors in the drawable or at the call site except to
express meaning (e.g. `MaterialTheme.colorScheme.error` for destructive
actions). Always provide a `contentDescription` for informative icons; use
`null` only when adjacent text already says the same thing.

For bottom-nav / `NavigationBarItem` / `NavigationRail`, pass the `Icon` via
the `icon` slot — selected-state color comes from the component.

## Adaptive launcher icon

Files (already scaffolded by the template; edit rather than recreate):

- `res/mipmap-anydpi/ic_launcher.xml` and `ic_launcher_round.xml`
- `res/drawable/ic_launcher_background.xml`
- `res/drawable/ic_launcher_foreground.xml`

Rules:

- Canvas is **108x108dp**. The launcher masks to circle/squircle/etc., so keep
  all essential artwork inside the centered **66x66dp safe zone**
  (viewport coords 21–87 when `viewportWidth=108`). The outer 18dp each side
  may be cropped or animated.
- **Background**: a flat color or very simple shape; no detail.
- **Foreground**: the glyph only, transparent elsewhere. No drop shadows —
  the launcher adds them.
- **Monochrome** (themed icons, API 33+): add
  `<monochrome android:drawable="@drawable/ic_launcher_foreground" />` (or a
  dedicated single-color drawable if the foreground uses multiple colors).
  The system tints it with the user's wallpaper palette, so the shape must
  read as a silhouette.
- Don't generate legacy `mipmap-*dpi` PNG/WebP launcher icons for new work;
  at minSdk 26 they are dead weight. If the template ships them, they may be
  deleted together with confirming the build still passes.
- Play Store icon (512x512 PNG) is a store asset, not a resource — export it
  separately if/when publishing; it is not part of the APK.

## Raster WebP workflow (fallback)

Use `scripts/gen_icons.py` as the template:

1. Copy it to the scratchpad, replace the `ICONS` dict with
   `draw_xxx(draw)` functions drawing in a 24x24dp viewport (helper `d(v)`
   converts dp to the supersampled canvas).
2. Draw at 20x supersampling in opaque black on transparent, then Lanczos
   downscale. Never draw directly at 24px.
3. Run it from the repo root. It writes lossless **WebP** (smaller than PNG,
   fully supported at minSdk 26) to
   `app/src/main/res/drawable-<density>/<name>.webp` for
   mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi (1.0/1.5/2.0/3.0/4.0 x 24dp =
   24/36/48/72/96 px). Pass `--png` to write PNG instead.
4. **Preview** with `--preview <path.png>`: composites the xxxhdpi icons on a
   white sheet, upscaled with NEAREST. `Read` the sheet to check shapes before
   wiring anything in.
5. Reference via `painterResource(R.drawable.ic_x)` and tint with
   `Icon(...)` as above (the opaque-black source makes the tint reliable). Use
   `Image(...)` instead only if the raster is intentionally multi-colored.

Naming is the same as for vectors; keep the identical base name across all
density folders.

## After generating

- Run `./gradlew assembleDebug testDebugUnitTest` — a drawable-only change
  needs no new unit tests (nothing to fail first), but the build must pass and
  resource references must resolve.
- If the icon is wired into UI (a new screen element, a nav item), that part
  is a feature change and follows the TDD and manual-testing-checklist rules in
  `CLAUDE.md`.
- `git status --short` to confirm only the intended resource files and
  call sites changed.
- Commit as `feat(ui): ...` / `chore(icons): ...` (Conventional Commits).
