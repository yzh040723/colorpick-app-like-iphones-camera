# ColorPick UI Design System

## Track

**Branded native Android (Jetpack Compose)** with an iOS-native camera visual language. The app deliberately mimics the iOS Camera's spatial hierarchy, flat chrome, and white-on-black control vocabulary while staying on Android's Compose rendering stack.

## Visual Thesis

A single, unobstructed viewfinder is the hero; every control is a thin, white glyph that only reveals a yellow accent when it is the active subject, keeping the interface as quiet as the camera hardware itself.

## Content Plan

1. **Hero — aspect-ratio viewfinder**  
   The viewfinder is centered in the remaining space between the top and bottom bars. Aspect-ratio masks are the only framing; no cards, no chrome inside the image area.
2. **Support — top control strip**  
   Flash, zoom / lens selector, and the more / Style entry sit at the top as a row of small rounded-square glyphs on a semi-transparent black background.
3. **Detail — floating overlays**  
   Focus brackets, exposure slider, zoom presets, and filter cards appear only when the user is actively controlling them.
4. **Final CTA — bottom capture panel**  
   A fixed-height bottom bar holds the shutter / filter controls and album thumbnail; it is visually anchored and never resized by aspect-ratio changes.

## Interaction Thesis

- **Instant, not bouncy.** State changes use short `tween(150)` fades and scales; no spring overshoot on controls.
- **Touch-first direct manipulation.** Tap to focus, long-press to lock, drag the exposure slider, pinch to zoom, swipe to switch filters or dismiss sheets.
- **Contextual reveal.** Secondary controls (six-dot menu, aspect-ratio selector, zoom presets) slide in from the nearest edge and leave when the gesture ends.

## Color

| Token | Value | Usage |
|-------|-------|-------|
| `Background` | `#000000` | Viewfinder fill, default canvas |
| `ControlDefault` | `#FFFFFF` | Icons, labels, borders, focus brackets |
| `ControlActive` | `#FFD60A` | Selected state, locked focus, active accent |
| `Panel` | `#000000` | Top bar, bottom bar, menus, glass filter cards |
| `Mask` | `Black @ 55%` | Outside aspect-ratio window in filter mode; solid black in default mode |

## Typography

- **Headline / labels:** Roboto Flex or system default, `FontWeight.Medium`, white.
- **Mode labels:** 13–15 sp, medium weight, default white / active yellow.
- **Zoom ratio / filter name badges:** 14 sp, medium weight.

## Shape

- **Cards, panels, menus, filter cards:** `RoundedCornerShape(25.dp)` (large radius for panels).
- **Small icon containers (48–64 dp):** `RoundedCornerShape(12–18.dp)` to stay square-with-rounded-corners, not circular.
- **Shutter:** circular; outer ring white, inner disc active yellow when capturing.

## Components

