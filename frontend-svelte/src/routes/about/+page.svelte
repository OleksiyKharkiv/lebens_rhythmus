<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
</script>

<svelte:head>
	<title>{m.site_name()} — {m.about_title()}</title>
</svelte:head>

<section class="mx-auto max-w-4xl px-6 py-16 sm:py-24">
	<!-- 2026-08-19 — .page-title, same as every other inner page's h1
	     (was .hero-title, the same big Home-hero size — extra-large was
	     right for Home's actual hero section, wrong here). -->
	<h1 class="page-title font-display font-semibold text-paper">{m.about_title()}</h1>
	<p class="lead-text mt-4 font-display text-teal">{m.about_subtitle()}</p>

	<div class="lead-text mt-10 space-y-6 leading-relaxed text-paper-dim">
		<p>{m.about_body_1()}</p>
		<p>{m.about_body_2()}</p>
	</div>
</section>
