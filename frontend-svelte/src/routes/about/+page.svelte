<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
</script>

<svelte:head>
	<title>{m.site_name()} — {m.about_title()}</title>
</svelte:head>

<section class="mx-auto max-w-3xl px-6 py-16 sm:py-24">
	<h1 class="font-display text-4xl font-semibold text-paper sm:text-5xl">{m.about_title()}</h1>
	<p class="mt-4 font-display text-lg text-teal">{m.about_subtitle()}</p>

	<div class="mt-10 space-y-6 text-lg leading-relaxed text-paper-dim">
		<p>{m.about_body_1()}</p>
		<p>{m.about_body_2()}</p>
	</div>
</section>
