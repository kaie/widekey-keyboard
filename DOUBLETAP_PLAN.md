# Double-Tap Keyboard — Implementation Plan

## Motivation

Users with large fingers on small screens struggle to hit the correct key accurately.
This feature replaces the standard keyboard with wider keys that each show two characters.
A single tap produces the left character (after a short delay); a double tap produces the
right character. No dictionary assistance or autocorrect is involved.

---

## Core Interaction Model

- Every letter key is **double-width** and displays **two characters**: left and right.
- **Single tap**: after ~333 ms with no second tap, the left character is committed.
- **Double tap**: a second tap on the same key within ~333 ms commits the right character immediately (cancels the pending timer).
- If the user taps a *different* key before the timer expires, the first key's left character is committed immediately, and the new key starts its own timer.
- The 333 ms timeout is a configurable setting.
- **Key preview popup**: shows the left character optimistically on first tap; updates or dismisses on second tap.
- **Sound/vibration**: fires on first tap only (not again on commit).

---

## Layout Rules

### General principles
- Shift and Delete keys are never modified — they always keep their current size and position.
- All character buttons within a row are **equally wide** (row width ÷ number of buttons).
- Keys are paired **left-to-right** within each row.
- Corner hint labels (keyHintLabel) are **never shown** in this layout.
- Long-press popups for accented characters (moreKeys) are **preserved** and their keys made wider for easier selection.

### Row 1
Always has 10 keys → 5 equal pairs at 20% each. No special handling needed.

### Rows 2 and 3 — the odd-count rule

When a row has an odd number of keys it would produce one solo (single-character) button.
When **both** row 2 and row 3 are odd, one character is moved between them to make both
even. The direction is chosen to **maximise the minimum button width** across both rows,
accounting for the fact that row 3 has shift (14%) and delete (14%) already occupying 28%,
leaving only 71% for letter buttons, while row 2 uses the full 100%.

**Formula:**
- Move UP (row3→row2): min( 100÷((r2+1)/2),  71÷((r3-1)/2) )
- Move DOWN (row2→row3): min( 100÷((r2-1)/2),  71÷((r3+1)/2) )
- Choose the direction with the larger result.

**Results by layout family:**

| Layout family | Row2 count | Row3 count | Direction | After: Row2 | After: Row3 | Min width |
|---|---|---|---|---|---|---|
| QWERTY / QWERTZ / ABC | 9 | 7 | **Move UP** | 5 pairs @ 20.0% | 3 pairs @ 23.7% | 20.0% |
| Nordic / Swiss / Uzbek | 11 | 7 | **Move DOWN** | 5 pairs @ 20.0% | 4 pairs @ 17.8% | 17.8% |
| Serbian QWERTZ / Turkish Q / Turkish F | 11 | 9 | **Move UP** | 6 pairs @ 16.7% | 4 pairs @ 17.8% | 16.7% |
| Azerty / Spanish / Colemak / Dvorak / Workman / Ergol / HCesar | 10 (even) | 7 (odd) | No move | 5 pairs @ 20.0% | 3 pairs + 1 solo @ 17.8% | 17.8% |
| Bépo | 10 (even) | 8 (even) | No move needed | 5 pairs @ 20.0% | 4 pairs @ 17.8% | 17.8% |

When only row 3 is odd (no move possible), the last key in row 3 becomes a solo
single-character button at the same width as the paired buttons — it simply has no
second character and commits immediately without delay.

The stagger offset on row 2 (currently 5%) is removed — with wider keys the stagger
serves no purpose and wastes space.

### Specific character moves per layout family

| Layout | Move | Resulting rightmost pair |
|---|---|---|
| QWERTY / QWERTZ / ABC | m ↑ to row 2 | l·m |
| Nordic / Swiss | locale₁₁ ↓ to row 3 | m·locale₁₁ |
| Uzbek | ʼ ↓ to row 3 | m·ʼ |
| Serbian QWERTZ | ž ↑ to row 2 | ć·ž |
| Turkish Q | ç ↑ to row 2 | i·ç |
| Turkish F | x ↑ to row 2 | ş·x |

---

## German QWERTZ Pairing (primary target layout)

```
Row 1:  q·w   e·r   t·z   u·i   o·p
Row 2:  a·s   d·f   g·h   j·k   l·m   ← m moved up from row 3
Row 3:  [shift]  y·x   c·v   b·n   [delete]
```

Umlauts (ä, ö, ü, ß) remain accessible via long-press popups, unchanged.

---

## Settings

### Number row toggle (`pref_show_number_row`)
Kept as-is. When disabled, the digit hint labels that normally appear in the key corners
are simply not shown (we remove all corner hint labels anyway). Users who need digits
use the number row or switch to the symbols view.

### Special chars toggle (`pref_show_special_chars`)
Kept as-is. In our layout, the corner hint label effect is irrelevant (labels are
never shown). The toggle continues to enrich long-press popup content, which still works.

### All other settings
Unaffected: themes, height, vibration, sound, long-press timeout, space/delete swipe,
language switch key.

---

## Symbol Views

