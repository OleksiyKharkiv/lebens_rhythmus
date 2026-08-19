<script lang="ts">
	import * as m from '$lib/paraglide/messages.js';
	import { getLocale } from '$lib/paraglide/runtime';
	import { getActivities, type ActivityDTO } from '$lib/api';
	import Card from '$lib/components/Card.svelte';

	let activities = $state<ActivityDTO[] | null>(null);
	let error = $state(false);

	$effect(() => {
		getActivities()
			.then((data) => (activities = data))
			.catch(() => (error = true));
	});

	function localizedTitle(a: ActivityDTO) {
		const locale = getLocale();
		if (locale === 'en' && a.titleEn) return a.titleEn;
		if (locale === 'uk' && a.titleUa) return a.titleUa;
		return a.titleDe;
	}

	function localizedDescription(a: ActivityDTO) {
		const locale = getLocale();
		if (locale === 'en' && a.descriptionEn) return a.descriptionEn;
		if (locale === 'uk' && a.descriptionUa) return a.descriptionUa;
		return a.descriptionDe;
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {m.activities_title()}</title>
</svelte:head>

<section class="mx-auto max-w-4xl px-6 py-16 sm:py-24">
	<h1 class="page-title font-display font-semibold text-paper">{m.activities_title()}</h1>

	{#if error}
		<p class="mt-8 text-error">{m.state_error()}</p>
	{:else if activities === null}
		<p class="mt-8 text-paper-dim">{m.state_loading()}</p>
	{:else if activities.length === 0}
		<p class="mt-8 text-paper-dim">{m.state_empty()}</p>
	{:else}
		<div class="mt-10 grid gap-6 sm:grid-cols-2">
			{#each activities.filter((a) => a.active) as activity (activity.id)}
				<Card>
					<h2 class="list-card-title font-display font-semibold text-paper">{localizedTitle(activity)}</h2>
					<p class="lead-text mt-2 text-paper-dim">{localizedDescription(activity)}</p>
					<div class="mt-4 flex items-center justify-between text-sm text-paper-dim">
						<span>{activity.durationMinutes} {m.activities_duration_min()}</span>
						<span class="font-display text-teal">
							{activity.price ? `${activity.price} €` : m.activities_price_on_request()}
						</span>
					</div>
				</Card>
			{/each}
		</div>
	{/if}
</section>
