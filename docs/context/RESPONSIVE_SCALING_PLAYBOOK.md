# Responsive Scaling Playbook — Handoff Notes

> Written 2026-08-19 for a parallel Claude Code session working on a
> similar frontend stack (SvelteKit + Tailwind CSS v4, or anything
> using Tailwind's `@theme`-based breakpoint system). Grounded in a
> real, multi-round debugging session on this project — every claim
> below is backed by an actual commit in this repo (`798f0ae`,
> `be74ea8`, `480347e`, `26027ec`), not general advice. Portable: copy
> this file into any project hitting the same class of problem.
>
> Not a ticket. This is a durable lesson doc, same spirit as
> `KNOWN_ISSUES.md` in this project but focused enough to hand to
> another session standalone.

## The symptom that starts this whole story

"Text looks small on a real desktop monitor" — reported repeatedly,
even after confirming the CSS already used `rem` everywhere (verified
via `grep` — zero `px` font-sizes in the codebase). **Unit choice was
never the bug.** `1rem` and `16px` render identically at default
browser settings; rem's actual benefit is respecting the user's
browser zoom/font-size preference, not making text bigger by default.

The real cause: Tailwind's named breakpoints (`sm:`, `md:`, `lg:`,
`xl:`, `2xl:`) have **no upper bound**. `lg:text-[1.625rem]` means
"from 1024px to infinity" — a value tuned by eyeballing a 1440px test
viewport stays exactly that size all the way to a 2560px or 3840px
monitor. The text isn't small in absolute terms; it's small
*relative to how much screen surrounds it* at a width nobody tested.

**Fix direction:** a real breakpoint ladder — several explicit steps
between "desktop" and "very wide desktop" — not one flat value past
`lg:`.

## THE gotcha — read this before touching Tailwind's `@theme` breakpoints

This is the one mistake worth preventing in a parallel session, because
it was hit **twice** here before the pattern was understood.

**Tailwind v4 does not merge named breakpoints (`sm:`/`md:`/`lg:`/
`xl:`/`2xl:`) and custom `--breakpoint-*` additions (declared in
`@theme`) into one pixel-sorted list.** Named breakpoints get a fixed
emission slot in the generated CSS regardless of their pixel value;
custom breakpoints are grouped separately. If you mix a named prefix
and a custom one on the **same element/property**, whichever rule
Tailwind happens to emit later in the stylesheet wins the cascade at
*every* viewport width both rules could apply to — regardless of which
breakpoint is actually narrower or wider. The named one silently
overrides the custom one (or vice versa) with no error, no warning,
and a computed style that looks plausible until you check the actual
number.

```css
/* @theme block */
--breakpoint-1366: 1366px;
--breakpoint-1440: 1440px;
--breakpoint-1920: 1920px;
--breakpoint-2560: 2560px;
--breakpoint-3840: 3840px;
```

```html
<!-- WRONG — mixes named lg: with custom breakpoints on one element.
     At 1920px, BOTH `lg:` (1024+) and `1920:` match — which one wins
     is determined by Tailwind's internal emission order, NOT by which
     breakpoint is numerically closer to the current width. -->
<h1 class="text-4xl lg:text-6xl 1920:text-[4.5rem] 2560:text-[4.875rem]">

<!-- RIGHT — every step in the ladder is a custom breakpoint, nothing
     named mixed in. Even the smallest step (a `sm:`-equivalent) gets
     its own custom breakpoint (--breakpoint-640) rather than reusing
     Tailwind's built-in `sm:`, so there's no named/custom collision
     anywhere in the ladder. -->
<h1 class="text-4xl 640:text-[3rem] 1024:text-[3.75rem] 1366:text-[4rem] 1920:text-[4.5rem] 2560:text-[4.875rem]">
```

**How this was actually diagnosed** (don't skip this step next time —
guessing at the cause from computed styles alone was not enough):
`fetch()` the compiled CSS in the live browser and read the raw text —
specifically check the **source order** of the `@media (min-width: …)`
blocks and which selector sits in which block. Computed-style checks
alone (`getComputedStyle(el).fontSize`) only tell you the *final*
number, not *which rule produced it* or *why a rule you expected to
win didn't*.

```js
const res = await fetch('/src/routes/layout.css'); // or wherever the stylesheet is served from in dev
const text = await res.text();
// look for the actual generated @media blocks and class selectors,
// in the order they appear — that IS the cascade priority.
```

## The fix that actually held: plain CSS, not repeated Tailwind arbitrary values

The first fix (put every ladder step as a Tailwind arbitrary-value
class directly in the markup, `1366:text-[1.6875rem] 1440:text-[…] …`)
worked for one element, but repeating a 6-breakpoint list by hand
across 15+ files is exactly how the named/custom mixing mistake
happened a second time (a leftover `sm:`/`lg:` in one file, custom
breakpoints in another). It was also just a lot of retyping the same
six numbers over and over.

**What replaced it:** a small number of reusable, semantically-named
CSS classes, written as **plain CSS with hand-ordered
`@media (min-width: …)` blocks** — deliberately *not* going through
Tailwind's variant system for this at all.