### Top Bar
- Row of rounded-square glyph buttons (40 dp).
- Default white; selected / active state uses yellow (#FFD60A).
- No titles, no text labels on the strip.
- Aspect-ratio button and six-dot menu button turn yellow while their menus are open.
- In filter selection mode, all controls except the six-dot menu are hidden; a live badge at the top center shows current brightness, saturation, and filter intensity values (e.g. "亮度 +20   饱和度 -15   强度 80").
- Top bar background is solid black; controls float above it at a fixed position so aspect-ratio changes only squeeze the background block, never the controls.
- Secondary menus (six-dot menu and aspect-ratio dropdown) extend downward from the top bar, starting at the same top edge (below the status bar) so they fully cover the top bar. Full-width card with solid black background and 25 dp rounded corners, no shadow, and no full-screen dim layer; tapping outside the card dismisses it.

### Zoom Preset Bar
- Visible in default camera mode and when filter cards are minimized in filter mode.
- Centered horizontally at the bottom of the viewfinder window.
- For 1:1 and 3:4 aspect ratios the bar follows the actual viewfinder bottom edge.
- For 9:16 and other long aspect ratios the bar is fixed at the same bottom height a 3:4 viewfinder would have, preventing overlap with the bottom controls.
- Shows three rounded-square presets: 0.6x, 1x, 2x.
- Active preset uses yellow text, 1.5 dp yellow border, and 22% yellow background.
- Swiping right on the preset bar expands a semi-circular zoom wheel for continuous adjustment.
- When current zoom exceeds 2x, the rightmost preset icon updates to the actual ratio (e.g. 3.2x).
- During pinch-to-zoom, the bar is replaced by a single fixed-centered badge showing the live zoom ratio; releasing restores the three-icon bar.
- Disabled presets are dimmed when the device zoom range does not support them.

### Square Color Palette
- Rounded square panel (25 dp radius) with a solid theme-color HSL gradient background that fills the entire panel, including the corners.
- The dot matrix is rendered inside a padded inner area so the gradient shows through the rounded corners.
- 11×11 grid of white dots (30% of cell size), centered in each cell.
- Center dot is a hollow ring (2 dp stroke) instead of solid.
- At rest, the joystick row and column are highlighted at 65% opacity, forming a subtle crosshair.
- While dragging, the two nearest rings of dots (Manhattan distance 1 and 2) scale up and glow, replacing the static crosshair.
- Joystick (draggable handle) is locked to the active grid dot center and scales from 1x to ~3.8x when dragging.
- HSL gradient background based on the active filter's theme color; saturation increases left-to-right, lightness decreases top-to-bottom.
- The dot matrix is visual feedback only; the underlying saturation and brightness values are continuous from -100 to +100.
- Tap anywhere on the palette to update the offset; tapping the center ring resets offsets.
- Internal padding keeps corner dots fully inside the 25 dp rounded corners.
- In filter mode the palette is sized at 100 dp so it sits beside the unified shutter button without overlapping.

### Bottom Control Panel
- Fixed 148 dp height, full-width, anchored at the bottom.
- Solid black background; controls float above it at a fixed position so aspect-ratio changes only squeeze the background block, never the controls.
- Default mode contains: album thumbnail (left), shutter (center), spacer (right).
- Filter mode contains: reset button (left), a center group with the square color palette and the unified shutter button side-by-side, and the close button (right). The horizontal intensity slider sits directly below the palette and shares the palette's theme-color HSL gradient as its track background.
- Height is constant across all aspect ratios; the palette is sized to stay inside the panel.

### Focus / Exposure Overlay
- Four-corner yellow/white bracket frame (80 dp).
- Tapping or long-pressing the viewfinder triggers a pop animation: frame appears at 1.3x and settles back to 1x with a smooth, low-bounce spring.
- Vertical white exposure slider to the right of the frame, flips to the left near edges.
- Locked state: frame and slider handle turn bright yellow and remain visible until the user taps elsewhere.
- Available in both default camera mode and filter mode; in filter mode the focus frame draws above the filter cards.

### Grid Overlay
- Toggle in the six-dot secondary menu.
- White lines at 33.3% / 66.6% horizontal and vertical.
- Line alpha 0.55, stroke 2 dp, drawn inside the viewfinder window only.

### Viewfinder / Live Preview
- The camera live feed is rendered only inside the viewfinder window.
- Default mode uses a solid black background outside the viewfinder.
- Filter mode uses a semi-transparent black mask (alpha 0.55) outside the viewfinder so the underlying UI remains visible but subdued.

### Filter Mode
- Screen is divided into three independent zones: top bar, viewfinder, and bottom bar.
- Filter mode is locked to 3:4 aspect ratio so the filter cards and bottom panel keep a stable layout.
- Filter mode is an overlay on top of the default camera UI. Borderless glass filter cards cycle horizontally across the viewfinder window with no scale animation.
- Filter cards use colorless frosted glass (neutral white/black alphas); the filter character comes from the graded image, not from tinted glass.
- The unified shutter button lives in the bottom bar beside the palette, instead of floating inside the viewfinder.
- Bottom bar contains: reset button (left), square color palette with intensity slider below it (center-left), unified shutter button (center-right), and close button (right).
- Full-screen filter cards provide a minimize button fixed at the bottom-right corner of the viewfinder; the button is rendered above the zoom bar so it remains tappable and does not move with the card slide animation.
- Minimized filter cards collapse into a 40 dp floating action button at the same bottom-right corner of the viewfinder; tapping it restores the full-screen cards with a slide-up animation.
- Swipe-down-to-dismiss is disabled; the close button is the explicit exit.
- The "standard" style does not apply a LUT and hides the palette / intensity slider, leaving only the shutter, reset and close buttons.
- The default selected filter on entering filter mode is "standard" (original image).

### Six-Dot Secondary Menu
- Slides up from bottom, no title.
- Two primary actions: **Style** (wordmark icon) and **Grid** toggle.
- **Style** shows a yellow border/background while filter mode is active.
- Tapping the active **Style** button closes filter mode and resets to standard.
- Action icons only, no sub-labels.
- A downward arrow at the top center hints that tapping outside dismisses the menu.

### Zoom Wheel
- A circular scale disk combined with a solid black rectangle that masks the lower half of the disk and covers the entire bottom control panel. The disk diameter, the rectangle width and the bottom bar width are all equal (full screen width); both shapes are solid black.
- Draw order: the circular disk, ticks and labels are drawn first (lower layer); the rectangle is drawn above them so it fully masks the disk's lower half.
- No white outline around the circle; only the protractor tick marks and radial labels are visible.
- The disk rotates as the user zooms: counter-clockwise while zooming in, clockwise while zooming out. The scale itself moves; the pointer stays fixed at the top center of the disk.
- The upper half of the disk acts as a protractor scale with tick marks and radial labels at 0.6x, 1x, 2x, 3.2x, 5x, and 10x, arranged left-to-right in clockwise order.
- Labels are radially aligned (clock-numeral style): each digit's baseline faces the disk center.
- A fixed yellow triangular pointer sits at the top center of the disk, pointing down at the current zoom position.
- A bright yellow live-ratio badge is fixed just above the pointer and updates in real time.
- The disk scale is fixed to the 0.6x–10x range; the maximum disk rotation angle corresponds exactly to the angle between the 0.6x and 10x marks.
- Drag horizontally across the preset bar to reveal the wheel: left zooms in, right zooms out (linear sensitivity).
- Hides immediately when the finger is released.
- Does not appear during pinch-to-zoom.

### Zoom Preset Bar
- Persistent three-icon bar (0.6x, 1x, 2x) centered above the bottom panel.
- Active preset uses yellow text, 1.5 dp yellow border, and 22% yellow background.
- Dragging left/right on the bar performs continuous zoom and immediately replaces the bar with the semi-circular zoom wheel.
- Tapping a preset animates the zoom ratio smoothly over ~200 ms instead of cutting instantly.
- During pinch-to-zoom, the three icons are replaced directly by a single live-ratio badge fixed at the bottom-center of the viewfinder; releasing restores the three-icon bar.

## Motion

- Entrance: `fadeIn(tween(150))` + `scaleIn(tween(150), initialScale = 0.95f)`.
- Exit: `fadeOut(tween(150))`.
- Aspect-ratio change: cross-fade the mask, no layout animation on the bottom bar.
- Filter card switch: horizontal pager snap, **no** per-card scale animation.

## Current Implementation Notes

- Filter pipeline no longer uses a 3D LUT. Instead, `CameraFrameRenderer` and `PhotoRenderer` apply a simple color-grading shader directly in RGB space: exposure, brightness, contrast, blacks, highlights/shadows, saturation, temperature/tint/warmth, hue shift, and vignette.
- All shader inputs are clamped to `[0, 1]` before and after grading to prevent channel overflow and color artifacts.
- Default mode and the "standard" style bypass all filter processing (`lutBitmap = null`, `lutMix = 0`), keeping the image clean.
- Square color palette applies saturation and brightness offsets on top of the selected filter's parameter set. The UI shows continuous values from -100 to +100, while the actual image effect is scaled by 0.5 for saturation and 0.35 for brightness to keep adjustments subtle and avoid clipping. The 11×11 dot matrix is visual feedback only.
- The palette and intensity slider are hidden for the "standard" style.
- Focus / exposure overlay uses a stable gesture detector: `cameraControl`, `previewSize`, and `isFocusLocked` are wrapped in `rememberUpdatedState` so the detector is not recreated on every state change, preventing missed taps.
- Zoom control combines three interactions:
  - Persistent bottom preset bar with 0.6x / 1x / 2x icons, anchored to the bottom of the viewfinder window (fixed at the 3:4 bottom line for long aspect ratios). Tapping a preset plays a 200 ms tween animation that synchronizes the UI badge and the camera zoom ratio.
  - Left/right drag on the preset bar expands a circular zoom wheel masked by a semi-transparent black rectangle that covers the bottom panel. The circular scale disk rotates counter-clockwise while zooming in and clockwise while zooming out; a fixed yellow pointer at the top center indicates the current zoom, and a yellow live-ratio badge floats above it. The scale is fixed to 0.6x–10x with protractor-style labels at 0.6x, 1x, 2x, 3.2x, 5x, and 10x. The wheel hides immediately on release.
  - Pinch-to-zoom on the viewfinder; during pinch the preset bar is replaced by a single live-ratio badge that is rendered as a fixed-centered overlay (no icon-merging animation).
- A black lens-switch mask (50ms fade in / 300ms fade out, ~700ms hold) covers the screen while the camera switches between front and back, hiding any brief freeze or orientation flip. A flipping camera-switch icon animates in the center so the mask is not a plain black screen.
- Front-camera rotation is inverted relative to the back camera before being passed to the renderer, preventing upside-down preview.
- `CameraControls` uses a full-screen `Box` with floating top/bottom controls and a centered viewfinder. Solid black viewfinder masks provide the top/bottom bar backgrounds; because the controls float independently, aspect-ratio changes only squeeze the mask blocks, never the controls.
- ImageCapture uses a `ResolutionSelector` that prefers the device's maximum 4:3 resolution, matching the preview aspect ratio so that the saved photo is cropped from the same field of view.
- HDR has been removed to avoid capture freezes.
- Grid overlay toggle lives in the six-dot secondary menu.

## Open Items / Next Iterations

- Predictive-back wiring and full Material 3 Expressive motion scheme.
- Support loading external professional 3D LUT files (.cube format).
