<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import { getWorkshops, type WorkshopListItem } from '$lib/api';
	import Card from '$lib/components/Card.svelte';

	let workshops = $state<WorkshopListItem[] | null>(null);
	let error = $state(false);

	$effect(() => {
		getWorkshops(true)
			.then((data) => (workshops = data))
			.catch(() => (error = true));
	});

	function formatDate(d: string | null) {
		if (!d) return null;
		return new Date(d).toLocaleDateString('de-DE', { day: '2-digit', month: '2-digit', year: 'numeric' });
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.workshops_title()}</title>
</svelte:head>

<section class="mx-auto max-w-4xl px-6 py-16 sm:py-24">
	<h1 class="font-display text-3xl font-semibold text-paper sm:text-4xl">{m.workshops_title()}</h1>

	{#if error}
		<p class="mt-8 text-error">{m.state_error()}</p>
	{:else if workshops === null}
		<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
	{:else if workshops.length === 0}
		<p class="mt-8 text-paper-dim">{m.workshops_no_upcoming()}</p>
	{:else}
		<div class="mt-10 grid gap-6 sm:grid-cols-2">
			{#each workshops as w (w.id)}
				<Card>
					<h2 class="font-display text-xl font-semibold text-paper">{w.title}</h2>
					{#if w.shortDescription}<p class="mt-2 text-sm text-paper-dim">{w.shortDescription}</p>{/if}
					<dl class="mt-4 space-y-1 text-sm text-paper-dim">
						{#if w.startDate}
							<div><dt class="inline font-semibold text-paper">Start:</dt> <dd class="inline">{formatDate(w.startDate)}</dd></div>
						{/if}
					</dl>
					<p class="mt-2 font-display text-teal">
						{w.price ? `${w.price} €` : m.activities_price_on_request()}
					</p>
					<div class="mt-4 flex gap-2">
						<a
							href={`/workshops/${w.id}`}
							class="rounded-full border border-ink-line px-4 py-2 text-sm text-paper transition-colors hover:border-gold"
						>
							{m.workshops_details()}
						</a>
					</div>
				</Card>
			{/each}
		</div>
	{/if}
</section>
