<script lang="ts">
	import { page } from '$app/state';
	import * as m from '$lib/paraglide/messages.js';
	import { isAuthenticated, getWorkshop, enrollInWorkshop, type WorkshopDetail, ApiError } from '$lib/api';
	import Card from '$lib/components/Card.svelte';
	import Button from '$lib/components/Button.svelte';
	import ErrorText from '$lib/components/ErrorText.svelte';

	let workshop = $state<WorkshopDetail | null>(null);
	let error = $state(false);
	let enrollBusy = $state<number | null>(null);
	let enrollError = $state('');
	let enrollSuccess = $state(false);

	// SvelteKit guarantees params.id is set for a matched [id] route — the
	// `string | undefined` in its generated type is generic route typing,
	// not a real runtime possibility here.
	const workshopId = $derived(page.params.id as string);

	function load() {
		getWorkshop(workshopId)
			.then((data) => (workshop = data))
			.catch(() => (error = true));
	}

	$effect(() => {
		load();
	});

	function formatDateTime(d: string) {
		return new Date(d).toLocaleString('de-DE', { dateStyle: 'medium', timeStyle: 'short' });
	}

	async function handleEnroll(groupId: number) {
		if (!isAuthenticated()) {
			window.location.href = '/login';
			return;
		}
		enrollError = '';
		enrollBusy = groupId;
		try {
			await enrollInWorkshop(workshopId, groupId);
			enrollSuccess = true;
			load();
		} catch (err) {
			enrollError = err instanceof ApiError ? err.message : 'Fehler bei der Anmeldung.';
		} finally {
			enrollBusy = null;
		}
	}
</script>

<svelte:head>
	<title>{m.site_name()} — {workshop?.title ?? m.workshops_title()}</title>
</svelte:head>

<section class="mx-auto max-w-3xl px-6 py-16 sm:py-24">
	{#if error}
		<p class="text-error">{m.state_error()}</p>
	{:else if workshop === null}
		<p class="text-paper-dim">{m.state_loading()}</p>
	{:else}
		<h1 class="font-display text-3xl font-semibold text-paper sm:text-4xl">{workshop.title}</h1>
		{#if workshop.description}<p class="mt-4 leading-relaxed text-paper-dim">{workshop.description}</p>{/if}

		<dl class="mt-6 grid grid-cols-2 gap-4 text-sm text-paper-dim">
			{#if workshop.startDate}
				<div><dt class="font-semibold text-paper">Start</dt><dd>{workshop.startDate}</dd></div>
			{/if}
			{#if workshop.teacher}
				<div>
					<dt class="font-semibold text-paper">Kursleitung</dt>
					<dd>{workshop.teacher.firstName} {workshop.teacher.lastName}</dd>
				</div>
			{/if}
			<div>
				<dt class="font-semibold text-paper">Preis</dt>
				<dd>{workshop.price ? `${workshop.price} €` : m.activities_price_on_request()}</dd>
			</div>
		</dl>

		<h2 class="mt-10 font-display text-xl font-semibold text-paper">{m.workshop_detail_groups_title()}</h2>

		{#if enrollSuccess}
			<p class="mt-3 text-success">{m.workshop_detail_enroll_success()}</p>
		{/if}
		<ErrorText message={enrollError} />

		{#if !workshop.groups || workshop.groups.length === 0}
			<p class="mt-3 text-paper-dim">{m.workshop_detail_no_groups()}</p>
		{:else}
			<div class="mt-4 space-y-4">
				{#each workshop.groups.filter((g) => g.active) as group (group.id)}
					{@const full = group.enrolledCount >= group.capacity}
					<Card>
						<div class="flex flex-wrap items-center justify-between gap-4">
							<div>
								<p class="font-display font-semibold text-paper">{group.titleDe || group.titleEn}</p>
								<p class="mt-1 text-sm text-paper-dim">
									{formatDateTime(group.startDateTime)}
									{#if group.endDateTime}— {formatDateTime(group.endDateTime)}{/if}
								</p>
								{#if group.venueName}
									<p class="mt-1 text-sm text-paper-dim">{group.venueName}</p>
								{/if}
								<p class="mt-1 text-sm text-paper-dim">
									{group.enrolledCount}/{group.capacity} {m.workshops_spots_left()}
								</p>
							</div>
							<Button
								variant="teal"
								fullWidth={false}
								disabled={full}
								busy={enrollBusy === group.id}
								onclick={() => handleEnroll(group.id)}
							>
								{full
									? m.workshop_detail_full()
									: isAuthenticated()
										? m.workshops_enroll()
										: m.workshops_enroll_login_first()}
							</Button>
						</div>
					</Card>
				{/each}
			</div>
		{/if}
	{/if}
</section>
