<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import { getPerformances, type PerformanceDTO } from '$lib/api';
	import Card from '$lib/components/Card.svelte';

	let performances = $state<PerformanceDTO[] | null>(null);
	let error = $state(false);

	$effect(() => {
		getPerformances()
			.then((data) => (performances = data))
			.catch(() => (error = true));
	});

	function formatDateTime(d: string) {
		return new Date(d).toLocaleString('de-DE', { dateStyle: 'medium', timeStyle: 'short' });
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.performances_title()}</title>
</svelte:head>

<section class="mx-auto max-w-3xl px-6 py-16 sm:py-24">
	<h1 class="font-display text-3xl font-semibold text-paper sm:text-4xl">{m.performances_title()}</h1>

	{#if error}
		<p class="mt-8 text-error">{m.state_error()}</p>
	{:else if performances === null}
		<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
	{:else if performances.length === 0}
		<p class="mt-8 text-paper-dim">{m.performances_none()}</p>
	{:else}
		<div class="mt-10 space-y-4">
			{#each performances as perf (perf.id)}
				<Card>
					<h2 class="font-display text-xl font-semibold text-paper">{perf.title}</h2>
					<p class="mt-1 text-sm text-paper-dim">{formatDateTime(perf.performanceDate)}</p>
					{#if perf.venue}<p class="text-sm text-paper-dim">{perf.venue}</p>{/if}
					{#if perf.description}<p class="mt-2 text-paper-dim">{perf.description}</p>{/if}
				</Card>
			{/each}
		</div>
	{/if}
</section>