Both symbol views (`kbd_symbols.xml` / `kbd_symbols_shift.xml`) are **shared across all
languages** (single file set). Only tiny locale exceptions exist for Farsi/Urdu digits.
The same double-tap pairing and commit delay applies in symbol views.

### Primary symbols view

```
Row 1 (5 pairs):  1·2    3·4    5·6    7·8    9·0
Row 2 (5 pairs):  @·*    #·$    %·&    -·+    (·)
Row 3 (3 pairs):  "·'    :·;    !·?
```

Hand-tuned: `*` moved from row 3 to after `@` in row 2 (making both rows even),
so that `(·)` stay together, and row 3 gains three natural semantic pairs.

### Symbols-shifted view

```
Row 1 (5 pairs):  ~·`    |·•    √·π    ÷·×    ¶·∆
Row 2 (5 pairs):  ¤₁·¤₂  ¤₃·¤₄  ^·°    =·℅   {·}
Row 3 (3 pairs):  \·©    ®·™    [·]
```

Hand-tuned: `℅` moved from row 3 to between `=` and `{` in row 2, so that `{·}` and
`[·]` each remain as self-contained pairs.

---

## Row 4 — Spacebar Row

**Current structure** (with language switch key active):
```
[?123 · 15%]  [, · 10%]  [🌐 · 10%]  [space · 40%]  [. · 10%]  [enter · 15%]
```

### Comma + Period merged into a doubletap pair

The `,` and `.` keys are currently 10% wide each — as small as letter keys, the same
accuracy problem we are solving. They are merged into a single doubletap key occupying
the period's current position (right of space bar):

- **Single tap** → `,` (comma) — the more frequent mid-sentence character
- **Double tap** → `.` (period) — naturally follows a pause at sentence end
- Same 333 ms timeout rule as letter keys
- Long-press popup preserved (comma moreKeys: quotes, punctuation variants)
- The standalone comma key on the left of the space bar is removed

### Functional key widths

All functional keys are bumped up from their current sizes for better accuracy.
Exact values to be tuned during implementation; indicative targets:

| Key | Current | Target |
|---|---|---|
| ?123 (symbols toggle) | 15% | ~18% |
| 🌐 (language switch) | 10% | ~12% |
| ,. (merged comma-period) | — (was 10%+10%) | 20% |
| enter | 15% | ~18% |
| space | 40% (with globe) / 50% (without) | adjusts to fill remaining width |

With globe enabled:  18% + 12% + space + 20% + 18% = 100% → space = **32%**
Without globe:       18% + space + 20% + 18% = 100% → space = **44%**

The space bar shrinks slightly compared to today but remains the dominant key in the row.

---

## Implementation Touchpoints

### New XML layout files
- `rows_doubletap_qwertz.xml` + `rowkeys_doubletap_qwertz{1,2,3}.xml` — German starting point
- `kbd_doubletap_qwertz.xml`
- `keyboard_layout_set_doubletap_qwertz.xml`
- New symbol row files: `rows_symbols_doubletap.xml` etc. (or modify pairing in-place)
- Eventually one doubletap variant per supported layout family

### `Key.java`
- Add `mSecondaryCode` (int) field for the right-character code point.
- Add `mSecondaryLabel` (String) for display.
- Parse new XML attribute `latin:keySecondarySpec`.

### `KeyboardBuilder.java`
- Parse `latin:keySecondarySpec` attribute into `Key.mSecondaryCode` / `mSecondaryLabel`.

### `TimerHandler.java`
- Add `MSG_PENDING_SINGLE_TAP` message type.
- `startSingleTapTimer(tracker, delay)` — fires after the configurable timeout.
- `cancelSingleTapTimer(tracker)` — called on second tap or different-key tap.

### `PointerTracker.java`
- On `ACTION_UP`: if the key has a secondary code, start the single-tap timer instead
  of calling `detectAndSendKey` immediately.
- On second `ACTION_DOWN` on the same key within timeout: cancel timer, commit secondary code.
- On `ACTION_DOWN` on a *different* key within timeout: cancel timer, commit primary code
  of the previous key immediately, then start normal handling for the new key.
- Solo keys (no secondary code): commit immediately with no delay, as today.

### `KeyboardView.java`
- `onDrawKeyTopVisuals()`: for paired keys, render left character left-aligned and right
  character right-aligned within the key, with a faint vertical divider in the centre.
- No corner hint labels rendered anywhere in this layout.

### Long-press popup keys (`MoreKeysKeyboard`)
- Key width in the popup is increased (exact value TBD, roughly 1.5× current) so that
  accented characters are easier to select.

### Settings
- Add a `pref_doubletap_timeout` slider (default 333 ms, range ~150–500 ms).
- The new layout appears as a selectable subtype ("German — Double-Tap" etc.).

---

## Roll-out Strategy

1. Implement German QWERTZ double-tap layout end-to-end (proof of concept).
2. Implement symbol views (shared, applies to all languages at once).
3. Generalise: apply pairing rules algorithmically or via per-family XML files for
   remaining Latin layouts (QWERTY, Nordic, Swiss, Spanish, Colemak, etc.).
4. Non-Latin scripts: deferred — requires per-script analysis.