```css
/* layout.css — one definition, applied everywhere via a shared class
   name. Ascending source order by hand = the cascade does exactly
   what's written; nothing here is auto-generated or auto-sorted by a
   framework, so the named/custom mixing bug is structurally
   impossible — there's no "named breakpoint" in the picture anymore. */

.page-title {
  font-size: 1.875rem; /* mobile/base */
}
@media (min-width: 640px)  { .page-title { font-size: 2.25rem; } }
@media (min-width: 1024px) { .page-title { font-size: 2.5rem;  } }
@media (min-width: 1366px) { .page-title { font-size: 2.625rem; } }
@media (min-width: 1440px) { .page-title { font-size: 2.75rem; } }
@media (min-width: 1920px) { .page-title { font-size: 3rem;    } }
@media (min-width: 2560px) { .page-title { font-size: 3.25rem; } }
@media (min-width: 3840px) { .page-title { font-size: 3.75rem; } }
```

Applied via an ordinary class (`<h1 class="page-title font-display …">`),
same everywhere it's used. One place to tune later, not fifteen.

**Why these specific breakpoints (1024 / 1366 / 1440 / 1920 / 2560 /
3840):** real, common Windows/monitor widths, not arbitrary round
numbers — 1366 is the most common laptop width for years (WXGA), 1440
is also a common laptop/monitor width (and the MacBook Air 13"
logical width), 1920 is the most common external monitor (Full HD),
2560 is QHD, 3840 is 4K. Named literally by pixel width in the
`@theme` tokens and in the CSS class media queries — not by a category
label like "laptop"/"desktop" — so there's no naming judgment call to
argue about later; the number *is* the breakpoint.

## Verification checklist — do all of this, not a subset

1. **Sweep every breakpoint the ladder covers, not just the one width
   you're actively testing at.** A fix confirmed correct at 1440px can
   still be silently broken at 1920px or 3840px if the underlying bug
   is the named/custom mixing issue above — that bug specifically
   manifests as "looks right at the width you happened to check,
   wrong everywhere else."
2. **`getComputedStyle(el).fontSize` proves the CSS rule resolved to a
   number — it does NOT prove the element is actually visible.** A
   separate real bug in the same project (unrelated to fonts): an
   opaque wrapper `<div>` was painting over a `z-index: -1` background
   layer — the layer's CSS was completely correct and its computed
   style checked out, but a sibling/ancestor painted over it in normal
   document flow. If a value "looks right" in computed style but the
   human report says it's not visible, check paint order /
   `document.elementsFromPoint()` at that spot, don't just re-trust the
   computed-style number.
3. **When a number doesn't match what you expect, fetch the actual
   compiled CSS text and read the media-query source order directly**
   (see the diagnosis snippet above) rather than guessing from the
   class list in the markup. The markup can look completely correct
   while the generated CSS still has the named/custom bug baked in.
4. **Check the un-changed end of the range too.** Confirm mobile/tablet
   sizing is byte-for-byte unchanged after a desktop-focused fix — a
   ladder that starts its custom breakpoints too early can
   accidentally shift the base/mobile size as a side effect.

## Consistency-of-tiers checklist

Decide up front which elements get a deliberately larger "hero" tier
vs. the standard "every other page's heading" tier — and don't let it
leak by copy-paste. A real regression here: a secondary page's `<h1>`
was put on the same oversized "hero" class as the actual homepage hero
section. Each page's *own* sizing looked internally consistent — but
navigating from that page to any other inner page produced a visible,
jarring jump in heading size for no reason a visitor could explain.
**A heading that's individually "correct" can still be a bug if it's
inconsistent with the equivalent heading on every other page of the
same kind.** Reserve the largest tier for the one place it's a
deliberate, singular design moment (a true landing hero) — everything
else that's "a page's title" should share one plain, consistent class,
full stop.

## Rollout-completeness checklist

A "site-wide" pass can still be incomplete on the first attempt. Here,
the first rollout correctly covered every heading (h1/h2/h3-equivalent
titles) across every page — but missed the **body/lead paragraph**
text using the exact same flat, small pattern (list-card descriptions,
detail-page body copy) on several pages, even though the shared class
for that role already existed and was already correctly applied
elsewhere. The gap was caught by grepping for the *old* pattern being
replaced (e.g. `text-sm`/`text-lg` on description-shaped elements)
across the whole route tree after the "complete" pass, not by
assuming the first sweep caught everything.

**Practical step:** after applying a new scaling class site-wide,
`grep` the routes directory for the flat class(es) it replaced, on
elements playing the same semantic role, and check each hit
individually — don't rely on memory of "which pages I already did."

## Explicit scope guardrails — what deliberately does NOT get this treatment

- **Compact, card-style pages** (login forms, narrow modal-like
  layouts) — scaling their headings to full-page-title or hero size
  looks wrong inside a constrained card; leave them at their existing
  fixed size.
- **Dense body/legal text** (long-form policy pages, disclaimers) —
  only the page's own `<h1>` needs the ladder; walls of paragraph text
  don't need hero-scale enlarging, and in some projects that text is
  deliberately frozen/unstyled for compliance reasons — check before
  touching it.
- **Meta/label lines** (dates, prices, venue names, status badges) —
  these are deliberately smaller/secondary by design, a different
  semantic role from actual body copy. Don't fold them into a
  "make everything bigger" pass just because they're nearby.

## Environment caveat (may not apply to your session — check first)

If your session's browser-preview tool cannot actually composite/
render visual frames (a screenshot call errors instead of returning an
image), computed-style checks via `getComputedStyle()` are a legitimate
substitute for verifying the *mechanism* is correct (right rule, right
value, right breakpoint), but they are **not** a substitute for
verifying something actually *looks* good (contrast, spacing,
whether a value that's numerically correct still reads well to a
human eye). Say so explicitly when reporting back — "confirmed via
computed style, not visually confirmed" — rather than implying a
visual claim you can't actually back up.
