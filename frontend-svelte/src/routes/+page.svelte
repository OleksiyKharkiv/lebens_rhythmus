<script lang="ts">
	import { resolve } from '$app/paths';
	import * as m from '$lib/paraglide/messages.js';

	const directions = [
		{ title: m.home_direction_theatre, desc: m.home_direction_theatre_desc },
		{ title: m.home_direction_dance, desc: m.home_direction_dance_desc },
		{ title: m.home_direction_gymnastics, desc: m.home_direction_gymnastics_desc }
	];
</script>

<svelte:head>
	<title>{m.site_name()} — {m.home_hero_title()}</title>
</svelte:head>

<section class="mx-auto grid max-w-5xl items-center gap-12 px-6 py-16 sm:py-24 lg:grid-cols-2 lg:gap-16">
	<div>
		<!-- Beta feedback 2026-08-19 — sm:/lg: are a low bar; a size tuned
		     for a 1024-1440px test viewport can still read as small on a
		     genuinely wide monitor, since nothing scaled beyond lg: (no
		     upper bound on a Tailwind breakpoint). Full ladder now (see
		     layout.css's --breakpoint-1366 etc.) instead of one flat lg:
		     value — mobile/tablet (base/sm:) stays exactly as it was. -->
		<h1
			class="hero-text font-display text-4xl leading-tight font-semibold text-paper 640:text-[3rem] 1024:text-[3.75rem] 1366:text-[4rem] 1440:text-[4.25rem] 1920:text-[4.5rem] 2560:text-[4.875rem] 3840:text-[5.5rem]"
		>
			{m.home_hero_title()}
		</h1>
		<p
			class="hero-text mt-6 max-w-md text-lg text-paper-dim 1024:text-[1.25rem] 1366:text-[1.3125rem] 1440:text-[1.375rem] 1920:text-[1.5rem] 2560:text-[1.625rem] 3840:text-[1.875rem]"
		>
			{m.home_hero_tagline()}
		</p>
		<a
			href={resolve('/login')}
			class="mt-10 inline-block rounded-full bg-gold px-8 py-3.5 font-display font-semibold text-ink transition-colors hover:bg-gold-deep"
		>
			{m.home_cta_register()}
		</a>
	</div>

	<!-- Placeholder hero visual — swap for Olena's real photo/video once
	     content is supplied (LR-ADR-019: she maintains content herself). -->
	<div class="relative aspect-square w-full max-w-md justify-self-center lg:justify-self-end">
		<div
			class="hero-glow absolute inset-0 rounded-[2.5rem]"
			style="background: radial-gradient(circle at 30% 30%, var(--color-teal) 0%, transparent 55%),
			                    radial-gradient(circle at 75% 70%, var(--color-gold) 0%, transparent 55%),
			                    var(--color-ink-soft);"
		></div>
		<div class="absolute inset-0 rounded-[2.5rem] border border-ink-line"></div>
	</div>
</section>

<section class="border-t border-ink-line bg-ink-soft/40 px-6 py-16 sm:py-20">
	<div class="mx-auto max-w-5xl">
		<h2 class="font-display text-2xl font-semibold text-paper sm:text-3xl">
			{m.home_directions_title()}
		</h2>
		<div class="mt-10 grid gap-6 sm:grid-cols-3">
			{#each directions as d (d.title)}
				<div class="rounded-2xl border border-ink-line bg-ink px-6 py-8 transition-colors hover:border-gold/60">
					<h3 class="font-display text-xl font-semibold text-teal">{d.title()}</h3>
					<!-- Direct request 2026-08-19 — +0.5rem (0.875rem -> 1.375rem)
					     as the base/mobile-through-1024 size (unchanged, this
					     element had no breakpoint at all before, flat
					     everywhere), then the same breakpoint ladder as the
					     rest of the page from 1024px up (layout.css). -->
					<p
						class="mt-3 text-[1.375rem] leading-relaxed text-paper-dim 1366:text-[1.4375rem] 1440:text-[1.5rem] 1920:text-[1.625rem] 2560:text-[1.75rem] 3840:text-[2rem]"
					>
						{d.desc()}
					</p>
				</div>
			{/each}
		</div>
	</div>
</section>

<style>
	/* 2026-08-17, beta feedback (round 2) — the first version of this fix
	   (two stacked shadows, one with a 12px blur) made both hero lines
	   look blurry/shaky instead of crisp, worst on the small-size,
	   thin-stroke tagline (Nunito Sans at text-lg) where a 12px blur is
	   huge relative to the actual glyph strokes — it wasn't adding a
	   shadow behind the text, it was smearing the text itself.

	   It also had no business running in light theme at all: light
	   theme's hero text (`text-paper`) is already DARK ink on a light
	   ground — a dark shadow behind already-dark text adds visible
	   blur without adding any contrast, which is exactly what read as
	   "wrong-prescription-glasses" there. Only dark theme's LIGHT text
	   (`text-paper`, cream) actually benefits from a dark edge — and
	   only dark theme was ever reported as having a legibility problem
	   before this fix existed. Scoped accordingly; single small shadow,
	   no second blurred layer. */
	:global(:root:not([data-theme='light'])) .hero-text {
		text-shadow: 0 1px 2px rgba(0, 0, 0, 0.75);
	}

	.hero-glow {
		animation: drift 24s ease-in-out infinite alternate;
	}
	@keyframes drift {
		from {
			filter: saturate(1);
		}
		to {
			filter: saturate(1.15);
		}
	}
	@media (prefers-reduced-motion: reduce) {
		.hero-glow {
			animation: none;
		}
	}
</style>
