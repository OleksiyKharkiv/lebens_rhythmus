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
		<h1 class="hero-text font-display text-4xl leading-tight font-semibold text-paper sm:text-5xl">
			{m.home_hero_title()}
		</h1>
		<p class="hero-text mt-6 max-w-md text-lg text-paper-dim">
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
					<p class="mt-3 text-sm leading-relaxed text-paper-dim">{d.desc()}</p>
				</div>
			{/each}
		</div>
	</div>
</section>

<style>
	/* 2026-08-17, beta feedback — this is the one spot on the site where
	   text sits directly on the raw page background (no card/section
	   bg behind it), so it's the one place the new background art
	   (layout.css) can land a same-hue patch right behind a word and
	   kill contrast (reported: tagline going near-invisible over a
	   tan/beige patch in dark theme, h1 hard to read over a pink one).
	   A soft dark shadow is the standard fix for text-over-busy-art
	   (hero banners do this constantly) — not layered opacity tricks,
	   which would also dim the art itself everywhere else on the page.
	   Plain black works in both themes: it darkens/defines edges
	   against whatever's behind it rather than trying to match a
	   ground color that flips meaning between themes. */
	.hero-text {
		text-shadow:
			0 1px 3px rgba(0, 0, 0, 0.55),
			0 1px 12px rgba(0, 0, 0, 0.35);
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
