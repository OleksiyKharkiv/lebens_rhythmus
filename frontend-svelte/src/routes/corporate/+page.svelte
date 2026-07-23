<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import Card from '$lib/components/Card.svelte';
	import Button from '$lib/components/Button.svelte';
</script>

<svelte:head>
	<title>{m.site_name()} — {m.corporate_title()}</title>
</svelte:head>

<section class="mx-auto max-w-3xl px-6 py-16 sm:py-24">
	<h1 class="font-display text-3xl font-semibold text-paper sm:text-4xl">{m.corporate_title()}</h1>
	<p class="mt-4 text-lg text-paper-dim">{m.corporate_intro()}</p>

	<div class="mt-10 grid gap-6 sm:grid-cols-3">
		<Card>
			<h2 class="font-display font-semibold text-paper">{m.corporate_team_title()}</h2>
			<p class="mt-2 text-sm text-paper-dim">{m.corporate_team_desc()}</p>
		</Card>
		<Card>
			<h2 class="font-display font-semibold text-paper">{m.corporate_wellness_title()}</h2>
			<p class="mt-2 text-sm text-paper-dim">{m.corporate_wellness_desc()}</p>
		</Card>
		<Card>
			<h2 class="font-display font-semibold text-paper">{m.corporate_workshops_title()}</h2>
			<p class="mt-2 text-sm text-paper-dim">{m.corporate_workshops_desc()}</p>
		</Card>
	</div>

	<div class="mt-14 text-center">
		<h2 class="font-display text-2xl font-semibold text-paper">{m.corporate_cta_title()}</h2>
		<p class="mt-2 text-paper-dim">{m.corporate_cta_body()}</p>
		<div class="mx-auto mt-6 max-w-xs">
			<Button onclick={() => (window.location.href = '/contact')}>{m.corporate_cta_button()}</Button>
		</div>
	</div>
</section>
